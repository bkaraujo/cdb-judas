package br.cdb.feature.f002;

import br.cdb.core.View;
import br.cdb.feature.f000._0_domain.ClosedPeriod;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f002} */
@NullMarked
public interface F002Api {


    /** Projeção de {@code AccountResponse} — mesmo shape JSON, sem obrigar o consumidor a conhecer o
     *  modelo {@code Account}/{@code CreditCard} de {@code f002}/{@code f003}. */
    @NullMarked
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
        /** Espelha {@code AccountResponse.Card} — projeção somente-leitura de cartão embutida na conta. */
        @NullMarked
        public record CardView(UUID id, String last4, UUID accountId, boolean active) {}
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

    /** Espelha {@code BalanceResponse}. */
    @NullMarked
    record BalanceView(UUID accountId, YearMonth period, BigDecimal balance) implements View {}

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
