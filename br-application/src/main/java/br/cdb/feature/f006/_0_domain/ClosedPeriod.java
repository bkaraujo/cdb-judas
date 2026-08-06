package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Fechamento contábil vigente ({@code null} = nenhum): a competência a partir da qual — dela
 * inclusive, para trás — nenhum lançamento pode ser criado, alterado ou importado. O valor vem de
 * {@code GET /accounts/closing} (f002), lido via {@code InternalApi}; aqui ele só ganha a regra de
 * cobertura, usada tanto pela política de escrita manual ({@code WriteUseCases}) quanto pela de
 * importação ({@code ImportUseCase}).
 */
@NullMarked
public record ClosedPeriod(@Nullable YearMonth period) {

    private static final ClosedPeriod OPEN = new ClosedPeriod(null);

    /** {@code period} no formato {@code yyyy-MM}; {@code null} devolve um fechamento que não cobre nada. */
    public static ClosedPeriod of(@Nullable String period) {
        return period == null ? OPEN : new ClosedPeriod(YearMonth.parse(period));
    }

    /** Verdadeiro quando {@code date} cai no período fechado ou antes dele. */
    public boolean covers(LocalDate date) {
        return period != null && !YearMonth.from(date).isAfter(period);
    }

    /** Competência fechada em {@code yyyy-MM}, ou vazio quando não há fechamento. */
    public String label() {
        return period == null ? "" : period.toString();
    }
}
