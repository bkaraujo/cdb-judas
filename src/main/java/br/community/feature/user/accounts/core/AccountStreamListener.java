package br.community.feature.user.accounts.core;

import br.commons.Result;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.event.TransactionEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.stream.SSE;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converte eventos de domínio de conta em mensagens SSE para o frontend (tipo {@code ACCOUNT};
 * cartões viajam sob o mesmo tipo). Mudanças de transação também disparam um UPSERT da conta
 * afetada, pois o saldo atual derivado muda. Todos os handlers retornam {@code CONSUMED} para não
 * interromper a cadeia de assinantes; falhas no envio são engolidas (entrega best-effort).
 */
@NullMarked
@RequiredArgsConstructor
public class AccountStreamListener {

    private static final String TYPE = "ACCOUNT";

    private final SSE sse;
    private final MonetaryContext monetaryContext;

    @MessageListener
    public MessageResult onAccountCreated(AccountEvents.Created event) {
        upsert(event.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onAccountUpdated(AccountEvents.Updated event) {
        upsert(event.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onAccountDeleted(AccountEvents.Deleted event) {
        delete(event.accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransactionCreated(TransactionEvents.Created event) {
        refreshAccount(event.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransactionUpdated(TransactionEvents.Updated event) {
        refreshAccount(event.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransactionDeleted(TransactionEvents.Deleted event) {
        refreshAccount(event.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    private void refreshAccount(UUID accountId) {
        switch (monetaryContext.findAccount(accountId)) {
            case Result.Success(var account) -> upsert(account);
            case Result.Failure(var ignored) -> { }
        }
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(Account account) {
        try {
            val dto = AccountResponse.from(account, allTransactions());
            sse.dispatch(CurrentUser.getId(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", dto));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID accountId) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", accountId.toString()));
        } catch (Exception ignored) {}
    }

    private List<Transaction> allTransactions() {
        return monetaryContext.listTransactions().getOrElse(List.of());
    }
}
