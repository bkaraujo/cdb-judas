package br.cdb.feature.f007._0_domain;

import br.commons.tools.Parser;
import org.jspecify.annotations.NullMarked;

/**
 * A bank-specific statement parser and the central piece of the import architecture: the use case
 * iterates a collection of these and asks each {@link #parseable(String)} whether it recognizes the
 * extracted text as its own document (issuer + document type), then lets the single capable one
 * {@link #parse(String)} it into the common {@link MonetaryDocument}. Replaces the former
 * {@code CreditCardStatementParser} / {@code BankStatementParser} split and the external issuer
 * detection plus registry routing.
 */
@NullMarked
public interface StatementParser extends Parser<MonetaryDocument> {

}
