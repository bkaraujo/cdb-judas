package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.event.TransactionEvents;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CostCenterUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f006._0_domain.MonetaryDocumentEntry;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.BTGStatementParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderInvoiceParser;
import br.cdb.feature.f006._2_infrastructure.provider.SantanderStatementParser;
import br.cdb.feature.f006._0_domain.ChargeKind;
import br.cdb.feature.f006._0_domain.*;
import br.cdb.feature.f006._1_application.*;
import br.cdb.feature.f006._2_infrastructure.*;
import br.cdb.feature.f006._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f006._1_application.preview.ImportPreview;
import br.cdb.feature.f006._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f006._1_application.preview.PreviewRow;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardStatementImportServiceTest {

    private static final long MAX_BYTES = 4096;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-07-15T12:00:00Z"), ZoneOffset.UTC);

    private static ImportPreview invoicePreview(Result<ImportPreviewOutcome, ImportError> result) {
        var outcome = assertInstanceOf(Result.Success.class, result).value();
        return assertInstanceOf(ImportPreviewOutcome.Invoice.class, outcome).preview();
    }

    private StatementImportService useCaseWith(PdfTextExtractor extractor) {
        return useCaseWith(extractor, new InMemoryRepositories.Accounts());
    }

    private StatementImportService useCaseWith(
            PdfTextExtractor extractor, InMemoryRepositories.Accounts accounts) {
        return useCaseWith(extractor, accounts, new InMemoryRepositories.Transactions());
    }

    private StatementImportService useCaseWith(
            PdfTextExtractor extractor,
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions) {
        return useCaseWith(extractor, accounts, transactions, List.of());
    }

    private StatementImportService useCaseWith(
            PdfTextExtractor extractor,
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions,
            List<CreditCard> creditCards) {
        final InMemoryRepositories.Cards cardRepo = new InMemoryRepositories.Cards();
        for (val c : creditCards) {
            cardRepo.save(c);
        }
        resetMonetaryRegistry(accounts, transactions, cardRepo);
        final CreditCardProvider provider = () -> creditCards;
        return new StatementImportService(
                provider, extractor,
                List.of(new BTGStatementParser(), new SantanderStatementParser(),
                        new BTGInvoiceParser(), new SantanderInvoiceParser()),
                MAX_BYTES, CLOCK);
    }

    /**
     * Reseta o grafo Registry-wired do contexto (services/use cases se auto-conectam via
     * Registry.tryGet — sem isso, a chamada seguinte reaproveitaria os singletons presos aos fakes
     * do teste anterior) e publica fakes novos antes de construir o {@code StatementImportService},
     * cujos campos resolvem os use cases via {@code MonetaryUseCases.uc*()} na construção.
     */
    private static void resetMonetaryRegistry(
            InMemoryRepositories.Accounts accounts,
            InMemoryRepositories.Transactions transactions,
            InMemoryRepositories.Cards cardRepo) {
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

        Registry.set(AccountRepository.class, accounts);
        Registry.set(BalanceRepository.class, new InMemoryRepositories.Balances());
        Registry.set(TransactionRepository.class, transactions);
        Registry.set(CostCenterRepository.class, new InMemoryRepositories.CostCenters());
        Registry.set(CreditCardRepository.class, cardRepo);
    }

    /** Cartão do contexto: identificado só pelo last4, sempre vinculado a uma conta real existente. */
    private static CreditCard registerCard(Account account, String last4) {
        return new CreditCard(UUID.randomUUID(), last4, account.id(), true);
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

        assertEquals("BTG Pactual", preview.issuer());
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
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, groupId, 1, 10, null, null));

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
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null));

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
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null));

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

        var rowA = new InvoiceConfirmCommand.Row(
                "Compra A", new BigDecimal("50.00"), LocalDate.of(2025, 7, 3), LocalDate.of(2025, 7, 3),
                null, null, categoryId, cardA.id());
        var rowB = new InvoiceConfirmCommand.Row(
                "Compra B", new BigDecimal("70.00"), LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 4),
                null, null, categoryId, cardB.id());

        var cmd = new InvoiceConfirmCommand(List.of(rowA, rowB));
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

        var avistaConfirmed = new InvoiceConfirmCommand.Row(
                "Mercado", new BigDecimal("80.00"), LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 10),
                null, null, categoryId, card.id());
        var avistaScheduled = new InvoiceConfirmCommand.Row(
                "Streaming", new BigDecimal("30.00"), LocalDate.of(2025, 9, 5), LocalDate.of(2025, 9, 5),
                null, null, categoryId, card.id());
        var parc1 = new InvoiceConfirmCommand.Row(
                "Geladeira", new BigDecimal("100.00"), LocalDate.of(2025, 7, 15), LocalDate.of(2025, 7, 15),
                1, 2, categoryId, card.id());
        var parc2 = new InvoiceConfirmCommand.Row(
                "Geladeira", new BigDecimal("100.00"), LocalDate.of(2025, 8, 15), LocalDate.of(2025, 7, 15),
                2, 2, categoryId, card.id());

        assertTrue(transactions.findAll().isEmpty());

        var cmd = new InvoiceConfirmCommand(List.of(avistaConfirmed, avistaScheduled, parc1, parc2));
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

        var row1 = new InvoiceConfirmCommand.Row(
                "Padaria", new BigDecimal("12.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, categoryId, card.id());
        var row2 = new InvoiceConfirmCommand.Row(
                "Farmácia", new BigDecimal("45.00"), LocalDate.of(2025, 7, 2), LocalDate.of(2025, 7, 2),
                null, null, categoryId, card.id());

        int before = counter.get();
        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new InvoiceConfirmCommand(List.of(row1, row2)))).value();

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

        var avista = new InvoiceConfirmCommand.Row(
                "Uber", new BigDecimal("25.00"), LocalDate.of(2025, 7, 8), LocalDate.of(2025, 7, 8),
                null, null, categoryId, card.id());
        var parc1 = new InvoiceConfirmCommand.Row(
                "Notebook", new BigDecimal("200.00"), LocalDate.of(2025, 7, 15), LocalDate.of(2025, 7, 15),
                3, 4, categoryId, card.id());
        var parc2 = new InvoiceConfirmCommand.Row(
                "Notebook", new BigDecimal("200.00"), LocalDate.of(2025, 8, 15), LocalDate.of(2025, 7, 15),
                4, 4, categoryId, card.id());
        var cmd = new InvoiceConfirmCommand(List.of(avista, parc1, parc2));

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
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), null, null, 1, 1, null, null));

        var useCase = useCaseWith(NOOP_EXTRACTOR, accounts, transactions, List.of(card));

        var row = new InvoiceConfirmCommand.Row(
                "Mercado Livre", new BigDecimal("90.00"), LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 4),
                null, null, UUID.randomUUID(), card.id());
        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new InvoiceConfirmCommand(List.of(row)))).value();

        assertEquals(0, result.created());
        assertEquals(1, result.skipped());
        assertEquals(1, transactions.findAll().size());
    }

    @Test
    void confirmFailsWhenRowCardNotFound() {
        var useCase = useCaseWith(NOOP_EXTRACTOR);
        var row = new InvoiceConfirmCommand.Row(
                "Compra", new BigDecimal("10.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, UUID.randomUUID(), UUID.randomUUID());
        var cmd = new InvoiceConfirmCommand(List.of(row));
        var error = assertInstanceOf(Result.Failure.class, useCase.confirm(cmd)).error();
        assertInstanceOf(BusinessError.NotFound.class, error);
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

        var ok1 = new InvoiceConfirmCommand.Row(
                "Antes", new BigDecimal("10.00"), LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                null, null, categoryId, card.id());
        var boom = new InvoiceConfirmCommand.Row(
                "BOOM", new BigDecimal("20.00"), LocalDate.of(2025, 7, 2), LocalDate.of(2025, 7, 2),
                null, null, categoryId, card.id());
        var ok2 = new InvoiceConfirmCommand.Row(
                "Depois", new BigDecimal("30.00"), LocalDate.of(2025, 7, 3), LocalDate.of(2025, 7, 3),
                null, null, categoryId, card.id());

        var result = (ImportResult) assertInstanceOf(Result.Success.class,
                useCase.confirm(new InvoiceConfirmCommand(List.of(ok1, boom, ok2)))).value();

        assertEquals(2, result.created());
        var saved = transactions.findAll();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(t -> t.description().equals("Antes")));
        assertTrue(saved.stream().anyMatch(t -> t.description().equals("Depois")));
        assertFalse(saved.stream().anyMatch(t -> t.description().equals("BOOM")));
    }
}
