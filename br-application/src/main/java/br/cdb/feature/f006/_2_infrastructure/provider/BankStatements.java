package br.cdb.feature.f006._2_infrastructure.provider;

import br.cdb.feature.f006._2_infrastructure.provider.btg_pactual.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.santander.SantanderStatementParser;
import org.jspecify.annotations.NullMarked;

/**
 * Recognizes whether extracted text is any bank's checking-account statement, by composing the
 * per-bank signatures the statement parsers already own. The credit-card parsers gate their
 * {@code parseable} on its negation so a statement is never mistaken for an invoice (e.g. a Santander
 * extrato that names "BTG Pactual" as a boleto counterparty). This is the document-type split that used
 * to live in {@code DocumentTypeDetector}, now expressed without duplicating the markers.
 */
@NullMarked
public final class BankStatements {

    private BankStatements() {
    }

    public static boolean isAny(DocumentText text) {
        return BTGStatementParser.looksLikeStatement(text)
                || SantanderStatementParser.looksLikeStatement(text);
    }
}
