package br.cdb.feature.f006._1_application;

import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f000._0_domain.event.TransactionImported;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Dono do vínculo {@code F005_TRANSACTION_CATEGORY} reagindo a eventos de fatias vizinhas —
 * best-effort, nunca falha o request que originou o evento por si só (mas propaga exceção, ver
 * {@code br.commons.MessageBus#submit}, revertendo a transação do publicador se o próprio
 * {@code save}/{@code reassignCategory}/{@code deleteByTransaction} falhar). Assinado no startup
 * (padrão {@code TransactionEventListener} do contexto monetário).
 */
@NullMarked
@Singleton
public class TransactionOverlayListener {

    private final WriteUseCases writes = Registry.tryGet(WriteUseCases.class);

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    /** Limpa o vínculo das transações apagadas, qualquer que seja o publicador (o próprio
     *  {@code WriteUseCases#deleteTransaction} ou uma exclusão em cascata de outra fatia, ex.: tag/categoria). */
    @MessageListener
    public MessageResult onTransactionsDeleted(TransactionsDeleted event) {
        event.transactionIds().forEach(writes::deleteCategory);
        return MessageResult.CONSUMED;
    }

    /** Grava o vínculo de uma transação importada ({@code InvoiceImportProcessor}/{@code StatementImportProcessor},
     *  aqui mesmo em f006 desde a fase 6 — evento mantido por simplicidade, não por fronteira de fatia)
     *  — mantém o 1:1 com {@code F006_TRANSACTION}. */
    @MessageListener
    public MessageResult onTransactionImported(TransactionImported event) {
        writes.saveCategory(event.transactionId(), event.personId(), event.categoryId());
        return MessageResult.CONSUMED;
    }

    /** Re-keya o vínculo da subárvore de categoria apagada (estratégia MOVE, f005) antes da subárvore sumir. */
    @MessageListener
    public MessageResult onCategoryReassigned(CategoryReassigned event) {
        event.oldCategoryIds().forEach(oldId ->
                writes.reassignCategory(oldId, event.newCategoryId(), event.personId()));
        return MessageResult.CONSUMED;
    }
}
