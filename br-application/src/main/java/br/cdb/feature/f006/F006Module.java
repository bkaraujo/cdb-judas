package br.cdb.feature.f006;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f000._0_domain.event.TransactionImported;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f006._0_domain.event.TransactionEvents;
import br.cdb.feature.f006._0_domain.repository.TransactionCategoryRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f006._2_infrastructure.F006ApiImpl;
import br.cdb.feature.f006._2_infrastructure.persistence.TransactionCategoryJDBCRepository;
import br.cdb.feature.f006._2_infrastructure.persistence.TransactionJDBCRepository;
import br.cdb.feature.f006._2_infrastructure.persistence.TransactionTagJDBCRepository;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f006} (lançamentos + transferência). O par CQRS {@link ReadUseCases}/
 * {@link WriteUseCases} monta-se sozinho no {@link Context} ({@code Context.tryGet(...)} no primeiro
 * consumidor), como as demais classes ex-contexto. Importação de extrato/fatura é fatia própria
 * ({@code f007}) desde a extração desta de {@code .claude/plan.md}.
 */
@NullMarked
public class F006Module implements Lifecycle {

    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F006_TRANSACTION (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    NUM_SIGNAL NUMERIC(1) NOT NULL,
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
                """,
                """
                CREATE TABLE F006_TRANSACTION_CATEGORY (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_CATEGORY CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON)
                )
                """,
                """
                CREATE TABLE F006_TRANSACTION_TAG (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_TAG CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON, COD_TAG)
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(TransactionRepository.class, TransactionJDBCRepository::new);
        Context.set(TransactionCategoryRepository.class, TransactionCategoryJDBCRepository::new);
        Context.set(TransactionTagRepository.class, TransactionTagJDBCRepository::new);
        Context.set(F006Api.class, F006ApiImpl::new);

        MessageBus.subscribe(new Object(){

            @MessageListener
            public MessageResult onTagDeleted(TagEvents.Deleted message) {
                Logger.debug("Processing %s", message);

                val transactionTags = Context.get(TransactionTagRepository.class);
                val tagId = message.tag().id();
                val personId = message.tag().personId();

                return switch (message.strategy()){
                    case MOVE -> {
                        if (message.targetId() == null) {
                            Logger.warn("Tag \"%s\" deleted with MOVE strategy without a target tag", tagId);
                            yield MessageResult.AVAILABLE;
                        }

                        transactionTags.reassignTag(tagId, message.targetId(), personId);
                        yield MessageResult.AVAILABLE;
                    }
                    case DETACH -> {
                        transactionTags.detachTag(tagId, personId);
                        yield MessageResult.AVAILABLE;
                    }
                    default -> {
                        Logger.error("Unsupported DeletionStrategy [%s]", message.strategy());
                        yield MessageResult.AVAILABLE;
                    }
                };
            }
        });


        MessageBus.subscribe(new Object(){

            /**
             * Dono dos vínculos {@code F006_TRANSACTION_CATEGORY}/{@code F006_TRANSACTION_TAG}
             * reagindo a eventos de fatias vizinhas — best-effort, nunca falha por si só o request
             * que originou o evento (mas propaga exceção, ver {@code MessageBus#submit}, revertendo a
             * transação do publicador se o próprio save/reassign/delete falhar). A reação à
             * exclusão/merge da própria {@code Tag} (MOVE/DETACH) é a assinatura acima.
             */
            private static WriteUseCases writes() {
                return Context.tryGet(WriteUseCases.class);
            }

            /** Limpa os vínculos das transações apagadas, qualquer que seja o publicador (o próprio
             *  {@code WriteUseCases#deleteTransaction} ou uma exclusão em cascata de outra fatia, ex.: conta). */
            @MessageListener
            public MessageResult onTransactionsDeleted(TransactionsDeleted message) {
                Logger.debug("Processing %s", message);
                message.transactionIds().forEach(id -> {
                    writes().deleteCategory(id);
                    writes().deleteTags(id);
                });
                return MessageResult.CONSUMED;
            }

            /** Grava os vínculos de uma transação importada ({@code InvoiceImportProcessor}/
             *  {@code StatementImportProcessor}) — mantém o 1:1 com {@code F006_TRANSACTION}. */
            @MessageListener
            public MessageResult onTransactionImported(TransactionImported message) {
                Logger.debug("Processing %s", message);
                writes().saveCategory(message.transactionId(), message.personId(), message.categoryId());
                writes().saveTags(message.transactionId(), message.personId(), message.tagIds());
                return MessageResult.CONSUMED;
            }

            /** Re-keya o vínculo da subárvore de categoria apagada (estratégia MOVE, f005) antes da subárvore sumir. */
            @MessageListener
            public MessageResult onCategoryReassigned(CategoryReassigned message) {
                Logger.debug("Processing %s", message);
                message.oldCategoryIds().forEach(oldId ->
                        writes().reassignCategory(oldId, message.newCategoryId(), message.personId()));
                return MessageResult.CONSUMED;
            }
        });

        MessageBus.subscribe(new Object(){

            private final BalanceService service = Context.tryGet(BalanceService.class);

            @MessageListener
            public MessageResult onTransaction(TransactionEvents.Created message) {
                Logger.debug("Processing %s", message);
                service.recalculate(message.transaction().accountId());
                return MessageResult.CONSUMED;
            }

            @MessageListener
            public MessageResult onTransaction(TransactionEvents.Updated message) {
                Logger.debug("Processing %s", message);
                service.recalculate(message.transaction().accountId());
                return MessageResult.CONSUMED;
            }

            @MessageListener
            public MessageResult onTransaction(TransactionEvents.Deleted message) {
                Logger.debug("Processing %s", message);
                service.recalculate(message.transaction().accountId());
                return MessageResult.CONSUMED;
            }
        });

        return Result.success();
    }
}
