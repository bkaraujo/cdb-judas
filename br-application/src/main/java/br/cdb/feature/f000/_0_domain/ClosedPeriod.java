package br.cdb.feature.f000._0_domain;

import br.cdb.core.View;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Fechamento contábil vigente ({@code null} = nenhum): a competência a partir da qual — dela
 * inclusive, para trás — nenhum lançamento pode ser criado, alterado ou importado. O valor vem de
 * {@code GET /accounts/closing}, devolvido já embrulhado por {@code f002.F002Api}; aqui ele só ganha
 * a regra de cobertura, usada tanto pela política de escrita manual ({@code f006.WriteUseCases})
 * quanto pela de importação ({@code f006.ImportUseCase}).
 *
 * <p>Mora em {@code f000} porque é vocabulário de duas fatias: quem <b>produz</b> o fechamento é
 * {@code f002} (dono da rota) e quem o <b>aplica</b> é {@code f006}. Em qualquer das duas seria
 * dependência de fatia irmã.
 */
@NullMarked
public record ClosedPeriod(@Nullable YearMonth period) implements View {

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
