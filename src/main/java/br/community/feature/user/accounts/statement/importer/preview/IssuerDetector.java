package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Detects the statement issuer from extracted text. Each bank's checking-account statement carries a
 * bank-specific header that is authoritative on its own (the Santander extrato omits any CNPJ). The
 * CNPJ is the next authority (matched on its digit sequence to be robust to spacing/punctuation):
 * both the credit-card and the checking-account CNPJ of each bank are recognized. The bank-name
 * fallback applies only when nothing above matched, because a bank name can appear merely as a
 * counterparty (e.g. a boleto paid to "Santander" on a BTG extrato, or to "BTG Pactual" on a
 * Santander extrato), which would otherwise make detection ambiguous.
 */
@NullMarked
public class IssuerDetector {

    private static final String BTG_CARD_CNPJ_DIGITS = "30306294000145";
    private static final String BTG_ACCOUNT_CNPJ_DIGITS = "30306294000226";
    private static final String SANTANDER_CNPJ_DIGITS = "90400888000142";
    private static final String BTG_STATEMENT_MARKER = "EXTRATO DA SUA CONTA CORRENTE BTG PACTUAL";
    private static final String SANTANDER_STATEMENT_MARKER = "EXTRATO CONSOLIDADO INTELIGENTE";

    public Issuer detect(String text) {
        final String upper = text.toUpperCase(Locale.ROOT);

        // Each checking-account statement stamps its own header — an authoritative per-bank signal.
        if (upper.contains(BTG_STATEMENT_MARKER)) {
            return Issuer.BTG;
        }
        if (upper.contains(SANTANDER_STATEMENT_MARKER)) {
            return Issuer.SANTANDER;
        }

        final Issuer byCnpj = byCnpj(text.replaceAll("\\D", ""));
        if (byCnpj != null) {
            return byCnpj; // CNPJ match é autoritativo; ambíguo → UNKNOWN, nunca cai para o nome
        }
        return byBankName(upper);
    }

    /** {@code null} se nenhum CNPJ conhecido aparece; senão BTG/SANTANDER (ou UNKNOWN se ambíguo). */
    private static @Nullable Issuer byCnpj(String digits) {
        final boolean btg = digits.contains(BTG_CARD_CNPJ_DIGITS) || digits.contains(BTG_ACCOUNT_CNPJ_DIGITS);
        final boolean santander = digits.contains(SANTANDER_CNPJ_DIGITS);
        if (!btg && !santander) {
            return null;
        }
        if (btg && santander) {
            return Issuer.UNKNOWN;
        }
        return btg ? Issuer.BTG : Issuer.SANTANDER;
    }

    /** Fallback por nome — só quando nada acima casou, pois um nome pode ser mera contraparte. */
    private static Issuer byBankName(String upper) {
        final boolean btg = upper.contains("BTG PACTUAL");
        final boolean santander = upper.contains("SANTANDER");
        if (btg && !santander) {
            return Issuer.BTG;
        }
        if (santander && !btg) {
            return Issuer.SANTANDER;
        }
        return Issuer.UNKNOWN;
    }
}
