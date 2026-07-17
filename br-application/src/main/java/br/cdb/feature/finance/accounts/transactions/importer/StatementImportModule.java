package br.cdb.feature.finance.accounts.transactions.importer;

import br.cdb.feature.finance.accounts.statement.provider.BTGInvoiceParser;
import br.cdb.feature.finance.accounts.statement.provider.BTGStatementParser;
import br.cdb.feature.finance.accounts.statement.provider.SantanderInvoiceParser;
import br.cdb.feature.finance.accounts.statement.provider.SantanderStatementParser;
import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Wires the credit-card statement-import feature: the PDF extractor (technical util), the parsing
 * services and the orchestrating {@link StatementImportService}, which reaches the monetary context
 * only through the use cases exposed by {@code MonetaryUseCases}.
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
    StatementImportService statementImportService(
            CreditCardProvider creditCardProvider,
            PdfTextExtractor extractor
    ) {
        return new StatementImportService(
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
