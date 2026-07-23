package br.cdb.feature.f002._1_application;

import br.cdb.feature.f000._0_domain.event.AccountDeleted;
import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Purga o overlay {@code PERSON_ACCOUNT} da conta apagada, qualquer que seja a estratégia de
 * exclusão — best-effort, nunca falha o request que originou a exclusão. Assinado no startup
 * (padrão {@code TransactionOverlayListener}).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountDeletedListener {

    private final UserAccountService userAccountService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onAccountDeleted(AccountDeleted event) {
        userAccountService.delete(event.personId().toString(), event.accountId());
        return MessageResult.CONSUMED;
    }
}
