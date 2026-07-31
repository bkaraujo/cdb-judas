package br.cdb.feature.f006;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f006._0_domain.CreditCardProvider;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._1_application.service.StatementImportService;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f006._2_infrastructure.MonetaryCardProvider;
import br.cdb.feature.f006._2_infrastructure.persistence.TransactionJDBCRepository;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderStatementParser;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f006} (transactions + transfer + importação de extrato/fatura — f007
 * fundida aqui na fase 6 de {@code .claude/plan.md}). Amarra o extrator de PDF (utilitário técnico)
 * e os parsers em {@link StatementImportService}; o par CQRS {@link ReadUseCases}/
 * {@link WriteUseCases} monta-se sozinho no {@link Context} ({@code Context.tryGet(...)} no primeiro
 * consumidor), como as demais classes ex-contexto.
 */
@NullMarked
public class F006Module implements Lifecycle {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F006_TRANSACTION (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    NUM_SIGNAL INT NOT NULL,
                    DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                    TMS_PURCHASE TIMESTAMP NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    COD_CARD CHAR(36),
                    COD_STATUS VARCHAR(20) NOT NULL REFERENCES SYS_STATUS(ID),
                    COD_COST_CENTER CHAR(36) NOT NULL REFERENCES F000_COST_CENTER(ID),
                    DAT_PAYMENT DATE,
                    GROUP_ID CHAR(36),
                    NUM_INSTALLMENT INT NOT NULL,
                    NUM_INSTALLMENT_TOTAL INT NOT NULL,
                    TXT_NOTES VARCHAR(1000),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(TransactionRepository.class, TransactionJDBCRepository::new);
        Context.set(CreditCardProvider.class, MonetaryCardProvider::new);
        Context.set(PdfTextExtractor.class, () -> new PdfBoxTextExtractor(MAX_STATEMENT_PAGES));
        Context.set(StatementImportService.class, () -> new StatementImportService(
                Context.get(CreditCardProvider.class),
                Context.get(PdfTextExtractor.class),
                List.of(
                        new BTGStatementParser(),
                        new BTGInvoiceParser(),
                        new SantanderStatementParser(),
                        new SantanderInvoiceParser()
                ),
                MAX_STATEMENT_FILE_BYTES));

        MessageBus.subscribe(new Object(){

            @MessageListener
            public MessageResult onTagDeleted(TagEvents.Deleted message) {
                Logger.debug("Processing %s", message);

                val transactions = Context.get(TransactionRepository.class);
                val tagId = message.tag().id();
                val personId = message.tag().personId();

                return switch (message.strategy()){
                    case MOVE -> {
                        if (message.targetId() == null) {
                            Logger.warn("Tag \"%s\" deleted with MOVE strategy without a target tag", tagId);
                            yield MessageResult.AVAILABLE;
                        }

                        transactions.reassignTag(tagId, message.targetId(), personId);
                        yield MessageResult.AVAILABLE;
                    }
                    case DETACH -> {
                        transactions.detachTag(tagId, personId);
                        yield MessageResult.AVAILABLE;
                    }
                    default -> {
                        Logger.error("Unsupported DeletionStrategy [%s]", message.strategy());
                        yield MessageResult.AVAILABLE;
                    }
                };
            }
        });

        return Result.success();
    }
}
