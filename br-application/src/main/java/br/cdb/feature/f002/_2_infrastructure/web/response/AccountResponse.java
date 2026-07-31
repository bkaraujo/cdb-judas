package br.cdb.feature.f002._2_infrastructure.web.response;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f006._0_domain.model.Transaction;
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
        List<Card> cards
) {
    /** Projeção somente-leitura de {@code CreditCard} (contexto monetário), com o mesmo shape
     *  JSON de {@code f003.CardResponse} — f002 não pode depender de f003 (fatia irmã), então
     *  declara sua própria; f003 é dono das mutações de cartão. */
    @NullMarked
    public record Card(UUID id, String last4, UUID accountId, boolean active) {
        public static Card from(CreditCard creditCard) {
            return new Card(creditCard.id(), creditCard.last4(), creditCard.accountId(), creditCard.active());
        }
    }

    /** Saldo atual = soma de todas as transações da conta (sem conceito de saldo de abertura). */
    public static AccountResponse from(Account account, List<CreditCard> creditCards, List<Transaction> transactions) {
        var sum = BigDecimal.ZERO;
        for (val t : transactions) {
            if (account.id().equals(t.accountId())) sum = sum.add(BigDecimal.valueOf(t.signal()).multiply(t.amount()));
        }
        val type = Strings.upper(account.type().name());
        val cardDtos = creditCards.stream().map(Card::from).toList();
        val color = account.color() != null ? account.color() : "#000000";
        return new AccountResponse(account.id(), account.name(), type, color, account.active(),
                account.creditLimit(), account.overdraftLimit(), account.closingDay(), account.dueDay(),
                sum, cardDtos);
    }
}
