package br.cdb.feature.f999._1_application;

import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f999._0_domain.DeletionQueueEntry;
import br.cdb.feature.f999._0_domain.DeletionQueueRepository;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Purga a fila {@code F999_DELETION_QUEUE} (rede de segurança da cascata de exclusão best-effort —
 * ver javadoc de {@code Database}): {@link #enqueue} grava uma linha durável logo após a fatia dona
 * publicar seu evento de exclusão; {@link #runOnce} varre as linhas não travadas e tenta
 * republicá-las — sucesso apaga a linha, falha incrementa {@code NUM_ATTEMPTS} e trava
 * ({@code FLG_LOCKED='Y'}) ao atingir {@link #MAX_ATTEMPTS} (sem retry eterno de um evento
 * permanentemente quebrado). No mesmo ciclo, recomputa os snapshots de {@code F002_BALANCE}
 * marcados sujos ({@link BalanceService#recomputeDirty}).
 *
 * <p>{@link #runOnce} é público e chamado tanto pelo {@code @Scheduled} de produção quanto direto
 * pelos testes (sem depender do timer) — ver {@code .claude/plan.md} fase 5.
 *
 * <p>Registrado no {@link Context} por {@code F999Module} (nunca um bean CDI): o construtor recebe a
 * porta, o que deixa os testes injetarem um fake.
 */
@NullMarked
@RequiredArgsConstructor
public class DeletionQueueService {

    public static final String TYPE_ACCOUNT_DELETED = "ACCOUNT_DELETED";
    public static final String TYPE_TRANSACTION_DELETED = "TRANSACTION_DELETED";

    private static final int MAX_ATTEMPTS = 5;

    private final DeletionQueueRepository repo;
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);

    public void enqueue(String type, UUID targetId, UUID personId) {
        val now = Time.now();
        repo.save(new DeletionQueueEntry(UUID.randomUUID(), type, targetId, personId, 0, false, now, now));
    }

    public void runOnce() {
        for (val entry : repo.findAllUnlocked()) {
            try {
                republish(entry);
                repo.deleteById(entry.id());
            } catch (RuntimeException e) {
                retry(entry, e);
            }
        }
        balanceService.recomputeDirty();
    }

    private void republish(DeletionQueueEntry entry) {
        switch (entry.type()) {
            case TYPE_ACCOUNT_DELETED -> MessageBus.submit(new AccountEvents.Deleted(entry.targetId(), entry.personId().toString()));
            case TYPE_TRANSACTION_DELETED -> MessageBus.submit(new TransactionsDeleted(List.of(entry.targetId())));
            default -> throw new IllegalStateException("Tipo desconhecido na fila de exclusão: " + entry.type());
        }
    }

    private void retry(DeletionQueueEntry entry, RuntimeException failure) {
        val attempts = entry.attempts() + 1;
        val locked = attempts >= MAX_ATTEMPTS;
        Logger.warn("Retry da fila de exclusão falhou para %s %s (tentativa %s%s): %s",
                entry.type(), entry.targetId(), attempts, locked ? ", travando" : "", failure.toString());
        repo.save(new DeletionQueueEntry(
                entry.id(), entry.type(), entry.targetId(), entry.personId(),
                attempts, locked, entry.createdAt(), Time.now()));
    }
}
