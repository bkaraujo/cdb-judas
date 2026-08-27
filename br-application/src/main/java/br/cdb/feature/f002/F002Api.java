package br.cdb.feature.f002;

import br.cdb.core.View;
import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f006.F006Api;
import br.commons.tools.Strings;
import lombok.val;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f002} */
@NullMarked
public interface F002Api {


    /** Também é o retorno JSON de {@code AccountResource} — mesmo tipo devolvido ao consumidor HTTP
     *  e ao cliente cross-slice, sem obrigar este último a conhecer o modelo {@code Account}/
     *  {@code CreditCard} de {@code f002}/{@code f003}. {@code @Schema(name=...)} fixa o nome do
     *  componente no OpenAPI/frontend, já que o tipo Java migrou pra dentro de {@code F002Api}. */
    @NullMarked
    @Schema(name = "AccountResponse")
    public record AccountView(
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
            List<CardView> cards
    ) implements View {
        /** Projeção somente-leitura de cartão embutida na conta — f002 não pode depender de f003
         *  (fatia irmã) além do modelo de domínio já tolerado, então declara sua própria. */
        @NullMarked
        @Schema(name = "Card")
        public record CardView(UUID id, String last4, UUID accountId, boolean active) {
            public static CardView from(CreditCard creditCard) {
                return new CardView(creditCard.id(), creditCard.last4(), creditCard.accountId(), creditCard.active());
            }
        }

        /** Saldo atual = soma de todas as transações da conta (sem conceito de saldo de abertura). */
        public static AccountView from(Account account, List<CreditCard> creditCards, List<F006Api.TransactionView> transactions) {
            var sum = BigDecimal.ZERO;
            for (val t : transactions) {
                if (account.id().equals(t.accountId())) sum = sum.add(t.amount());
            }
            val type = Strings.upper(account.type().name());
            val cardViews = creditCards.stream().map(CardView::from).toList();
            val color = account.color() != null ? account.color() : "#000000";
            return new AccountView(account.id(), account.name(), type, color, account.active(),
                    account.creditLimit(), account.overdraftLimit(), account.closingDay(), account.dueDay(),
                    sum, cardViews);
        }
    }

    /** Corpo de {@link #createAccount}/{@link #updateAccount} — espelha {@code AccountRequest}. */
    @NullMarked
    public record AccountBody(
            String name,
            String type,
            String color,
            boolean active,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay
    ) {}

    /** Também é o retorno JSON de {@code AccountBalanceResource}. */
    @NullMarked
    @Schema(name = "BalanceResponse")
    record BalanceView(UUID accountId, YearMonth period, BigDecimal balance) implements View {
        public static BalanceView of(Balance balance) {
            return new BalanceView(balance.account().id(), balance.period(), balance.value());
        }
    }

    List<AccountView> accounts();

    AccountView account(UUID id);

    AccountView createAccount(AccountBody body);

    AccountView updateAccount(UUID id, AccountBody body);

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@code MOVE} exige {@code targetId}. */
    void deleteAccount(UUID id, @Nullable String strategy, @Nullable UUID targetId);

    // ── Saldos ──────────────────────────────────────────────────────

    /** Saldo do período para todas as contas da pessoa numa só chamada. */
    List<BalanceView> balances(YearMonth period);

    BalanceView monthlyBalance(UUID accountId, YearMonth period);

    List<BalanceView> yearBalances(UUID accountId, int year);

    // ── Fechamento ─────────────────────────────────────────────────

    /** Período de fechamento contábil vigente ({@code yyyy-MM}) ou {@code null} quando não há. */
    ClosedPeriod closingPeriod();

    ClosedPeriod setClosing(String period);

    void clearClosing();

}
