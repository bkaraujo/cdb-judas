package br.community.feature.user.accounts.statement.importer.provider;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Text primitives shared by the parsers' {@code parseable} checks: the uppercased text (for
 * case-insensitive marker lookups) and the digits-only projection (for CNPJ matching robust to
 * spacing/punctuation). Holds no issuer knowledge — each parser composes these primitives with its own
 * signatures, so the detection logic stays in the parsers while the boilerplate is factored out here.
 */
@NullMarked
final class DocumentText {

    private final String upper;
    private final String digits;

    DocumentText(String raw) {
        this.upper = raw.toUpperCase(Locale.ROOT);
        this.digits = raw.replaceAll("\\D", "");
    }

    /** {@code needle} must already be uppercase. */
    boolean has(String needle) {
        return upper.contains(needle);
    }

    boolean hasCnpj(String cnpjDigits) {
        return digits.contains(cnpjDigits);
    }

    /** Bank-name fallback: mine present and the other absent (a bank name alone can be a counterparty). */
    boolean nameOnly(String mine, String other) {
        return upper.contains(mine) && !upper.contains(other);
    }
}
