package br.cdb.feature.f006._1_application;

import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f000._0_domain.event.TransactionImported;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Dono dos vínculos {@code F006_TRANSACTION_CATEGORY} e {@code F006_TRANSACTION_TAG} reagindo a
 * eventos de fatias vizinhas — best-effort, nunca falha o request que originou o evento por si só
 * (mas propaga exceção, ver {@code br.commons.MessageBus#submit}, revertendo a transação do
 * publicador se o próprio {@code save}/{@code reassignCategory}/{@code deleteByTransaction} falhar).
 * Assinado no startup (padrão {@code TransactionEventListener} do contexto monetário). Tags reagem
 * só aqui (delete/import); a reação a exclusão/merge do próprio {@code Tag} (MOVE/DETACH) é
 * separada, em {@code F006Module} (assinada no {@code initialize()}, não numa classe à parte).
 */
@NullMarked
@Singleton
public class TransactionOverlayListener {

    private final WriteUseCases writes = Context.tryGet(WriteUseCases.class);

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    /** Limpa os vínculos das transações apagadas, qualquer que seja o publicador (o próprio
     *  {@code WriteUseCases#deleteTransaction} ou uma exclusão em cascata de outra fatia, ex.: tag/categoria). */
    @MessageListener
    public MessageResult onTransactionsDeleted(TransactionsDeleted event) {
        event.transactionIds().forEach(id -> {
            writes.deleteCategory(id);
            writes.deleteTags(id);
        });
        return MessageResult.CONSUMED;
    }

    /** Grava os vínculos de uma transação importada ({@code InvoiceImportProcessor}/{@code StatementImportProcessor},
     *  aqui mesmo em f006 desde a fase 6 — evento mantido por simplicidade, não por fronteira de fatia)
     *  — mantém o 1:1 com {@code F006_TRANSACTION}. */
    @MessageListener
    public MessageResult onTransactionImported(TransactionImported event) {
        writes.saveCategory(event.transactionId(), event.personId(), event.categoryId());
        writes.saveTags(event.transactionId(), event.personId(), event.tagIds());
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
