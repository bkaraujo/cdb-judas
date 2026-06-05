package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Detects the statement issuer from extracted text. The CNPJ is authoritative (matched on its digit
 * sequence to be robust to spacing/punctuation): both the credit-card and the checking-account CNPJ
 * of each bank are recognized. The bank-name fallback applies only when no CNPJ matched, because a
 * bank name can appear merely as a counterparty (e.g. a boleto paid to "Santander" on a BTG extrato),
 * which would otherwise make detection ambiguous.
 */
@NullMarked
public class IssuerDetector {

    private static final String BTG_CARD_CNPJ_DIGITS = "30306294000145";
    private static final String BTG_ACCOUNT_CNPJ_DIGITS = "30306294000226";
    private static final String SANTANDER_CNPJ_DIGITS = "90400888000142";
    private static final String BTG_STATEMENT_MARKER = "EXTRATO DA SUA CONTA CORRENTE BTG PACTUAL";

    public Issuer detect(String text) {
        final String digits = text.replaceAll("\\D", "");
        final String upper = text.toUpperCase(Locale.ROOT);

        // The BTG checking-account statement stamps its own header — an authoritative BTG signal.
        if (upper.contains(BTG_STATEMENT_MARKER)) {
            return Issuer.BTG;
        }

        final boolean btgCnpj = digits.contains(BTG_CARD_CNPJ_DIGITS) || digits.contains(BTG_ACCOUNT_CNPJ_DIGITS);
        final boolean santanderCnpj = digits.contains(SANTANDER_CNPJ_DIGITS);
        if (btgCnpj || santanderCnpj) {
            if (btgCnpj && !santanderCnpj) return Issuer.BTG;
            if (santanderCnpj && !btgCnpj) return Issuer.SANTANDER;
            return Issuer.UNKNOWN;
        }

        final boolean btgName = upper.contains("BTG PACTUAL");
        final boolean santanderName = upper.contains("SANTANDER");
        if (btgName && !santanderName) return Issuer.BTG;
        if (santanderName && !btgName) return Issuer.SANTANDER;
        return Issuer.UNKNOWN;
    }
}
