package br.cdb.feature.f007._2_infrastructure.provider;

import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

/**
 * Text primitives shared by the parsers' {@code parseable} checks: the uppercased text (for
 * case-insensitive marker lookups) and the digits-only projection (for CNPJ matching robust to
 * spacing/punctuation). Holds no issuer knowledge — each parser composes these primitives with its own
 * signatures, so the detection logic stays in the parsers while the boilerplate is factored out here.
 */
@NullMarked
public final class DocumentText {

    private final String upper;
    private final String digits;

    public DocumentText(String raw) {
        this.upper = Strings.upper(raw);
        this.digits = raw.replaceAll("\\D", "");
    }

    /** {@code needle} must already be uppercase. */
    public boolean has(String needle) {
        return upper.contains(needle);
    }

    public boolean hasCnpj(String cnpjDigits) {
        return digits.contains(cnpjDigits);
    }

    /** Bank-name fallback: mine present and the other absent (a bank name alone can be a counterparty). */
    public boolean nameOnly(String mine, String other) {
        return upper.contains(mine) && !upper.contains(other);
    }
}
