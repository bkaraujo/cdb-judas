package br.community.feature.user.accounts.statement.importer.preview;

/**
 * The kind of financial PDF the user uploaded on the Lançamentos import box. A credit-card invoice
 * (fatura) feeds the per-card parser + card matching; a bank statement (extrato de conta corrente)
 * feeds the bank-statement parser + account selection. {@link DocumentType#UNKNOWN} when neither
 * marker is recognized.
 */
public enum DocumentType {
    CREDIT_CARD_INVOICE,
    BANK_STATEMENT,
    UNKNOWN
}
