package br.cdb.feature.f003._1_application;

import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Purga os vínculos {@code PERSON_TRANSACTION_TAG} das transações apagadas, qualquer que seja o
 * publicador (f005 ou uma exclusão em cascata de tag/categoria) — best-effort. Assinado no
 * startup (padrão {@code TransactionEventListener} do contexto monetário).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagTransactionListener {

    private final UserTransactionTagService tagLinkService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onTransactionsDeleted(TransactionsDeleted event) {
        event.transactionIds().forEach(tagLinkService::deleteByTransaction);
        return MessageResult.CONSUMED;
    }
}
