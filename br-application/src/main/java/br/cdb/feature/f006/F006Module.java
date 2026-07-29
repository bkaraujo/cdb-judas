package br.cdb.feature.f006;

import br.cdb.feature.f006._0_domain.CreditCardProvider;
import br.cdb.feature.f006._0_domain.UserTransactionRepository;
import br.cdb.feature.f006._1_application.StatementImportService;
import br.cdb.feature.f006._2_infrastructure.persistence.UserTransactionJDBCRepository;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderStatementParser;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
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
 * Módulo CDI da fatia {@code f006} (transactions + transfer + importação de extrato/fatura — f007
 * fundida aqui na fase 6 de {@code .claude/plan.md}). {@link #userTransactionRepository} recebe
 * {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do adaptador
 * JDBC — a dependência real fica escondida dentro do {@link Registry}. {@link #statementImportService}
 * amarra o extrator de PDF (utilitário técnico) e os parsers; {@link StatementImportService} alcança
 * as engines Registry-wired (ex-contexto monetário) de f002/f003/f006 via {@code Registry.tryGet(...)}.
 */
@NullMarked
@ApplicationScoped
public class F006Module {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    @Produces
    @Singleton
    public UserTransactionRepository userTransactionRepository() {
        return Registry.tryGet(UserTransactionRepository.class, UserTransactionJDBCRepository::new);
    }

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

    void onStart(@Observes @Priority(6) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
