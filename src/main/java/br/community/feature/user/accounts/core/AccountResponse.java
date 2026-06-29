package br.community.feature.user.accounts.core;

import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Transaction;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
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
        @Nullable String last4,
        @Nullable Integer dueDay,
        @Nullable Integer closingDay,
        @Nullable BigDecimal creditLimit,
        @Nullable BigDecimal overdraftLimit,
        BigDecimal currentBalance
) {
    /** Saldo atual = saldo de abertura + todas as transações da conta. */
    public static AccountResponse from(Account monetary, @Nullable UserAccount ua, List<Transaction> transactions) {
        var sum = BigDecimal.ZERO;
        for (val t : transactions) {
            if (monetary.id().equals(t.accountId())) sum = sum.add(BigDecimal.valueOf(t.signal()).multiply(t.amount()));
        }
        val type = Strings.upper(monetary.type().name());
        if (ua == null) {
            return new AccountResponse(monetary.id(), monetary.name(), BigDecimal.ZERO, type,
                    "#000000", monetary.active(), null, null, null, null, null, null, sum);
        }
        val opening = ua.openingBalance();
        return new AccountResponse(monetary.id(), monetary.name(), opening, type,
                ua.color(), ua.active(), ua.linkedAccountId(), ua.last4(), ua.dueDay(),
                ua.closingDay(), ua.creditLimit(), ua.overdraftLimit(), opening.add(sum));
    }
}
