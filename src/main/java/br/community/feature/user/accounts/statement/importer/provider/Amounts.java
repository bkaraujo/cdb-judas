package br.community.feature.user.accounts.statement.importer.provider;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;

/** Conversão de valores BRL ({@code "1.234,56"}) compartilhada pelos parsers de extrato. */
@NullMarked
final class Amounts {

    private Amounts() {
    }

    /** {@code "1.234,56"} (milhar com ponto, decimal com vírgula) → {@link BigDecimal}. */
    static BigDecimal brl(String raw) {
        return new BigDecimal(raw.replace(".", "").replace(",", "."));
    }
}
