package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.*;
import br.cdb.context.monetary._1_application.command.TransactionCommand;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cobre §4 (lançamentos + parcelamento + edição/exclusão). */

class TransactionResponseUseCaseTest {

    private InMemoryRepositories.Transactions txRepo;
    private InMemoryRepositories.Cards cardRepo;
    private InMemoryRepositories.Accounts accountRepo;
    private InMemoryRepositories.Balances balanceRepo;
    private TransactionUseCase useCase;

    private final UUID accountId = UUID.randomUUID();
    private final UUID costCenterId = CostCenter.VARIAVEL.id();

    @BeforeEach
    void setUp() {
        txRepo = new InMemoryRepositories.Transactions();
        cardRepo = new InMemoryRepositories.Cards();
        accountRepo = new InMemoryRepositories.Accounts();
        balanceRepo = new InMemoryRepositories.Balances();
        accountRepo.save(new Account(accountId, "Banco", Account.Type.CHECKING, true));

        val transactionService = new TransactionService(txRepo);
        val accountService = new AccountService(accountRepo);
        val balanceRecalculationService = new BalanceRecalculationService(accountService, new BalanceService(balanceRepo), transactionService);
        useCase = new TransactionUseCase(transactionService, new CardService(cardRepo), balanceRecalculationService);
    }

    private TransactionCommand cmd(LocalDate date, Transaction.Status status, Integer installments) {
        return new TransactionCommand("desc", new BigDecimal("10.00"), date, accountId,
                costCenterId, status, Transaction.Type.EXPENSE, installments, null, null, null);
    }

    private TransactionCommand cmdWithCard(LocalDate date, Integer installments, UUID cardId) {
        return new TransactionCommand("desc", new BigDecimal("10.00"), date, accountId,
                costCenterId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, installments, null, null, cardId);
    }

    private CreditCard seedCard(UUID forAccountId, String last4) {
        return cardRepo.save(new CreditCard(UUID.randomUUID(), last4, forAccountId, true));
    }

    @Test
    @DisplayName("§4.2 1 parcela: salva único lançamento sem groupId")
    void singleInstallmentCreatesOne() {
        Result<Transaction, BusinessError> r = useCase.createTransaction(
                cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
        Transaction t = txRepo.findAll().get(0);
        assertNull(t.groupId());
        assertEquals(1, t.installmentNumber());
        assertEquals(1, t.totalInstallments());
        assertEquals(Transaction.Status.CONFIRMED, t.status());
    }

    @Test
    @DisplayName("§4.2 N parcelas: N lançamentos, mesmo groupId, datas +i meses, status pending exceto 1ª")
    void nInstallmentsExpandsAndShiftsDates() {
        LocalDate base = LocalDate.of(2026, 5, 10);
        Result<Transaction, BusinessError> r = useCase.createTransaction(
                cmd(base, Transaction.Status.CONFIRMED, 3));
        assertTrue(r.isSuccess());
        List<Transaction> all = txRepo.findAll();
        assertEquals(3, all.size());
        UUID groupId = all.get(0).groupId();
        assertNotNull(groupId);
        for (Transaction t : all) {
            assertEquals(groupId, t.groupId());
            assertEquals(3, t.totalInstallments());
            int n = t.installmentNumber();
            assertEquals(base.plusMonths(n - 1), t.date());
            assertEquals(n == 1 ? Transaction.Status.CONFIRMED : Transaction.Status.PENDING, t.status());
        }
    }

    @Test
    @DisplayName("§4.3 update default só altera o selecionado")
    void updateDefaultModeOnlyOne() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, 3));
        List<Transaction> all = txRepo.findAll();
        Transaction first = all.stream().filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), accountId, costCenterId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, null, null, null, null);
        Result<Transaction, BusinessError> r = useCase.updateTransaction(first.id(), upd);
        assertTrue(r.isSuccess());

        Transaction reload = txRepo.findById(first.id()).orElseThrow();
        assertEquals(new BigDecimal("20.00"), reload.amount());
        assertEquals(LocalDate.of(2026, 5, 15), reload.date());

        long unchanged = txRepo.findAll().stream()
                .filter(t -> !t.id().equals(first.id()))
                .filter(t -> "desc".equals(t.description()))
                .count();
        assertEquals(2, unchanged);
    }

    @Test
    @DisplayName("§4.3 FUTURE: atualiza atual+futuras, preserva status, ajusta datas proporcionalmente")
    void updateFutureModeShiftsDatesAndPreservesStatus() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, 4));
        Transaction second = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();

        LocalDate newDate = LocalDate.of(2026, 7, 20);
        TransactionCommand upd = new TransactionCommand("future", new BigDecimal("99.00"),
                newDate, accountId, costCenterId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, null, "FUTURE", null, null);
        Result<Transaction, BusinessError> r = useCase.updateTransaction(second.id(), upd);
        assertTrue(r.isSuccess());

        Transaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        assertEquals("desc", first.description());
        assertEquals(new BigDecimal("10.00"), first.amount());

        for (int n = 2; n <= 4; n++) {
            int finalN = n;
            Transaction t = txRepo.findAll().stream()
                    .filter(x -> x.installmentNumber() == finalN).findFirst().orElseThrow();
            assertEquals("future", t.description());
            assertEquals(new BigDecimal("99.00"), t.amount());
            assertEquals(newDate.plusMonths(n - 2), t.date());
            assertEquals(Transaction.Status.PENDING, t.status());
        }
    }

    @Test
    @DisplayName("§4.5 updateStatus apenas muda status e paymentDate")
    void updateStatusBypassesClosing() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.PENDING, null));
        Transaction t = txRepo.findAll().get(0);
        Result<Transaction, BusinessError> r = useCase.updateTransactionStatus(
                t.id(), Transaction.Status.CONFIRMED, LocalDate.of(2026, 5, 12));
        assertTrue(r.isSuccess());
        assertEquals(Transaction.Status.CONFIRMED, txRepo.findById(t.id()).orElseThrow().status());
        assertEquals(LocalDate.of(2026, 5, 12), txRepo.findById(t.id()).orElseThrow().paymentDate());
    }

    @Test
    @DisplayName("§4.4 delete default exclui só o lançamento, valida fechamento")
    void deleteDefaultMode() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, 2));
        Transaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        Result<List<UUID>, BusinessError> r = useCase.deleteTransaction(first.id(), null);
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
    }

    @Test
    @DisplayName("§4.4 delete FUTURE exclui atual+futuras do grupo")
    void deleteFutureMode() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, 4));
        Transaction second = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();
        Result<List<UUID>, BusinessError> r = useCase.deleteTransaction(second.id(), "FUTURE");
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
        assertEquals(1, txRepo.findAll().get(0).installmentNumber());
    }

    @Test
    @DisplayName("listTransactions ordena por data desc")
    void listOrdersByDateDesc() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        useCase.createTransaction(cmd(LocalDate.of(2026, 6, 10), Transaction.Status.CONFIRMED, null));
        useCase.createTransaction(cmd(LocalDate.of(2026, 4, 10), Transaction.Status.CONFIRMED, null));
        List<Transaction> list = ((Result.Success<List<Transaction>, BusinessError>)
                useCase.listTransactions()).value();
        assertEquals(LocalDate.of(2026, 6, 10), list.get(0).date());
        assertEquals(LocalDate.of(2026, 4, 10), list.get(list.size() - 1).date());
    }

    @Test
    @DisplayName("lançamento criado guarda o centro de custo do comando")
    void persistsCostCenter() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        assertEquals(costCenterId, txRepo.findAll().get(0).costCenterId());
    }

    // ── Transferências: par indissociável ─────────────────────

    /** Persiste as duas pernas de uma transferência (saída + entrada) sob um mesmo groupId. */
    private UUID saveTransferPair(LocalDate date, UUID fromAccount, UUID toAccount) {
        UUID groupId = UUID.randomUUID();
        txRepo.save(new Transaction(UUID.randomUUID(), "Transferência (saída)", new BigDecimal("-50.00"),
                date, fromAccount, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, costCenterId, date, groupId, 1, 2, null, null));
        txRepo.save(new Transaction(UUID.randomUUID(), "Transferência (entrada)", new BigDecimal("50.00"),
                date, toAccount, Transaction.Status.CONFIRMED, Transaction.Type.INCOME, costCenterId, date, groupId, 2, 2, null, null));
        return groupId;
    }

    @Test
    @DisplayName("excluir uma perna da transferência remove ambas")
    void deleteTransferLegRemovesBothLegs() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction entrada = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.INCOME.equals(t.type())).findFirst().orElseThrow();

        Result<List<UUID>, BusinessError> r = useCase.deleteTransaction(entrada.id(), null);
        assertTrue(r.isSuccess());
        assertEquals(0, txRepo.findAll().size(), "as duas pernas removidas");
    }

    @Test
    @DisplayName("excluir perna de transferência ignora o mode (FUTURE remove ambas)")
    void deleteTransferLegIgnoresMode() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction saida = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.EXPENSE.equals(t.type())).findFirst().orElseThrow();

        assertTrue(useCase.deleteTransaction(saida.id(), "FUTURE").isSuccess());
        assertEquals(0, txRepo.findAll().size());
    }

    @Test
    @DisplayName("editar uma perna mantém o par: data/valor em ambas, conta só na editada")
    void updateTransferLegKeepsPairAndMirrors() {
        UUID toAccount = UUID.randomUUID();
        UUID groupId = saveTransferPair(LocalDate.of(2026, 5, 10), accountId, toAccount);
        Transaction entrada = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.INCOME.equals(t.type())).findFirst().orElseThrow();
        Transaction saida = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.EXPENSE.equals(t.type())).findFirst().orElseThrow();

        UUID newAccount = UUID.randomUUID();
        TransactionCommand upd = new TransactionCommand("ajuste", new BigDecimal("70.00"),
                LocalDate.of(2026, 5, 12), newAccount, costCenterId,
                Transaction.Status.CONFIRMED, Transaction.Type.INCOME, null, null, null, null);
        Result<Transaction, BusinessError> r = useCase.updateTransaction(entrada.id(), upd);
        assertTrue(r.isSuccess());

        assertEquals(2, txRepo.findAll().size(), "par preservado");
        Transaction newEntrada = txRepo.findById(entrada.id()).orElseThrow();
        Transaction newSaida = txRepo.findById(saida.id()).orElseThrow();

        assertEquals(groupId, newEntrada.groupId());
        assertEquals(groupId, newSaida.groupId());

        assertEquals(LocalDate.of(2026, 5, 12), newEntrada.date());
        assertEquals(LocalDate.of(2026, 5, 12), newSaida.date());
        assertEquals(0, new BigDecimal("70.00").compareTo(newEntrada.amount()));
        assertEquals(0, new BigDecimal("70.00").compareTo(newSaida.amount()), "saída mantém valor absoluto");
        assertEquals(-1, newSaida.signal());

        assertEquals(newAccount, newEntrada.accountId());
        assertEquals(accountId, newSaida.accountId(), "perna oposta mantém a conta");
    }

    @Test
    @DisplayName("mover a perna editada para a conta da oposta é rejeitado (origem == destino)")
    void updateTransferRejectsCollapsingToSameAccount() {
        UUID toAccount = UUID.randomUUID();
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, toAccount);
        Transaction saida = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.EXPENSE.equals(t.type())).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("x", new BigDecimal("50.00"),
                LocalDate.of(2026, 5, 10), toAccount, costCenterId,
                Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, null, null, null, null);
        assertTrue(useCase.updateTransaction(saida.id(), upd).isFailure());
        assertEquals(2, txRepo.findAll().size(), "nada alterado");
        assertEquals(accountId, txRepo.findById(saida.id()).orElseThrow().accountId(), "conta intacta");
    }

    @Test
    @DisplayName("grupo de parcelas (tipo único) não é tratado como transferência ao editar")
    void updateInstallmentGroupNotTreatedAsTransfer() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, 3));
        Transaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), accountId, costCenterId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, null, null, null, null);
        assertTrue(useCase.updateTransaction(first.id(), upd).isSuccess());

        assertEquals(3, txRepo.findAll().size(), "parcelas preservadas");
        Transaction reload = txRepo.findById(first.id()).orElseThrow();
        assertNotNull(reload.groupId(), "ainda parte do grupo de parcelas");
    }

    // ── cardId: validação de posse ──────────────────────────────

    @Test
    @DisplayName("createTransaction com cardId da própria conta é aceito e persistido")
    void createWithOwnCardIsAccepted() {
        CreditCard creditCard = seedCard(accountId, "1234");
        Result<Transaction, BusinessError> r = useCase.createTransaction(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        assertTrue(r.isSuccess());
        assertEquals(creditCard.id(), txRepo.findAll().get(0).cardId());
    }

    @Test
    @DisplayName("createTransaction com cardId de outra conta é rejeitado")
    void createWithCardFromAnotherAccountIsRejected() {
        CreditCard creditCard = seedCard(UUID.randomUUID(), "1234");
        Result<Transaction, BusinessError> r = useCase.createTransaction(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Transaction, BusinessError>) r).error());
        assertTrue(txRepo.findAll().isEmpty(), "nada deve ser persistido");
    }

    @Test
    @DisplayName("createTransaction com cardId inexistente é rejeitado")
    void createWithUnknownCardIsRejected() {
        Result<Transaction, BusinessError> r = useCase.createTransaction(cmdWithCard(LocalDate.of(2026, 5, 10), null, UUID.randomUUID()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Transaction, BusinessError>) r).error());
    }

    @Test
    @DisplayName("parcelamento propaga o mesmo cardId para todas as parcelas")
    void installmentsPropagateCardId() {
        CreditCard creditCard = seedCard(accountId, "1234");
        useCase.createTransaction(cmdWithCard(LocalDate.of(2026, 5, 10), 3, creditCard.id()));
        List<Transaction> all = txRepo.findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(t -> creditCard.id().equals(t.cardId())));
    }

    @Test
    @DisplayName("transferência nunca carrega cardId, mesmo se a conta tiver cartões")
    void transferNeverCarriesCard() {
        seedCard(accountId, "1234");
        Result<Transaction, BusinessError> r = useCase.createTransfer(accountId, UUID.randomUUID(), LocalDate.of(2026, 5, 10), new BigDecimal("50.00"));
        assertTrue(r.isSuccess());
        assertTrue(txRepo.findAll().stream().allMatch(t -> t.cardId() == null));
    }

    @Test
    @DisplayName("updateTransactionStatus preserva o cardId existente")
    void updateStatusPreservesCardId() {
        CreditCard creditCard = seedCard(accountId, "1234");
        useCase.createTransaction(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        Transaction t = txRepo.findAll().get(0);

        Result<Transaction, BusinessError> r = useCase.updateTransactionStatus(t.id(), Transaction.Status.CONFIRMED, LocalDate.of(2026, 5, 12));
        assertTrue(r.isSuccess());
        assertEquals(creditCard.id(), txRepo.findById(t.id()).orElseThrow().cardId());
    }

    // ── deleteTransactions (exclusão em massa: categoria/tag DELETE) ─────

    @Test
    @DisplayName("deleteTransactions: dedupe (mesmo id repetido) apaga uma vez")
    void deleteTransactionsDedupesRepeatedIds() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        Transaction t = txRepo.findAll().get(0);

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id(), t.id()));

        assertTrue(r.isSuccess());
        assertTrue(txRepo.findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteTransactions: id inexistente é ignorado (idempotente)")
    void deleteTransactionsSkipsMissingIds() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        Transaction t = txRepo.findAll().get(0);

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id(), UUID.randomUUID()));

        assertTrue(r.isSuccess());
        assertTrue(txRepo.findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteTransactions: passar só uma perna expande para a irmã de transferência")
    void deleteTransactionsExpandsTransferSiblings() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction saida = txRepo.findAll().stream()
                .filter(t -> Transaction.Type.EXPENSE.equals(t.type())).findFirst().orElseThrow();

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(saida.id()));

        assertTrue(r.isSuccess());
        assertTrue(txRepo.findAll().isEmpty(), "as duas pernas removidas");
    }

    @Test
    @DisplayName("deleteTransactions: recalcula saldo da conta (sem atividade → snapshot antigo cai)")
    void deleteTransactionsRecalculatesBalance() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), Transaction.Status.CONFIRMED, null));
        Transaction t = txRepo.findAll().get(0);
        balanceRepo.save(new MonthlyBalance(UUID.randomUUID(), accountId, YearMonth.of(2026, 5), new BigDecimal("999.00")));

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id()));

        assertTrue(r.isSuccess());
        assertTrue(balanceRepo.findByAccount(accountId).isEmpty(), "sem atividade → snapshots removidos");
    }
}
