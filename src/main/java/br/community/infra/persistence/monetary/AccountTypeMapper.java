package br.community.infra.persistence.monetary;

import br.community.context.monetary._0_domain.model.Account;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/** Mapa bidirecional fixo entre {@link Account.Type} e o UUID semeado em {@code MON_ACCOUNT_TYPE}. */
@NullMarked
public abstract class AccountTypeMapper {

    private AccountTypeMapper() {}

    public static final String CHECKING_ID    = "a1000000-0000-0000-0000-000000000001";
    public static final String INVESTMENT_ID  = "a1000000-0000-0000-0000-000000000002";
    public static final String CREDIT_CARD_ID = "a1000000-0000-0000-0000-000000000003";

    private static final Map<Account.Type, String> TO_ID = Map.of(
            Account.Type.CHECKING,    CHECKING_ID,
            Account.Type.INVESTMENT,  INVESTMENT_ID,
            Account.Type.CREDIT_CARD, CREDIT_CARD_ID
    );

    private static final Map<String, Account.Type> FROM_ID = Map.of(
            CHECKING_ID,    Account.Type.CHECKING,
            INVESTMENT_ID,  Account.Type.INVESTMENT,
            CREDIT_CARD_ID, Account.Type.CREDIT_CARD
    );

    public static String toId(Account.Type type) {
        val id = TO_ID.get(type);
        if (id == null) throw new IllegalArgumentException("Unknown Account.Type: " + type);
        return id;
    }

    public static Account.Type fromId(String id) {
        val type = FROM_ID.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown Account.Type ID: " + id);
        return type;
    }
}
