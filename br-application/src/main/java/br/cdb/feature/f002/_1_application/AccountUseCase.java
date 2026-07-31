package br.cdb.feature.f002._1_application;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.AccountDeleted;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f002._1_application.command.AccountCommand;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutação de conta na fronteira da fatia {@code f002} (accounts, com balance — fatia fina demais para
 * hexágono próprio, fundida aqui per .claude/refactor.md). Toda a <b>leitura</b> da fatia vive em
 * {@link ReadUseCase}, de onde os {@code *Resource} leem direto; o que resta aqui é escrita, e o que
 * ela lê (conta de destino do MOVE, cartões da conta recém-salva, contagem de transações ligadas)
 * também vem de {@link ReadUseCase}. Nome coincide com o use case ex-contexto monetário;
 * referenciado por FQN completo (campo {@code ucAccount}) para evitar colisão de import.
 *
 * <p>{@code deleteAccount} não reatribui nada imperativamente no MOVE: reassign de conta em
 * transação é feito pela própria engine ({@code AccountUseCase.deleteMove}, contexto monetário) ao
 * mover as transações. A conta é uma linha única (dono+cor inclusos) apagada pela própria engine
 * antes do evento pós-delete ({@link AccountDeleted}/{@link TransactionsDeleted}) ser publicado —
 * sem FK entre tabelas de dados, o contexto pode apagar a raiz primeiro; o evento só resta pros
 * listeners de outras fatias (overlay de tags/categoria das transações apagadas). Cada evento
 * também vira uma linha em {@code F999_DELETION_QUEUE} (via {@link DeletionQueue}) — rede de
 * segurança pro job de f999 reprocessar se o listener best-effort falhar ou o processo cair no meio
 * da cascata. No MOVE, a conta de destino é marcada suja ({@link BalanceService#markDirty}) — o
 * próprio job recomputa.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountUseCase {

    private final br.cdb.feature.f002._1_application.usecase.AccountUseCase ucAccount =
            Context.tryGet(br.cdb.feature.f002._1_application.usecase.AccountUseCase.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    private final UserGuards guards;
    private final DeletionQueue deletionQueue;
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);

    public Result<ReadUseCase.AccountView, BusinessError> createAccount(AccountCommand.Create cmd, String color) {
        val personId = HTTPRequest.personId();
        return ucAccount.upsert(cmd, personId, color).map(account -> {
            MessageBus.submit(new AccountStreamEvents.Created(account.id(), personId));
            return new ReadUseCase.AccountView(account, List.of(), List.of());
        });
    }

    public Result<ReadUseCase.AccountView, BusinessError> updateAccount(AccountCommand.Update cmd, String color) {
        return guards.ownsAccount(cmd.id()).flatMap(ignored -> {
            val personId = HTTPRequest.personId();
            return ucAccount.upsert(cmd, personId, color).map(account -> {
                MessageBus.submit(new AccountStreamEvents.Updated(account.id(), personId));
                return new ReadUseCase.AccountView(account, reads.cards(account.id(), personId), List.of());
            });
        });
    }

    /**
     * MOVE tem o alvo validado aqui primeiro (fronteira da feature) para nunca reatribuir transações
     * para uma conta inválida — a engine (contexto monetário) faz o re-key de fato ao apagar.
     * Sem FK forçando ordem: o contexto apaga a raiz (linha única, dono+cor inclusos) e só então
     * publica-se {@link AccountDeleted} (todas as estratégias) e, fora do MOVE,
     * {@link TransactionsDeleted} (overlay/tag das transações apagadas).
     */
    public Result<DeletionOutcome, BusinessError> deleteAccount(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        val guard = strategy == DeletionStrategy.MOVE
                ? guards.ownsAccounts(id, Objects.requireNonNull(targetId))
                : guards.ownsAccount(id);
        if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);

        if (strategy == null) {
            val count = reads.transactionCount(personId, id);
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
        }

        if (strategy == DeletionStrategy.MOVE) {
            val target = Objects.requireNonNull(targetId);
            val targetCheck = validateAccountMoveTarget(id, target, personId);
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
        }

        return ucAccount.delete(new AccountCommand.Delete(id, Deletions.toPolicy(strategy, targetId))).map(ids -> {
            MessageBus.submit(new AccountDeleted(id, personId));
            deletionQueue.enqueueAccountDeleted(id, personId);

            if (strategy != DeletionStrategy.MOVE) {
                MessageBus.submit(new TransactionsDeleted(ids));
                ids.forEach(txId -> deletionQueue.enqueueTransactionDeleted(txId, personId));
            }

            MessageBus.submit(new AccountStreamEvents.Deleted(id, personId.toString()));
            if (strategy == DeletionStrategy.MOVE) {
                val target = Objects.requireNonNull(targetId);
                balanceService.markDirty(target);
                MessageBus.submit(new AccountStreamEvents.Refresh(target, personId.toString()));
            }
            return new DeletionOutcome.Completed();
        });
    }

    private Result<Void, BusinessError> validateAccountMoveTarget(UUID sourceId, UUID targetId, UUID personId) {
        return reads.findAccount(targetId, personId.toString()).flatMap(target -> {
            if (target.id().equals(sourceId)) {
                return Result.failure(new BusinessError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.failure(new BusinessError.BusinessRule("Target account is inactive: " + targetId));
            }
            return Result.success();
        });
    }
}
