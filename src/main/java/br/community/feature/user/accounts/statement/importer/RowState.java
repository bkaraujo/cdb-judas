package br.community.feature.user.accounts.statement.importer;

/**
 * How a bank-statement line relates to what is already stored on the chosen account:
 * {@link #NEW} (will be inserted), {@link #DUPLICATE} (already imported — same date/amount/description,
 * skipped) or {@link #RECONCILE} (matches a pending/scheduled manual transaction, which will be
 * promoted to {@code confirmed} instead of inserting a second row).
 */
public enum RowState {
    NEW,
    DUPLICATE,
    RECONCILE
}
