package br.community.feature.user.accounts.transactions.importer;

import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import br.community.context.monetary.MonetaryContext;
import br.community.feature.user.accounts.statement.provider.BTGInvoiceParser;
import br.community.feature.user.accounts.statement.provider.BTGStatementParser;
import br.community.feature.user.accounts.statement.provider.SantanderInvoiceParser;
import br.community.feature.user.accounts.statement.provider.SantanderStatementParser;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Wires the credit-card statement-import feature: the PDF extractor (technical util), the parsing
 * services and the orchestrating {@link StatementImportUseCase}, which reaches the monetary context
 * only through its {@link MonetaryContext} facade.
 */
@NullMarked
@Singleton
public class StatementImportModule {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    @Produces
    @Singleton
    PdfTextExtractor pdfTextExtractor() {
        return new PdfBoxTextExtractor(MAX_STATEMENT_PAGES);
    }

    @Produces
    @Singleton
    StatementImportUseCase statementImportUseCase(
            MonetaryContext monetaryContext,
            CreditCardProvider creditCardProvider,
            PdfTextExtractor extractor
    ) {
        return new StatementImportUseCase(
                monetaryContext,
                creditCardProvider,
                extractor,
                List.of(
                        new BTGStatementParser(),
                        new BTGInvoiceParser(),
                        new SantanderStatementParser(),
                        new SantanderInvoiceParser()
                ),
                MAX_STATEMENT_FILE_BYTES);
    }
}
