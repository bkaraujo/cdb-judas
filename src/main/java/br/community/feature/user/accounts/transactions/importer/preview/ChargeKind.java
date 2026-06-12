package br.community.feature.user.accounts.transactions.importer.preview;

/**
 * Classification of a kept credit-card statement charge. Payment-received and summary lines are
 * dropped by each parser, so they are not represented here — only what survives into the preview.
 */
public enum ChargeKind {
    PURCHASE,
    IOF,
    FEE,
    INTEREST
}
