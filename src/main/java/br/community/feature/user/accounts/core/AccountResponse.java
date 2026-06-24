package br.community.feature.user.accounts.core;

import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Transaction;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NullMarked
public record AccountResponse(
        UUID id,
        String name,
        BigDecimal balance,
        String type,
        String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        Map<String, Object> additionalInfo,
        BigDecimal currentBalance
) {
    /** Saldo atual = saldo de abertura + todas as transações da conta. */
    public static AccountResponse from(Account monetary, @Nullable UserAccount ua, List<Transaction> transactions) {
        val opening = ua != null ? ua.openingBalance() : BigDecimal.ZERO;
        val color = ua != null ? ua.color() : "#000000";
        val active = ua != null ? ua.active() : monetary.active();
        var sum = BigDecimal.ZERO;
        for (val t : transactions) {
            if (monetary.id().equals(t.accountId())) sum = sum.add(BigDecimal.valueOf(t.signal()).multiply(t.amount()));
        }
        return new AccountResponse(
                monetary.id(),
                monetary.name(),
                opening,
                Strings.upper(monetary.type().name()),
                color,
                active,
                monetary.linkedAccountId(),
                monetary.additionalInfo(),
                opening.add(sum)
        );
    }
}
