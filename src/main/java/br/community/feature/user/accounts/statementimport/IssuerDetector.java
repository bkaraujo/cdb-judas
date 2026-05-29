package br.community.feature.user.accounts.statementimport;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Detects the statement issuer from extracted text by the marker tokens locked in issue-01:
 * the bank CNPJ (primary, matched on its digit sequence to be robust to spacing/punctuation)
 * with a bank-name fallback.
 */
@NullMarked
public class IssuerDetector {

    private static final String BTG_CNPJ_DIGITS = "30306294000145";
    private static final String SANTANDER_CNPJ_DIGITS = "90400888000142";

    public Issuer detect(String text) {
        final String digits = text.replaceAll("\\D", "");
        final String upper = text.toUpperCase(Locale.ROOT);

        final boolean btg = digits.contains(BTG_CNPJ_DIGITS) || upper.contains("BTG PACTUAL");
        final boolean santander = digits.contains(SANTANDER_CNPJ_DIGITS) || upper.contains("SANTANDER");

        if (btg && !santander) return Issuer.BTG;
        if (santander && !btg) return Issuer.SANTANDER;
        return Issuer.UNKNOWN;
    }
}
