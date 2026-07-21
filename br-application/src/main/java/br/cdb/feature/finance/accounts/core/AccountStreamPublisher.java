package br.cdb.feature.finance.accounts.core;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.core.web.Request;
import br.cdb.feature.f000._0_domain.SSE;
import br.commons.Result;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publica atualizações de conta via SSE a partir do fluxo da feature (chamado pelos Resources
 * após a mutação já ter sido persistida — contexto + overlay), nunca em reação a um evento do
 * contexto monetário.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountStreamPublisher {

    private static final String TYPE = "ACCOUNT";

    private final AccountUseCase ucAccount = MonetaryUseCases.ucAccount();
    private final CreditCardUseCase ucCreditCard = MonetaryUseCases.ucCreditCard();
    private final TransactionUseCase ucTransaction = MonetaryUseCases.ucTransaction();

    private final SSE sse;
    private final UserAccountService userAccountService;

    @SuppressWarnings("EmptyCatch")
    public void upsert(UUID accountId) {
        try {
            switch (ucAccount.findAccount(accountId)) {
                case Result.Success(var account) -> dispatchUpsert(account);
                case Result.Failure(var ignored) -> { }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    public void delete(UUID accountId) {
        try {
            sse.dispatch(Request.personId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", accountId.toString()));
        } catch (Exception ignored) {}
    }

    private void dispatchUpsert(Account account) {
        val personId = Request.personId();
        val ua = userAccountService.find(personId, account.id());
        val cards = ucCreditCard.list(account.id()).getOrElse(List.of());
        val dto = AccountResponse.from(account, ua, cards, allTransactions());
        sse.dispatch(personId, SSE.Event.UPSERT, Map.of("type", TYPE, "payload", dto));
    }

    private List<Transaction> allTransactions() {
        return ucTransaction.transactions().getOrElse(List.of());
    }
}
