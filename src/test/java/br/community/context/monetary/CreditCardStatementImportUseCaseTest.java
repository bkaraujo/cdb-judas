package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.Result;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import br.community.context.monetary._0_domain.event.TransactionEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.service.CardService;
import br.community.context.monetary._1_application.service.CostCenterService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.CardUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import br.community.feature.user.accounts.statement.Issuer;
import br.community.feature.user.accounts.statement.MonetaryDocumentEntry;
import br.community.feature.user.accounts.statement.provider.BTGInvoiceParser;
import br.community.feature.user.accounts.statement.provider.BTGStatementParser;
import br.community.feature.user.accounts.statement.provider.SantanderInvoiceParser;
import br.community.feature.user.accounts.statement.provider.SantanderStatementParser;
import br.community.feature.user.accounts.transactions.core.ChargeKind;
import br.community.feature.user.accounts.transactions.importer.*;
import br.community.feature.user.accounts.transactions.importer.preview.ImportPreview;
import br.community.feature.user.accounts.transactions.importer.preview.ImportPreviewOutcome;
import br.community.feature.user.accounts.transactions.importer.preview.PreviewRow;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardStatementImportUseCaseTest {

    private static final long MAX_BYTES = 4096;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-07-15T12:00:00Z"), ZoneOffset.UTC);

    private static ImportPreview invoicePreview(Result<ImportPreviewOutcome, ImportError> result) {
        var outcome = assertInstanceOf(Result.Success.class, result).value();
        return assertInstanceOf(ImportPreviewOutcome.Invoice.class, outcome).preview();
    }

    private StatementImportUseCase useCaseWith(PdfTextExtractor extractor) {
        return useCaseWith(extractor, new InMemoryRepositories.Accounts());
    }

    private StatementImportUseCase useCaseWith(
            PdfTextExtractor extractor, InMemoryRepositories.Accounts accounts) {
        return useCaseWith(extractor, accounts, new InMemoryRepositories.Transactions());
    }

    private StatementImportUseCase useCaseWith(
            PdfTextExtractor extractor,
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions) {
        return useCaseWith(extractor, accounts, transactions, List.of());
    }

    private StatementImportUseCase useCaseWith(
            PdfTextExtractor extractor,
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions,
            List<CreditCard> cards) {
        final InMemoryRepositories.Cards cardRepo = new InMemoryRepositories.Cards();
        for (val c : cards) {
            cardRepo.save(new Card(c.id(), c.last4(), c.accountId(), true));
        }
        final MonetaryContext monetaryContext = monetaryContext(accounts, transactions, cardRepo);
        final CreditCardProvider provider = () -> cards;
        return new StatementImportUseCase(
                monetaryContext, provider, extractor,
                List.of(new BTGStatementParser(), new SantanderStatementParser(),
                        new BTGInvoiceParser(), new SantanderInvoiceParser()),
                MAX_BYTES, CLOCK);
    }

    private static MonetaryContext monetaryContext(
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions,
            InMemoryRepositories.Cards cardRepo) {
        final AccountService accountService = new AccountService(accounts);
        final BalanceService balanceService = new BalanceService(new InMemoryRepositories.Balances());
        final TransactionService transactionService = new TransactionService(transactions);
        final CostCenterService costCenterService = new CostCenterService(new InMemoryRepositories.CostCenters());
        final CardService cardService = new CardService(cardRepo);
        final AccountUseCase ucAccount = new AccountUseCase(accountService, balanceService);
        final TransactionUseCase ucTransaction = new TransactionUseCase(transactionService, cardService);
        final MetadataUseCase ucMetadata = new MetadataUseCase(costCenterService);
        final CardUseCase ucCard = new CardUseCase(cardService, accountService);
        return new MonetaryContext(ucAccount, ucTransaction, ucMetadata, ucCard);
    }

    /** Cartão do contexto: identificado só pelo last4, sempre vinculado a uma conta real existente. */
    private static CreditCard registerCard(Account account, String last4) {
        return new CreditCard(UUID.randomUUID(), account.id(), account.name(), last4);
    }

    private static Account checking(String name) {
        return new Account(UUID.randomUUID(), name, Account.Type.CHECKING, true);
    }

    private static final PdfTextExtractor NOOP_EXTRACTOR =
            (bytes, password) -> Result.success("unused");

    @Test
    void rejectsOversizedFileBeforeExtracting() {
        var useCase = useCaseWith((bytes, password) -> Result.success("BTG Pactual 30.306.294/0001-45"));
        var result = useCase.preview(new byte[(int) MAX_BYTES + 1], null, null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ImportError.FileTooLarge.class, error);
    }

    @Test
    void mapsEncryptedToPasswordRequired() {
        var useCase = useCaseWith((bytes, password) -> new Result.Failure<>(new ExtractionFailure.Encrypted()));
        var result = useCase.preview(new byte[1], null, null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ImportError.PasswordRequired.class, error);
    }

    @Test
    void mapsTooManyPagesPreservingCounts() {
        var useCase = useCaseWith((bytes, password) -> new Result.Failure<>(new ExtractionFailure.TooManyPages(99, 50)));
        var result = useCase.preview(new byte[1], null, null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        var tooMany = assertInstanceOf(ImportError.TooManyPages.class, error);
        assertEquals(99, tooMany.pages());
        assertEquals(50, tooMany.maxPages());
    }

    @Test
    void rejectsUnknownIssuer() {
        var useCase = useCaseWith((bytes, password) -> Result.success("texto qualquer sem banco"));
        var result = useCase.preview(new byte[1], null, null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ImportError.UnknownIssuer.class, error);
    }

    @Test
    void buildsPreviewWithParsedRowsForRecognizedIssuer() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(Issuer.BTG, preview.issuer());
        assertEquals(java.util.List.of("0020"), preview.last4s());
        assertEquals(1, preview.statement().size());

        MonetaryDocumentEntry row = preview.statement().getFirst();
        assertEquals("Amazonmktplc Megabytem", row.description());
        assertEquals(0, row.amount().compareTo(new BigDecimal("72.99")));
        assertEquals(MonthDay.of(7, 15), MonthDay.from(row.date()));
        assertEquals(9, row.installmentNumber());
        assertEquals(10, row.installmentTotal());
    }

    @Test
    void matchesRegisteredCardByLast4AndExposesCandidates() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var accounts = new InMemoryRepositories.Accounts();
        var bank = checking("Conta");
        accounts.save(bank);
        var matchingCard = registerCard(bank, "0020");
        var otherCard = registerCard(bank, "9999");
        var useCase = useCaseWith((bytes, password) -> Result.success(text), accounts,
                new InMemoryRepositories.Transactions(), List.of(matchingCard, otherCard));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(java.util.List.of(matchingCard), preview.candidateCards());
    }

    @Test
    void aVistaRowCarriesNoInstallment() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 60,00
                Total de compras e despesas
                R$ 60,00Microsoft09 Mar
                R$ 60,00
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));
        MonetaryDocumentEntry row = preview.statement().getFirst();
        assertEquals(1, row.installmentNumber());
        assertEquals(1, row.installmentTotal());
    }

    @Test
    void previewRowsAreExpandedInstallmentSchedule() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(1, preview.statement().size());
        assertEquals(10, preview.rows().size());
        assertEquals(1, preview.rows().getFirst().draft().installmentNumber());
        assertEquals(10, preview.rows().getLast().draft().installmentNumber());
        assertEquals(java.util.List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                preview.rows().stream().map(r -> r.draft().installmentNumber()).toList());
    }

    @Test
    void flagsDuplicateRowWhenExistingTransactionShareGroupId() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var accounts = new InMemoryRepositories.Accounts();
        var bank = checking("Conta");
        accounts.save(bank);
        var card = registerCard(bank, "0020");

        var groupId = new GroupSignature().groupId(bank.id(), LocalDate.of(2024, 7, 15), 10, "Amazonmktplc Megabytem");
        var transactions = new InMemoryRepositories.Transactions();
        transactions.save(new Transaction(
                UUID.randomUUID(), "Amazonmktplc Megabytem", new BigDecimal("72.99"),
                LocalDate.of(2024, 7, 15), bank.id(),
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL_ID, null, groupId, 1, 10, null, null));

        var useCase = useCaseWith((bytes, password) -> Result.success(text), accounts, transactions, List.of(card));
        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(10, preview.rows().size());
        assertTrue(preview.rows().stream().allMatch(PreviewRow::duplicate),
                "every installment of the same group should be flagged duplicate");
    }

    @Test
    void flagsDuplicateRowForAvistaKeyMatch() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 60,00
                Total de compras e despesas
                R$ 60,00Microsoft09 Mar
                R$ 60,00
                """;
        var accounts = new InMemoryRepositories.Accounts();
        var bank = checking("Conta");
        accounts.save(bank);
        var card = registerCard(bank, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        transactions.save(new Transaction(
                UUID.randomUUID(), "MICROSOFT", new BigDecimal("-60.00"),
                LocalDate.of(2025, 3, 9), bank.id(),
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL_ID, null, null, 1, 1, null, null));

        var useCase = useCaseWith((bytes, password) -> Result.success(text), accounts, transactions, List.of(card));
        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(1, preview.rows().size());
        assertTrue(preview.rows().getFirst().duplicate(),
                "à-vista row matching account+date+amount+normalized-description should be a duplicate");
    }

    @Test
    void previewRowsCategoryIdIsNullWhenNoHistory() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(10, preview.rows().size());
        // Category guessing is now done at the feature layer; invoice preview returns null categoryId.
        assertTrue(preview.rows().stream().allMatch(r -> r.categoryId() == null));
    }

    @Test
    void previewRunsSuccessfullyEvenWithTransactionHistory() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        transactions.save(new Transaction(
                UUID.randomUUID(), "AMAZONMKTPLC MEGABYTEM", new BigDecimal("-99.90"),
                LocalDate.of(2024, 1, 10), account.id(),
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL_ID, null, null, 1, 1, null, null));

        var useCase = useCaseWith((bytes, password) -> Result.success(text), accounts, transactions, List.of(card));
        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        // Preview succeeds; category guessing from history is done at the feature layer.
        assertEquals(10, preview.rows().size());
    }

    @Test
    void rowsNotFlaggedDuplicateWhenNoCardMatched() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 60,00
                Total de compras e despesas
                R$ 60,00Microsoft09 Mar
                R$ 60,00
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));
        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertTrue(preview.candidateCards().isEmpty());
        assertEquals(1, preview.rows().size());
        assertFalse(preview.rows().getFirst().duplicate());
        assertEquals(ChargeKind.PURCHASE, preview.rows().getFirst().draft().kind());
    }

    @Test
    void previewSuggestsTheUniquelyMatchedCardPerRow() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 72,99
                Total de compras e despesas
                R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul
                R$ 72,99
                """;
        var accounts = new InMemoryRepositories.Accounts();
        var bank = checking("Conta");
        accounts.save(bank);
        var card = registerCard(bank, "0020");
        var useCase = useCaseWith((bytes, password) -> Result.success(text), accounts,
                new InMemoryRepositories.Transactions(), List.of(card));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(10, preview.rows().size());
        assertTrue(preview.rows().stream().allMatch(r -> card.id().equals(r.suggestedCardId())),
                "every row of a charge on card 0020 should suggest that card");
    }

    @Test
    void previewLeavesSuggestedCardNullWhenNoCardMatches() {
        var text = """
                BTG Pactual S.A CNPJ 30.306.294/0001-45
                Lançamentos do cartão físico | Fulano | Final 0020 Total do cartão: R$ 60,00
                Total de compras e despesas
                R$ 60,00Microsoft09 Mar
                R$ 60,00
                """;
        var useCase = useCaseWith((bytes, password) -> Result.success(text));

        var preview = invoicePreview(useCase.preview(new byte[1], null, null));

        assertEquals(1, preview.rows().size());
        assertNull(preview.rows().getFirst().suggestedCardId());
    }

    // ── confirm ────────────────────────────────────────────────────────────────

    @Test
    void confirmRoutesEachRowToItsOwnCardAccount() {
        var accounts = new InMemoryRepositories.Accounts();
        var accountA = checking("Conta A");
        var accountB = checking("Conta B");
        accounts.save(accountA);
        accounts.save(accountB);
        var cardA = registerCard(accountA, "0020");
        var cardB = registerCard(accountB, "9999");

        var transactions = new InMemoryRepositories.Transactions();
        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(cardA, cardB));
        var categoryId = UUID.randomUUID();

        var rowA = new ImportConfirmCommand.Row(
                "Compra A", new BigDecimal("50.00"), LocalDate.of(2025, 7, 3), LocalDate.of(2025, 7, 3),
                null, null, categoryId, cardA.id());
        var rowB = new ImportConfirmCommand.Row(
                "Compra B", new BigDecimal("70.00"), LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 4),
                null, null, categoryId, cardB.id());

        var cmd = new ImportConfirmCommand(List.of(rowA, rowB));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(cmd)).value();

        assertEquals(2, result.created());
        var saved = transactions.findAll();
        var a = saved.stream().filter(t -> t.description().equals("Compra A")).findFirst().orElseThrow();
        var b = saved.stream().filter(t -> t.description().equals("Compra B")).findFirst().orElseThrow();
        assertEquals(accountA.id(), a.accountId());
        assertEquals(accountB.id(), b.accountId());
    }

    @Test
    void confirmPersistsAvistaAndParceladoRowsOnLinkedAccount() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));
        var categoryId = UUID.randomUUID();

        var avistaConfirmed = new ImportConfirmCommand.Row(
                "Mercado", new BigDecimal("80.00"), LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 10),
                null, null, categoryId, card.id());
        var avistaScheduled = new ImportConfirmCommand.Row(
                "Streaming", new BigDecimal("30.00"), LocalDate.of(2025, 9, 5), LocalDate.of(2025, 9, 5),
                null, null, categoryId, card.id());
        var parc1 = new ImportConfirmCommand.Row(
                "Geladeira", new BigDecimal("100.00"), LocalDate.of(2025, 7, 15), LocalDate.of(2025, 7, 15),
                1, 2, categoryId, card.id());
        var parc2 = new ImportConfirmCommand.Row(
                "Geladeira", new BigDecimal("100.00"), LocalDate.of(2025, 8, 15), LocalDate.of(2025, 7, 15),
                2, 2, categoryId, card.id());

        assertTrue(transactions.findAll().isEmpty());

        var cmd = new ImportConfirmCommand(List.of(avistaConfirmed, avistaScheduled, parc1, parc2));
        var result = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(cmd)).value();

        assertEquals(4, result.created());
        assertEquals(0, result.skipped());

        var saved = transactions.findAll();
        assertEquals(4, saved.size());
        assertTrue(saved.stream().allMatch(t -> account.id().equals(t.accountId())));
        assertTrue(saved.stream().allMatch(t -> Transaction.Type.EXPENSE.equals(t.type())));
        assertTrue(saved.stream().allMatch(t -> t.amount().signum() > 0));
        assertTrue(saved.stream().allMatch(t -> t.signal() == -1));

        var mercado = saved.stream().filter(t -> t.description().equals("Mercado")).findFirst().orElseThrow();
        assertEquals(Transaction.Status.CONFIRMED, mercado.status());
        var streaming = saved.stream().filter(t -> t.description().equals("Streaming")).findFirst().orElseThrow();
        assertEquals(Transaction.Status.SCHEDULED, streaming.status());

        var parcelas = saved.stream().filter(t -> t.description().equals("Geladeira")).toList();
        assertEquals(2, parcelas.size());
        assertEquals(1L, parcelas.stream().map(Transaction::groupId).distinct().count());
        assertTrue(parcelas.stream().allMatch(t -> t.groupId() != null));
        assertEquals(List.of(1, 2), parcelas.stream().map(Transaction::installmentNumber).sorted().toList());
        assertTrue(parcelas.stream().allMatch(t -> Integer.valueOf(2).equals(t.totalInstallments())));
        var parcelaConfirmed = parcelas.stream().filter(t -> Integer.valueOf(1).equals(t.installmentNumber())).findFirst().orElseThrow();
        assertEquals(Transaction.Status.CONFIRMED, parcelaConfirmed.status());
        var parcelaScheduled = parcelas.stream().filter(t -> Integer.valueOf(2).equals(t.installmentNumber())).findFirst().orElseThrow();
        assertEquals(Transaction.Status.SCHEDULED, parcelaScheduled.status());
    }

    @Test
    void confirmEmitsOneTransactionCreatedEventPerCreatedRow() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));
        var categoryId = UUID.randomUUID();

        var counter = new AtomicInteger(0);
        MessageBus.subscribe(new Object() {
            @br.commons.framework.message.MessageListener
            public br.commons.framework.message.MessageResult on(TransactionEvents.Created event) {
                counter.incrementAndGet();
                return br.commons.framework.message.MessageResult.AVAILABLE;
            }
        });

        var row1 = new ImportConfirmCommand.Row(
                "Padaria", new BigDecimal("12.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, categoryId, card.id());
        var row2 = new ImportConfirmCommand.Row(
                "Farmácia", new BigDecimal("45.00"), LocalDate.of(2025, 7, 2), LocalDate.of(2025, 7, 2),
                null, null, categoryId, card.id());

        int before = counter.get();
        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new ImportConfirmCommand(List.of(row1, row2)))).value();

        assertEquals(2, result.created());
        assertEquals(2, counter.get() - before);
    }

    @Test
    void confirmIsIdempotentOnReimportOfSameCommand() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));
        var categoryId = UUID.randomUUID();

        var avista = new ImportConfirmCommand.Row(
                "Uber", new BigDecimal("25.00"), LocalDate.of(2025, 7, 8), LocalDate.of(2025, 7, 8),
                null, null, categoryId, card.id());
        var parc1 = new ImportConfirmCommand.Row(
                "Notebook", new BigDecimal("200.00"), LocalDate.of(2025, 7, 15), LocalDate.of(2025, 7, 15),
                3, 4, categoryId, card.id());
        var parc2 = new ImportConfirmCommand.Row(
                "Notebook", new BigDecimal("200.00"), LocalDate.of(2025, 8, 15), LocalDate.of(2025, 7, 15),
                4, 4, categoryId, card.id());
        var cmd = new ImportConfirmCommand(List.of(avista, parc1, parc2));

        var first = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(cmd)).value();
        assertEquals(3, first.created());
        assertEquals(0, first.skipped());
        assertEquals(3, transactions.findAll().size());

        var second = (ImportResult) assertInstanceOf(Result.Success.class, useCase.confirm(cmd)).value();
        assertEquals(0, second.created());
        assertEquals(cmd.rows().size(), second.skipped());
        assertEquals(3, transactions.findAll().size());
    }

    @Test
    void confirmSkipsAvistaDuplicateAgainstExistingTransaction() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions();
        transactions.save(new Transaction(
                UUID.randomUUID(), "MERCADO LIVRE", new BigDecimal("-90.00"),
                LocalDate.of(2025, 7, 4), account.id(),
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL_ID, null, null, 1, 1, null, null));

        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));

        var row = new ImportConfirmCommand.Row(
                "Mercado Livre", new BigDecimal("90.00"), LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 4),
                null, null, UUID.randomUUID(), card.id());
        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new ImportConfirmCommand(List.of(row)))).value();

        assertEquals(0, result.created());
        assertEquals(1, result.skipped());
        assertEquals(1, transactions.findAll().size());
    }

    @Test
    void confirmFailsWhenRowCardNotFound() {
        var useCase = useCaseWith(NOOP_EXTRACTOR);
        var row = new ImportConfirmCommand.Row(
                "Compra", new BigDecimal("10.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, UUID.randomUUID(), UUID.randomUUID());
        var cmd = new ImportConfirmCommand(List.of(row));
        var error = assertInstanceOf(Result.Failure.class, useCase.confirm(cmd)).error();
        assertInstanceOf(br.community.context.shared._0_domain.model.DomainError.NotFound.class, error);
    }

    @Test
    void confirmIsBestEffortWhenOneRowFailsToPersist() {
        var accounts = new InMemoryRepositories.Accounts();
        var account = checking("Conta");
        accounts.save(account);
        var card = registerCard(account, "0020");

        var transactions = new InMemoryRepositories.Transactions() {
            @Override
            public Transaction save(@NonNull Transaction e) {
                if ("BOOM".equals(e.description())) throw new RuntimeException("simulated save failure");
                return super.save(e);
            }
        };
        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));
        var categoryId = UUID.randomUUID();

        var ok1 = new ImportConfirmCommand.Row(
                "Antes", new BigDecimal("10.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, categoryId, card.id());
        var boom = new ImportConfirmCommand.Row(
                "BOOM", new BigDecimal("20.00"), LocalDate.of(2025, 7, 2), LocalDate.of(2025, 7, 2),
                null, null, categoryId, card.id());
        var ok2 = new ImportConfirmCommand.Row(
                "Depois", new BigDecimal("30.00"), LocalDate.of(2025, 7, 3), LocalDate.of(2025, 7, 3),
                null, null, categoryId, card.id());

        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new ImportConfirmCommand(List.of(ok1, boom, ok2)))).value();

        assertEquals(2, result.created());
        var saved = transactions.findAll();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(t -> t.description().equals("Antes")));
        assertTrue(saved.stream().anyMatch(t -> t.description().equals("Depois")));
        assertFalse(saved.stream().anyMatch(t -> t.description().equals("BOOM")));
    }
}
