package br.cdb.feature.finance.accounts.statement.provider;

import org.jspecify.annotations.NullMarked;

/**
 * Recognizes whether extracted text is any bank's checking-account statement, by composing the
 * per-bank signatures the statement parsers already own. The credit-card parsers gate their
 * {@code parseable} on its negation so a statement is never mistaken for an invoice (e.g. a Santander
 * extrato that names "BTG Pactual" as a boleto counterparty). This is the document-type split that used
 * to live in {@code DocumentTypeDetector}, now expressed without duplicating the markers.
 */
@NullMarked
final class BankStatements {

    private BankStatements() {
    }

    static boolean isAny(DocumentText text) {
        return BTGStatementParser.looksLikeStatement(text)
                || SantanderStatementParser.looksLikeStatement(text);
    }
}
