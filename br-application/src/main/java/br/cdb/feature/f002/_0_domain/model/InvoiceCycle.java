package br.cdb.feature.f002._0_domain.model;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Ciclo de fatura de cartão. O ciclo é da <b>conta</b>, não do cartão: {@code NUM_CLOSING_DAY} e
 * {@code NUM_DUE_DAY} são colunas de {@code F002_ACCOUNT}, compartilhadas por todos os cartões dela.
 *
 * <p>Regra (idêntica à do frontend em {@code web/js/_1_domain/invoice.js} — contrato descrito em
 * {@code docs/backend/invoice-cycle.md}):
 * <ul>
 *   <li>{@code close(m)} = dia {@code closingDay} do mês {@code m}, clampado ao tamanho do mês;</li>
 *   <li>uma compra {@code d} pertence ao ciclo {@code m} tal que {@code close(m-1) < d <= close(m)}
 *       (intervalo meio-aberto: comprar no dia do fechamento ainda entra na fatura que fecha);</li>
 *   <li>o vencimento é a primeira data com dia {@code dueDay} <b>estritamente depois</b> de
 *       {@code close(m)} — o que resolve sozinho o caso {@code dueDay <= closingDay}, em que a
 *       fatura vence no mês seguinte ao do fechamento.</li>
 * </ul>
 *
 * <p>É o que faz a compra de cartão sair da conta só quando a fatura vence:
 * {@code BalanceService.recalculate} re-data toda transação com {@code cardId} para
 * {@link #dueDate(Account, LocalDate)} antes de somar os snapshots mensais de {@code F002_BALANCE}.
 *
 * <p>Puro: recebe {@link Account} + {@link LocalDate}, nunca {@code Transaction} — assim não
 * acopla f002 a f006.
 */
@NullMarked
public final class InvoiceCycle {

    public static final int DEFAULT_CLOSING_DAY = 1;
    public static final int DEFAULT_DUE_DAY = 10;

    private InvoiceCycle() {}

    /** Vencimento da fatura em que a compra {@code purchase} cai. */
    public static LocalDate dueDate(Account account, LocalDate purchase) {
        val month = YearMonth.from(purchase);
        val closingMonth = purchase.isAfter(closingOn(account, month)) ? month.plusMonths(1) : month;

        val closing = closingOn(account, closingMonth);
        val candidate = dueOn(account, closingMonth);
        return candidate.isAfter(closing) ? candidate : dueOn(account, closingMonth.plusMonths(1));
    }

    private static LocalDate closingOn(Account account, YearMonth month) {
        return atDay(month, day(account.closingDay(), DEFAULT_CLOSING_DAY));
    }

    private static LocalDate dueOn(Account account, YearMonth month) {
        return atDay(month, day(account.dueDay(), DEFAULT_DUE_DAY));
    }

    private static LocalDate atDay(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private static int day(@Nullable Integer configured, int fallback) {
        return configured != null && configured > 0 ? configured : fallback;
    }
}
