package br.cdb.feature.user.accounts.core;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.core.web.security.CurrentUser;
import br.cdb.feature.user.stream.SSE;
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

    private final SSE sse;
    private final MonetaryContext monetaryContext;
    private final UserAccountService userAccountService;

    @SuppressWarnings("EmptyCatch")
    public void upsert(UUID accountId) {
        try {
            switch (monetaryContext.findAccount(accountId)) {
                case Result.Success(var account) -> dispatchUpsert(account);
                case Result.Failure(var ignored) -> { }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    public void delete(UUID accountId) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", accountId.toString()));
        } catch (Exception ignored) {}
    }

    private void dispatchUpsert(Account account) {
        val userId = CurrentUser.getId();
        val ua = userAccountService.find(userId, account.id());
        val cards = monetaryContext.listCardsByAccount(account.id()).getOrElse(List.of());
        val dto = AccountResponse.from(account, ua, cards, allTransactions());
        sse.dispatch(userId, SSE.Event.UPSERT, Map.of("type", TYPE, "payload", dto));
    }

    private List<Transaction> allTransactions() {
        return monetaryContext.listTransactions().getOrElse(List.of());
    }
}
