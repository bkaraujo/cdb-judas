package br.community.context.monetary;

import br.commons.Result;
import br.commons.pdf.PdfTextExtractor;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.service.*;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import br.community.feature.user.accounts.statement.provider.BTGInvoiceParser;
import br.community.feature.user.accounts.statement.provider.BTGStatementParser;
import br.community.feature.user.accounts.statement.provider.SantanderInvoiceParser;
import br.community.feature.user.accounts.statement.provider.SantanderStatementParser;
import br.community.feature.user.accounts.transactions.importer.ImportError;
import br.community.feature.user.accounts.transactions.importer.ImportResult;
import br.community.feature.user.accounts.transactions.importer.StatementImportUseCase;
import br.community.feature.user.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.community.feature.user.accounts.transactions.importer.preview.BankStatementPreview;
import br.community.feature.user.accounts.transactions.importer.preview.ImportPreviewOutcome;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Bank-statement (extrato) import: detection, signed typing, reconciliation and dedup, driven
 *  through a real {@link br.community.context.monetary.MonetaryContext} over in-memory repositories. */
class StatementImportUseCaseTest {

    private static final long MAX_BYTES = 4096;
    // Fixed today after the fixture dates → every March 2025 movement is "confirmed".
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-07-15T12:00:00Z"), ZoneOffset.UTC);

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
        var preview = statementPreview(useCase.preview(new byte[1], null, account.id()));

        assertEquals(2, preview.rows().size(), "saldo diário and card payment must be dropped");
        var odonto = preview.rows().stream().filter(r -> r.description().contains("Odontoprev")).findFirst().orElseThrow();
        assertEquals(MonetaryTransaction.Type.EXPENSE, odonto.type());
        assertEquals(0, odonto.amount().compareTo(new BigDecimal("-161.43")));
        var pix = preview.rows().stream().filter(r -> r.description().contains("Caixa")).findFirst().orElseThrow();
        assertEquals(MonetaryTransaction.Type.INCOME, pix.type());
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

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), MonetaryTransaction.Type.EXPENSE, UUID.randomUUID()),
                new BankStatementConfirmCommand.Row("Caixa Economica", new BigDecimal("3000.00"), LocalDate.of(2025, 3, 6), MonetaryTransaction.Type.INCOME, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

        assertEquals(2, result.created());
        assertEquals(0, result.reconciled());
        assertEquals(0, result.skipped());

        var saved = transactions.findAll();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(t -> account.id().equals(t.accountId())));
        var odonto = saved.stream().filter(t -> t.description().equals("Odontoprev")).findFirst().orElseThrow();
        assertEquals(MonetaryTransaction.Type.EXPENSE, odonto.type());
        assertEquals(-1, odonto.amount().signum());
        assertEquals(MonetaryTransaction.Status.CONFIRMED, odonto.status());
        var pix = saved.stream().filter(t -> t.description().equals("Caixa Economica")).findFirst().orElseThrow();
        assertEquals(MonetaryTransaction.Type.INCOME, pix.type());
        assertEquals(1, pix.amount().signum(), "imported income must be stored positive");
    }

    @Test
    void reconcilePromotesPendingManualMatchWithinWindowWithoutInserting() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        // A manual, still-pending expense the user typed with a slightly different date and description.
        var manual = new MonetaryTransaction(
                UUID.randomUUID(), "Dentista", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 4),
                UUID.randomUUID(), account.id(), MonetaryTransaction.Status.PENDING, MonetaryTransaction.Type.EXPENSE, MonetaryCenter.VARIAVEL_ID, null, null, 1, 1, null);
        transactions.save(manual);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), MonetaryTransaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

        assertEquals(0, result.created());
        assertEquals(1, result.reconciled());
        assertEquals(0, result.skipped());
        assertEquals(1, transactions.findAll().size(), "reconcile must not insert a second row");
        assertEquals(MonetaryTransaction.Status.CONFIRMED, transactions.findById(manual.id()).orElseThrow().status());
    }

    @Test
    void doesNotReconcileOutsideTheDateWindow() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var manual = new MonetaryTransaction(
                UUID.randomUUID(), "Dentista", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 1),
                UUID.randomUUID(), account.id(), MonetaryTransaction.Status.PENDING, MonetaryTransaction.Type.EXPENSE, MonetaryCenter.VARIAVEL_ID, null, null, 1, 1, null);
        transactions.save(manual);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), MonetaryTransaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

        assertEquals(1, result.created());
        assertEquals(0, result.reconciled());
        assertEquals(2, transactions.findAll().size());
        assertEquals(MonetaryTransaction.Status.PENDING, transactions.findById(manual.id()).orElseThrow().status());
    }

    @Test
    void skipsAlreadyImportedIdenticalRow() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta BTG");
        accounts.save(account);
        var transactions = new InMemoryRepositories.Transactions();
        var existing = new MonetaryTransaction(
                UUID.randomUUID(), "Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5),
                UUID.randomUUID(), account.id(), MonetaryTransaction.Status.CONFIRMED, MonetaryTransaction.Type.EXPENSE, MonetaryCenter.VARIAVEL_ID, null, null, 1, 1, null);
        transactions.save(existing);
        var useCase = useCaseWith(accounts, transactions);

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), MonetaryTransaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

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

    private StatementImportUseCase useCaseWith(InMemoryRepositories.Accounts accounts, InMemoryRepositories.Transactions transactions) {
        final PdfTextExtractor extractor = (bytes, password) -> Result.success(EXTRATO);
        final MonetaryContext monetaryContext = monetaryContext(accounts, transactions, new InMemoryRepositories.Categories());
        return new StatementImportUseCase(
                monetaryContext, extractor,
                List.of(new BTGStatementParser(), new SantanderStatementParser(),
                        new BTGInvoiceParser(), new SantanderInvoiceParser()),
                MAX_BYTES, CLOCK);
    }

    private static MonetaryContext monetaryContext(
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions,
            InMemoryRepositories.Categories categories) {
        final AccountService accountService = new AccountService(accounts);
        final BalanceService balanceService = new BalanceService(new InMemoryRepositories.Balances());
        final TransactionService transactionService = new TransactionService(transactions);
        final CategoryService categoryService = new CategoryService(categories);
        final TagService tagService = new TagService(new InMemoryRepositories.Tags());
        final CostCenterService costCenterService = new CostCenterService(new InMemoryRepositories.CostCenters());
        final AccountUseCase ucAccount = new AccountUseCase(accountService, balanceService);
        final TransactionUseCase ucTransaction = new TransactionUseCase(transactionService, categoryService);
        final MetadataUseCase ucMetadata =
                new MetadataUseCase(tagService, categoryService, costCenterService, transactionService);
        return new MonetaryContext(ucAccount, ucTransaction, ucMetadata);
    }

    private static MonetaryAccount checking(String name) {
        return new MonetaryAccount(
                UUID.randomUUID(), name, BigDecimal.ZERO, MonetaryAccount.Type.CHECKING, "#000", true, null, Map.of());
    }
}
