package br.cdb.feature.f006._1_application;

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
 * Limpa o overlay {@code PERSON_TRANSACTION} das transações apagadas, qualquer que seja o
 * publicador do evento (o próprio {@code TransactionUseCase} ou uma exclusão em cascata de
 * outra fatia, ex.: tag/categoria) — best-effort, nunca falha o request que originou a exclusão.
 * Assinado no startup (padrão {@code TransactionEventListener} do contexto monetário).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionOverlayListener {

    private final UserTransactionService userTransactionService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onTransactionsDeleted(TransactionsDeleted event) {
        event.transactionIds().forEach(userTransactionService::deleteByTransaction);
        return MessageResult.CONSUMED;
    }
}
