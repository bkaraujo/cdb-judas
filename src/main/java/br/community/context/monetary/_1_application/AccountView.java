package br.community.context.monetary._1_application;

import br.community.context.monetary._0_domain.model.Account;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Read projection of an account carrying both the persisted opening balance
 * ({@code balance}) and the derived {@code currentBalance} (opening + every
 * transaction). Used as the domain event payload so read screens (cadastro, visão geral)
 * stay in sync with the statement, which derives the same figure.
 */
@NullMarked
public record AccountView(
        UUID id,
        String name,
        BigDecimal balance,
        Account.Type type,
        String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        Map<String, Object> additionalInfo,
        BigDecimal currentBalance
) {
    public static AccountView of(Account account, BigDecimal currentBalance) {
        return new AccountView(
                account.id(),
                account.name(),
                account.balance(),
                account.type(),
                account.color(),
                account.active(),
                account.linkedAccountId(),
                account.additionalInfo(),
                currentBalance
        );
    }
}
