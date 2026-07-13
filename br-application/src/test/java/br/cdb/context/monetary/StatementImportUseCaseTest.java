package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CardUseCase;
import br.cdb.context.monetary._1_application.usecase.MetadataUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.feature.user.accounts.statement.provider.BTGInvoiceParser;
import br.cdb.feature.user.accounts.statement.provider.BTGStatementParser;
import br.cdb.feature.user.accounts.statement.provider.SantanderInvoiceParser;
import br.cdb.feature.user.accounts.statement.provider.SantanderStatementParser;
import br.cdb.feature.user.accounts.transactions.importer.ImportError;
import br.cdb.feature.user.accounts.transactions.importer.ImportResult;
import br.cdb.feature.user.accounts.transactions.importer.StatementImportUseCase;
import br.cdb.feature.user.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.cdb.feature.user.accounts.transactions.importer.preview.BankStatementPreview;
import br.cdb.feature.user.accounts.transactions.importer.preview.ImportPreviewOutcome;
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
class StatementImportUseCaseTest {

    private static final long MAX_BYTES = 4096;
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

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID()),
                new BankStatementConfirmCommand.Row("Caixa Economica", new BigDecimal("3000.00"), LocalDate.of(2025, 3, 6), Transaction.Type.INCOME, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

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

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

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

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirmStatement(cmd)).value();

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

        var cmd = new BankStatementConfirmCommand(account.id(), List.of(
                new BankStatementConfirmCommand.Row("Odontoprev", new BigDecimal("-161.43"), LocalDate.of(2025, 3, 5), Transaction.Type.EXPENSE, UUID.randomUUID())));
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
        final MonetaryContext monetaryContext = monetaryContext(accounts, transactions);
        return new StatementImportUseCase(
                monetaryContext, List::of, extractor, // empty card provider (bank-statement path)
                List.of(new BTGStatementParser(), new SantanderStatementParser(),
                        new BTGInvoiceParser(), new SantanderInvoiceParser()),
                MAX_BYTES, CLOCK);
    }

    private static MonetaryContext monetaryContext(
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions) {
        final AccountService accountService = new AccountService(accounts);
        final BalanceService balanceService = new BalanceService(new InMemoryRepositories.Balances());
        final TransactionService transactionService = new TransactionService(transactions);
        final CostCenterService costCenterService = new CostCenterService(new InMemoryRepositories.CostCenters());
        final CardService cardService = new CardService(new InMemoryRepositories.Cards());
        final BalanceRecalculationService balanceRecalculationService =
                new BalanceRecalculationService(accountService, balanceService, transactionService);
        final AccountUseCase ucAccount = new AccountUseCase(accountService, balanceService, transactionService, cardService, balanceRecalculationService);
        final TransactionUseCase ucTransaction = new TransactionUseCase(transactionService, cardService, balanceRecalculationService);
        final MetadataUseCase ucMetadata = new MetadataUseCase(costCenterService);
        final CardUseCase ucCard = new CardUseCase(cardService, accountService, transactionService, balanceRecalculationService);
        return new MonetaryContext(ucAccount, ucTransaction, ucMetadata, ucCard);
    }

    private static Account checking(String name) {
        return new Account(UUID.randomUUID(), name, Account.Type.CHECKING, true);
    }
}
