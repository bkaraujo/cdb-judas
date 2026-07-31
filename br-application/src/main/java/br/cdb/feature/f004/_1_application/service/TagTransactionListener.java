package br.cdb.feature.f004._1_application.service;

import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Purga os vínculos {@code PERSON_TRANSACTION_TAG} das transações apagadas, qualquer que seja o
 * publicador (f006 ou uma exclusão em cascata de tag/categoria) — best-effort. Assinado no
 * startup (padrão {@code TransactionEventListener} do contexto monetário).
 */
@NullMarked
@Singleton
public class TagTransactionListener {

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    /** Context-wired (o serviço deixou de ser bean CDI): resolvido por chamada, já com as portas de
     *  {@code F004Module} publicadas. */
    @MessageListener
    public MessageResult onTransactionsDeleted(TransactionsDeleted event) {
        val tagLinkService = Context.tryGet(TransactionTagService.class);
        event.transactionIds().forEach(tagLinkService::deleteByTransaction);
        return MessageResult.CONSUMED;
    }
}
