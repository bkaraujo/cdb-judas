package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Routes an uploaded PDF to the invoice path or the bank-statement path, before any issuer-specific
 * parsing. The bank statement (extrato de conta corrente) is recognized by a header marker — BTG's
 * {@code "Extrato (da sua) conta corrente"} with its {@code "Data e hora ... Saldo Diário"} columns,
 * or Santander's {@code "Extrato Consolidado Inteligente"} (whose own "Conta Corrente" wording is too
 * weak, since a Santander invoice also mentions a "Conta Corrente" debit). BTG also stamps the
 * checking-account CNPJ {@code 30.306.294/0002-26} (digits {@code ...000226}), which differs from the
 * credit-card CNPJ {@code ...000145}. Anything not recognized as a statement is treated as a
 * credit-card invoice so the existing flow is unchanged.
 */
@NullMarked
public class DocumentTypeDetector {

    private static final String BTG_ACCOUNT_CNPJ_DIGITS = "30306294000226";

    public DocumentType detect(String text) {
        final String upper = text.toUpperCase(Locale.ROOT);
        final String digits = text.replaceAll("\\D", "");

        final boolean statement =
                upper.contains("EXTRATO DE CONTA CORRENTE")
                        || upper.contains("EXTRATO DA SUA CONTA CORRENTE")
                        || upper.contains("EXTRATO CONSOLIDADO INTELIGENTE")
                        || digits.contains(BTG_ACCOUNT_CNPJ_DIGITS)
                        || (upper.contains("SALDO DIÁRIO") && upper.contains("DATA E HORA"));

        return statement ? DocumentType.BANK_STATEMENT : DocumentType.CREDIT_CARD_INVOICE;
    }
}
