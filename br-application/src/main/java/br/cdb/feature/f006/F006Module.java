package br.cdb.feature.f006;

import br.cdb.feature.f005._1_application.UserTransactionService;
import br.cdb.feature.f006._0_domain.CreditCardProvider;
import br.cdb.feature.f006._1_application.StatementImportService;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderStatementParser;
import br.commons.Logger;
import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Wires a fatia {@code f006} (importação de extrato): o extrator de PDF (utilitário técnico), os
 * parsers e o {@link StatementImportService}, que alcança o contexto monetário só pelos use cases
 * expostos por {@code MonetaryUseCases}.
 */
@NullMarked
@ApplicationScoped
public class F006Module {

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
            PdfTextExtractor extractor,
            UserTransactionService userTransactionService
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
                MAX_STATEMENT_FILE_BYTES,
                userTransactionService);
    }

    void onStart(@Observes @Priority(6) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
