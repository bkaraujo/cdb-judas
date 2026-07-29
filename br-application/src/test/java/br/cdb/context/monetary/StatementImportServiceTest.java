package br.cdb.context.monetary;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f000._1_application.usecase.CostCenterUseCase;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f002._1_application.usecase.AccountUseCase;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f006._0_domain.ImportError;
import br.cdb.feature.f006._0_domain.ImportResult;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._1_application.StatementImportService;
import br.cdb.feature.f006._1_application.TransactionOverlayListener;
import br.cdb.feature.f006._1_application.UserTransactionService;
import br.cdb.feature.f006._1_application.confirm.StatementConfirmCommand;
import br.cdb.feature.f006._1_application.preview.BankStatementPreview;
import br.cdb.feature.f006._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.cdb.feature.f006._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderStatementParser;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.pdf.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Bank-statement (extrato) import: detection, signed typing, reconciliation and dedup. */
class StatementImportServiceTest {

    private static final long MAX_BYTES = 4096;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-07-15T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID PERSON = UUID.randomUUID();

    private InMemoryUserTransactions overlays = new InMemoryUserTransactions();

    private static final String EXTRATO = String.join("\n",
            "Extrato de conta corrente",
            "Este é o extrato da sua conta corrente BTG Pactual",
            "Data e hora Categoria Transação Descrição Valor",
            "05/03/2025 10h42 Saúde Pagamento de boleto Odontoprev -R$ 161,43",
            "06/03/2025 02h17 Crédito e Financiamento Pix recebido Caixa Economica R$ 3.000,00",
            "07/03/2025 23h59 Saldo Diário R$ 100,00",
            "08/03/2025 11h52 Contas Pagamento de fatura do cartão Fatura do cartão BTG Pactual -R$ 232,97");

    @Test
    void previewDetectsStatementSignsRowsAndDropsBalanceAndCardPayment() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);

        var useCase = useCaseWith(accounts, new InMemoryRepositories.Transactions());
        var preview = statementPreview(useCase.preview(PERSON.toString(), new byte[1], null, account.id()));

        assertEquals(2, preview.rows().size(), "saldo diário and card payment must be dropped");
        var odonto = preview.rows().stream().filter(r -> r.description().contains("Odontoprev")).findFirst().orElseThrow();
        assertEquals(Transaction.Type.EXPENSE, odonto.type());
        assertEquals(0, odonto.amount().compareTo(new BigDecimal("-161.43")));
        var pix = preview.rows().stream().filter(r -> r.description().contains("Caixa")).findFirst().orElseThrow();
        assertEquals(Transaction.Type.INCOME, pix.type());
        assertEquals(0, pix.amount().compareTo(new BigDecimal("3000.00")));
        assertEquals(account.id(), preview.selectedAccountId());
    }

    @Test
    void confirmInsertsNewRowsWithSignTypeAndStatus() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var useCase = useCaseWith(accounts, transactions);

        var catOdonto = UUID.randomUUID();
        var catPix = UUID.randomUUID();
        var cmd = new StatementConfirmCommand(account.id(), List.of(
                new StatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, catOdonto),
                new StatementConfirmCommand.Row("Caixa Economica", new BigDecimal("3000.00"), LocalDate.of(2025, 3, 6), Transaction.Type.INCOME, catPix)));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(PERSON, cmd)).value();

        assertEquals(2, result.created());
        assertEquals(0, result.reconciled());
        assertEquals(0, result.skipped());

        var saved = transactions.findAll();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(t -> account.id().equals(t.accountId())));
        var odonto = saved.stream().filter(t -> t.description().equals("Odontoprev")).findFirst().orElseThrow();
        assertEquals(Transaction.Type.EXPENSE, odonto.type());
        assertEquals(1, odonto.amount().signum(), "stored amount is always positive");
        assertEquals(-1, odonto.signal());
        assertEquals(Transaction.Status.CONFIRMED, odonto.status());
        var pix = saved.stream().filter(t -> t.description().equals("Caixa Economica")).findFirst().orElseThrow();
        assertEquals(Transaction.Type.INCOME, pix.type());
        assertEquals(1, pix.amount().signum(), "imported income must be stored positive");

        // 1:1 com F006_TRANSACTION: cada linha importada cria vínculo F005_TRANSACTION_CATEGORY com a categoria da linha.
        assertEquals(2, overlays.all().size(), "cada transação importada gera vínculo F005_TRANSACTION_CATEGORY");
        assertTrue(overlays.all().stream().allMatch(o -> PERSON.equals(o.personId())));
        assertEquals(catOdonto, overlays.findByTransactionAndPerson(odonto.id(), PERSON).orElseThrow().categoryId());
        assertEquals(catPix, overlays.findByTransactionAndPerson(pix.id(), PERSON).orElseThrow().categoryId());
    }

    @Test
    void reconcilePromotesPendingManualMatchWithinWindowWithoutInserting() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var manual = new Transaction(
                UUID.randomUUID(), "Dentista", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 4),
                account.id(), Transaction.Status.PENDING, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null);
        transactions.save(manual);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new StatementConfirmCommand(account.id(), List.of(
                new StatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(PERSON, cmd)).value();

        assertEquals(0, result.created());
        assertEquals(1, result.reconciled());
        assertEquals(0, result.skipped());
        assertEquals(1, transactions.findAll().size(), "reconcile must not insert a second row");
        assertEquals(Transaction.Status.CONFIRMED, transactions.findById(manual.id()).orElseThrow().status());
    }

    @Test
    void doesNotReconcileOutsideTheDateWindow() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var manual = new Transaction(
                UUID.randomUUID(), "Dentista", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 1),
                account.id(), Transaction.Status.PENDING, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null);
        transactions.save(manual);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new StatementConfirmCommand(account.id(), List.of(
                new StatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(PERSON, cmd)).value();

        assertEquals(1, result.created());
        assertEquals(0, result.reconciled());
        assertEquals(2, transactions.findAll().size());
        assertEquals(Transaction.Status.PENDING, transactions.findById(manual.id()).orElseThrow().status());
    }

    @Test
    void skipsAlreadyImportedIdenticalRow() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var existing = new Transaction(
                UUID.randomUUID(), "Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5),
                account.id(), Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null);
        transactions.save(existing);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new StatementConfirmCommand(account.id(), List.of(
                new StatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(PERSON, cmd)).value();

        assertEquals(0, result.created());
        assertEquals(0, result.reconciled());
        assertEquals(1, result.skipped());
        assertEquals(1, transactions.findAll().size());
    }

    // ── wiring ─────────────────────────────────────────────────────────────────

    private static BankStatementPreview statementPreview(Result<ImportPreviewOutcome, ImportError> result) {
        var outcome = assertInstanceOf(Result.Success.class, result).value();
        return assertInstanceOf(ImportPreviewOutcome.Statement.class, outcome).preview();
    }

    private StatementImportService useCaseWith(InMemoryRepositories.Accounts accounts, InMemoryRepositories.Transactions transactions) {
        final PdfTextExtractor extractor = (bytes, password) -> Result.success(EXTRATO);
        resetMonetaryRegistry(accounts, transactions);
        this.overlays = new InMemoryUserTransactions();
        // O save do vínculo agora é reação a TransactionImported (f000, publicado pelos processors) —
        // sem @QuarkusTest não há StartupEvent pra assinar o listener real, então assina aqui.
        MessageBus.subscribe(new TransactionOverlayListener(new UserTransactionService(overlays)));
        return new StatementImportService(
                List::of, extractor, // empty card provider (bank-statement path)
                List.of(new BTGStatementParser(), new SantanderStatementParser(),
                        new BTGInvoiceParser(), new SantanderInvoiceParser()),
                MAX_BYTES, CLOCK);
    }

    /**
     * Reseta o grafo Registry-wired do contexto (services/use cases se auto-conectam via
     * Registry.tryGet — sem isso, a chamada seguinte reaproveitaria os singletons presos aos fakes
     * do teste anterior) e publica fakes novos antes de construir o {@code StatementImportService},
     * cujos campos resolvem os use cases via {@code Registry.tryGet(...)} na construção.
     */
    private static void resetMonetaryRegistry(
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions) {
        MessageBus.reset();
        Registry.remove(AccountService.class);
        Registry.remove(BalanceService.class);
        Registry.remove(TransactionService.class);
        Registry.remove(CreditCardService.class);
        Registry.remove(CostCenterService.class);
        Registry.remove(AccountUseCase.class);
        Registry.remove(TransactionUseCase.class);
        Registry.remove(CostCenterUseCase.class);
        Registry.remove(CreditCardUseCase.class);

        Registry.set(AccountRepository.class, () -> accounts);
        Registry.set(BalanceRepository.class, InMemoryRepositories.Balances::new);
        Registry.set(TransactionRepository.class, () -> transactions);
        Registry.set(CostCenterRepository.class, InMemoryRepositories.CostCenters::new);
        Registry.set(CreditCardRepository.class, InMemoryRepositories.Cards::new);
    }

    private static Account checking(String name) {
        return new Account(UUID.randomUUID(), name, Account.Type.CHECKING, true);
    }
}
