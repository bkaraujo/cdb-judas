package br.cdb.feature.f007;

import br.cdb.feature.f007._1_application.service.StatementImportService;
import br.cdb.feature.f007._2_infrastructure.MonetaryCardProvider;
import br.cdb.feature.f007._2_infrastructure.provider.btg_pactual.BTGInvoiceParser;
import br.cdb.feature.f007._2_infrastructure.provider.btg_pactual.BTGStatementParser;
import br.cdb.feature.f007._2_infrastructure.provider.sams_club.SamsClubInvoiceParser;
import br.cdb.feature.f007._2_infrastructure.provider.santander.SantanderInvoiceParser;
import br.cdb.feature.f007._2_infrastructure.provider.santander.SantanderStatementParser;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f007} (importação de extrato/fatura — extraída de {@code f006} por
 * {@code .claude/plan.md}). Sem tabela própria ({@code Database.initialize} não entra aqui): a
 * importação escreve em {@code F006_TRANSACTION} via a porta {@code TransactionWriter}, servida por
 * {@code f999.TransactionWriterAdapter}. Amarra o extrator de PDF (utilitário técnico) e os parsers
 * em {@link StatementImportService}, movidos de {@code F006Module} nesta extração.
 */
@NullMarked
public class F007Module implements Lifecycle {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(PdfTextExtractor.class, () -> new PdfBoxTextExtractor(MAX_STATEMENT_PAGES));
        Context.set(StatementImportService.class, () -> new StatementImportService(
                new MonetaryCardProvider(),
                Context.get(PdfTextExtractor.class),
                List.of(
                        new BTGStatementParser(),
                        new BTGInvoiceParser(),
                        new SantanderStatementParser(),
                        new SantanderInvoiceParser(),
                        new SamsClubInvoiceParser()
                ),
                MAX_STATEMENT_FILE_BYTES)
        );

        return Result.success();
    }
}
