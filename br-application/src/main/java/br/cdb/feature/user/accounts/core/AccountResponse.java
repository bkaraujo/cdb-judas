package br.cdb.feature.user.accounts.core;

import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.user.accounts.cards.CardResponse;
import br.commons.tools.Strings;
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
        String type,
        String color,
        boolean active,
        @Nullable BigDecimal creditLimit,
        @Nullable BigDecimal overdraftLimit,
        @Nullable Integer closingDay,
        @Nullable Integer dueDay,
        BigDecimal currentBalance,
        List<CardResponse> cards
) {
    /** Saldo atual = soma de todas as transações da conta (sem conceito de saldo de abertura). */
    public static AccountResponse from(Account monetary, @Nullable UserAccount ua,
                                       List<CreditCard> creditCards, List<Transaction> transactions) {
        var sum = BigDecimal.ZERO;
        for (val t : transactions) {
            if (monetary.id().equals(t.accountId())) sum = sum.add(BigDecimal.valueOf(t.signal()).multiply(t.amount()));
        }
        val type = Strings.upper(monetary.type().name());
        val cardDtos = creditCards.stream().map(CardResponse::from).toList();
        val color = ua != null ? ua.color() : "#000000";
        return new AccountResponse(monetary.id(), monetary.name(), type, color, monetary.active(),
                monetary.creditLimit(), monetary.overdraftLimit(), monetary.closingDay(), monetary.dueDay(),
                sum, cardDtos);
    }
}
