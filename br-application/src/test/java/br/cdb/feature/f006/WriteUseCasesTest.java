package br.cdb.feature.f006;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.TransactionCommand;
import br.cdb.feature.f006._1_application.usecase.TransactionScope;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
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

/** Cobre lançamentos: criação/parcelamento, edição/exclusão (default/FUTURE), transferências, cardId. */
class WriteUseCasesTest extends AbstractUseCaseTest {

    private WriteUseCases useCase;
    private ReadUseCases reads;

    private final UUID accountId = UUID.randomUUID();
    private final boolean planned = false;

    @BeforeEach
    void setUp() {
        accountRepository().save(new Account(accountId, "Banco", Account.Type.CHECKING, true));

        useCase = new WriteUseCases();
        reads = new ReadUseCases();
    }

    private TransactionCommand.Create cmd(LocalDate date, Status status, Integer installments) {
        return new TransactionCommand.Create("desc", new BigDecimal("10.00"), date, accountId,
                planned, status, Nature.EXPENSE, installments, null, null);
    }

    private TransactionCommand.Create cmdWithCard(LocalDate date, Integer installments, UUID cardId) {
        return new TransactionCommand.Create("desc", new BigDecimal("10.00"), date, accountId,
                planned, Status.CONFIRMED, Nature.EXPENSE, installments, null, cardId);
    }

    private CreditCard seedCard(UUID forAccountId, String last4) {
        return cardRepository().save(new CreditCard(UUID.randomUUID(), last4, forAccountId, true));
    }

    @Test
    @DisplayName("1 parcela: salva único lançamento sem groupId")
    void singleInstallmentCreatesOne() {
        Result<Transaction, BusinessError> r = useCase.upsert(
                cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        assertTrue(r.isSuccess());
        assertEquals(1, transactionRepository().findAll().size());
        Transaction t = transactionRepository().findAll().get(0);
        assertNull(t.groupId());
        assertEquals(1, t.installmentNumber());
        assertEquals(1, t.totalInstallments());
        assertEquals(Status.CONFIRMED, t.status());
    }

    @Test
    @DisplayName("N parcelas: N lançamentos, mesmo groupId, datas +i meses, status pending exceto 1ª")
    void nInstallmentsExpandsAndShiftsDates() {
        LocalDate base = LocalDate.of(2026, 5, 10);
        Result<Transaction, BusinessError> r = useCase.upsert(
                cmd(base, Status.CONFIRMED, 3));
        assertTrue(r.isSuccess());
        List<Transaction> all = transactionRepository().findAll();
        assertEquals(3, all.size());
        UUID groupId = all.get(0).groupId();
        assertNotNull(groupId);
        for (Transaction t : all) {
            assertEquals(groupId, t.groupId());
            assertEquals(3, t.totalInstallments());
            int n = t.installmentNumber();
            assertEquals(base.plusMonths(n - 1), t.date());
            assertEquals(n == 1 ? Status.CONFIRMED : Status.PENDING, t.status());
        }
    }

    @Test
    @DisplayName("update default só altera o selecionado")
    void updateDefaultModeOnlyOne() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, 3));
        List<Transaction> all = transactionRepository().findAll();
        Transaction first = all.stream().filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand.Update upd = new TransactionCommand.Update(first.id(), "upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), accountId, planned, Status.CONFIRMED, Nature.EXPENSE, new TransactionScope.Single(), null, null);
        Result<Transaction, BusinessError> r = useCase.upsert(upd);
        assertTrue(r.isSuccess());

        Transaction reload = transactionRepository().findById(first.id()).orElseThrow();
        assertEquals(new BigDecimal("20.00"), reload.amount());
        assertEquals(LocalDate.of(2026, 5, 15), reload.date());

        long unchanged = transactionRepository().findAll().stream()
                .filter(t -> !t.id().equals(first.id()))
                .filter(t -> "desc".equals(t.description()))
                .count();
        assertEquals(2, unchanged);
    }

    @Test
    @DisplayName("FUTURE: atualiza atual+futuras, preserva status, ajusta datas proporcionalmente")
    void updateFutureModeShiftsDatesAndPreservesStatus() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, 4));
        Transaction second = transactionRepository().findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();

        LocalDate newDate = LocalDate.of(2026, 7, 20);
        TransactionCommand.Update upd = new TransactionCommand.Update(second.id(), "future", new BigDecimal("99.00"),
                newDate, accountId, planned, Status.CONFIRMED, Nature.EXPENSE, new TransactionScope.Future(), null, null);
        Result<Transaction, BusinessError> r = useCase.upsert(upd);
        assertTrue(r.isSuccess());

        Transaction first = transactionRepository().findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        assertEquals("desc", first.description());
        assertEquals(new BigDecimal("10.00"), first.amount());

        for (int n = 2; n <= 4; n++) {
            int finalN = n;
            Transaction t = transactionRepository().findAll().stream()
                    .filter(x -> x.installmentNumber() == finalN).findFirst().orElseThrow();
            assertEquals("future", t.description());
            assertEquals(new BigDecimal("99.00"), t.amount());
            assertEquals(newDate.plusMonths(n - 2), t.date());
            assertEquals(Status.PENDING, t.status());
        }
    }

    @Test
    @DisplayName("updateStatus apenas muda status e paymentDate")
    void updateStatusBypassesClosing() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.PENDING, null));
        Transaction t = transactionRepository().findAll().get(0);
        Result<Transaction, BusinessError> r = useCase.updateTransactionStatus(
                t.id(), Status.CONFIRMED, LocalDate.of(2026, 5, 12));
        assertTrue(r.isSuccess());
        assertEquals(Status.CONFIRMED, transactionRepository().findById(t.id()).orElseThrow().status());
        assertEquals(LocalDate.of(2026, 5, 12), transactionRepository().findById(t.id()).orElseThrow().paymentDate());
    }

    @Test
    @DisplayName("delete default exclui só o lançamento")
    void deleteDefaultMode() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, 2));
        Transaction first = transactionRepository().findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        Result<List<UUID>, BusinessError> r = useCase.delete(new TransactionCommand.Delete(first.id(), new TransactionScope.Single()));
        assertTrue(r.isSuccess());
        assertEquals(1, transactionRepository().findAll().size());
    }

    @Test
    @DisplayName("delete FUTURE exclui atual+futuras do grupo")
    void deleteFutureMode() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, 4));
        Transaction second = transactionRepository().findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();
        Result<List<UUID>, BusinessError> r = useCase.delete(new TransactionCommand.Delete(second.id(), new TransactionScope.Future()));
        assertTrue(r.isSuccess());
        assertEquals(1, transactionRepository().findAll().size());
        assertEquals(1, transactionRepository().findAll().get(0).installmentNumber());
    }

    @Test
    @DisplayName("transactions ordena por data desc")
    void listOrdersByDateDesc() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        useCase.upsert(cmd(LocalDate.of(2026, 6, 10), Status.CONFIRMED, null));
        useCase.upsert(cmd(LocalDate.of(2026, 4, 10), Status.CONFIRMED, null));
        List<Transaction> list = ((Result.Success<List<Transaction>, BusinessError>)
                reads.transactions()).value();
        assertEquals(LocalDate.of(2026, 6, 10), list.get(0).date());
        assertEquals(LocalDate.of(2026, 4, 10), list.get(list.size() - 1).date());
    }

    @Test
    @DisplayName("pending retorna só PENDING")
    void listPendingReturnsOnlyPending() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        useCase.upsert(cmd(LocalDate.of(2026, 5, 11), Status.PENDING, null));
        List<Transaction> pending = ((Result.Success<List<Transaction>, BusinessError>)
                reads.pending()).value();
        assertEquals(1, pending.size());
        assertEquals(Status.PENDING, pending.get(0).status());
    }

    @Test
    @DisplayName("lançamento criado guarda o centro de custo do comando")
    void persistsCostCenter() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        assertEquals(planned, transactionRepository().findAll().get(0).planned());
    }

    // ── Transferências: par indissociável ─────────────────────

    /** Persiste as duas pernas de uma transferência (saída + entrada) sob um mesmo groupId. */
    private UUID saveTransferPair(LocalDate date, UUID fromAccount, UUID toAccount) {
        UUID groupId = UUID.randomUUID();
        transactionRepository().save(new Transaction(UUID.randomUUID(), "Transferência (saída)", new BigDecimal("-50.00"),
                date, fromAccount, Status.CONFIRMED, Nature.EXPENSE, planned, date, groupId, 1, 2, null, null));
        transactionRepository().save(new Transaction(UUID.randomUUID(), "Transferência (entrada)", new BigDecimal("50.00"),
                date, toAccount, Status.CONFIRMED, Nature.INCOME, planned, date, groupId, 2, 2, null, null));
        return groupId;
    }

    @Test
    @DisplayName("createTransfer cria par indissociável saída/entrada")
    void createTransferCreatesLinkedPair() {
        UUID toAccount = UUID.randomUUID();
        Result<Transaction, BusinessError> r = useCase.createTransfer(accountId, toAccount, LocalDate.of(2026, 5, 10), new BigDecimal("50.00"));
        assertTrue(r.isSuccess());

        List<Transaction> all = transactionRepository().findAll();
        assertEquals(2, all.size());
        UUID groupId = all.get(0).groupId();
        assertNotNull(groupId);
        assertTrue(all.stream().allMatch(t -> groupId.equals(t.groupId())));
        assertTrue(all.stream().anyMatch(t -> t.signal() < 0 && accountId.equals(t.accountId())));
        assertTrue(all.stream().anyMatch(t -> t.signal() > 0 && toAccount.equals(t.accountId())));
    }

    @Test
    @DisplayName("createTransfer com mesma conta origem/destino é rejeitado")
    void createTransferRejectsSameAccount() {
        Result<Transaction, BusinessError> r = useCase.createTransfer(accountId, accountId, LocalDate.of(2026, 5, 10), new BigDecimal("50.00"));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Transaction, BusinessError>) r).error());
        assertTrue(transactionRepository().findAll().isEmpty());
    }

    @Test
    @DisplayName("excluir uma perna da transferência remove ambas")
    void deleteTransferLegRemovesBothLegs() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction entrada = transactionRepository().findAll().stream()
                .filter(t -> t.signal() > 0).findFirst().orElseThrow();

        Result<List<UUID>, BusinessError> r = useCase.delete(new TransactionCommand.Delete(entrada.id(), new TransactionScope.Single()));
        assertTrue(r.isSuccess());
        assertEquals(0, transactionRepository().findAll().size(), "as duas pernas removidas");
    }

    @Test
    @DisplayName("excluir perna de transferência ignora o mode (FUTURE remove ambas)")
    void deleteTransferLegIgnoresMode() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction saida = transactionRepository().findAll().stream()
                .filter(t -> t.signal() < 0).findFirst().orElseThrow();

        assertTrue(useCase.delete(new TransactionCommand.Delete(saida.id(), new TransactionScope.Future())).isSuccess());
        assertEquals(0, transactionRepository().findAll().size());
    }

    @Test
    @DisplayName("editar uma perna mantém o par: data/valor em ambas, conta só na editada")
    void updateTransferLegKeepsPairAndMirrors() {
        UUID toAccount = UUID.randomUUID();
        UUID groupId = saveTransferPair(LocalDate.of(2026, 5, 10), accountId, toAccount);
        Transaction entrada = transactionRepository().findAll().stream()
                .filter(t -> t.signal() > 0).findFirst().orElseThrow();
        Transaction saida = transactionRepository().findAll().stream()
                .filter(t -> t.signal() < 0).findFirst().orElseThrow();

        UUID newAccount = UUID.randomUUID();
        TransactionCommand.Update upd = new TransactionCommand.Update(entrada.id(), "ajuste", new BigDecimal("70.00"),
                LocalDate.of(2026, 5, 12), newAccount, planned,
                Status.CONFIRMED, Nature.INCOME, new TransactionScope.Single(), null, null);
        Result<Transaction, BusinessError> r = useCase.upsert(upd);
        assertTrue(r.isSuccess());

        assertEquals(2, transactionRepository().findAll().size(), "par preservado");
        Transaction newEntrada = transactionRepository().findById(entrada.id()).orElseThrow();
        Transaction newSaida = transactionRepository().findById(saida.id()).orElseThrow();

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
        Transaction saida = transactionRepository().findAll().stream()
                .filter(t -> t.signal() < 0).findFirst().orElseThrow();

        TransactionCommand.Update upd = new TransactionCommand.Update(saida.id(), "x", new BigDecimal("50.00"),
                LocalDate.of(2026, 5, 10), toAccount, planned,
                Status.CONFIRMED, Nature.EXPENSE, new TransactionScope.Single(), null, null);
        assertTrue(useCase.upsert(upd).isFailure());
        assertEquals(2, transactionRepository().findAll().size(), "nada alterado");
        assertEquals(accountId, transactionRepository().findById(saida.id()).orElseThrow().accountId(), "conta intacta");
    }

    @Test
    @DisplayName("grupo de parcelas (tipo único) não é tratado como transferência ao editar")
    void updateInstallmentGroupNotTreatedAsTransfer() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, 3));
        Transaction first = transactionRepository().findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand.Update upd = new TransactionCommand.Update(first.id(), "upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), accountId, planned, Status.CONFIRMED, Nature.EXPENSE, new TransactionScope.Single(), null, null);
        assertTrue(useCase.upsert(upd).isSuccess());

        assertEquals(3, transactionRepository().findAll().size(), "parcelas preservadas");
        Transaction reload = transactionRepository().findById(first.id()).orElseThrow();
        assertNotNull(reload.groupId(), "ainda parte do grupo de parcelas");
    }

    // ── cardId: validação de posse ──────────────────────────────

    @Test
    @DisplayName("createTransaction com cardId da própria conta é aceito e persistido")
    void createWithOwnCardIsAccepted() {
        CreditCard creditCard = seedCard(accountId, "1234");
        Result<Transaction, BusinessError> r = useCase.upsert(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        assertTrue(r.isSuccess());
        assertEquals(creditCard.id(), transactionRepository().findAll().get(0).cardId());
    }

    @Test
    @DisplayName("createTransaction com cardId de outra conta é rejeitado")
    void createWithCardFromAnotherAccountIsRejected() {
        CreditCard creditCard = seedCard(UUID.randomUUID(), "1234");
        Result<Transaction, BusinessError> r = useCase.upsert(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Transaction, BusinessError>) r).error());
        assertTrue(transactionRepository().findAll().isEmpty(), "nada deve ser persistido");
    }

    @Test
    @DisplayName("createTransaction com cardId inexistente é rejeitado")
    void createWithUnknownCardIsRejected() {
        Result<Transaction, BusinessError> r = useCase.upsert(cmdWithCard(LocalDate.of(2026, 5, 10), null, UUID.randomUUID()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Transaction, BusinessError>) r).error());
    }

    @Test
    @DisplayName("parcelamento propaga o mesmo cardId para todas as parcelas")
    void installmentsPropagateCardId() {
        CreditCard creditCard = seedCard(accountId, "1234");
        useCase.upsert(cmdWithCard(LocalDate.of(2026, 5, 10), 3, creditCard.id()));
        List<Transaction> all = transactionRepository().findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(t -> creditCard.id().equals(t.cardId())));
    }

    @Test
    @DisplayName("transferência nunca carrega cardId, mesmo se a conta tiver cartões")
    void transferNeverCarriesCard() {
        seedCard(accountId, "1234");
        Result<Transaction, BusinessError> r = useCase.createTransfer(accountId, UUID.randomUUID(), LocalDate.of(2026, 5, 10), new BigDecimal("50.00"));
        assertTrue(r.isSuccess());
        assertTrue(transactionRepository().findAll().stream().allMatch(t -> t.cardId() == null));
    }

    @Test
    @DisplayName("updateTransactionStatus preserva o cardId existente")
    void updateStatusPreservesCardId() {
        CreditCard creditCard = seedCard(accountId, "1234");
        useCase.upsert(cmdWithCard(LocalDate.of(2026, 5, 10), null, creditCard.id()));
        Transaction t = transactionRepository().findAll().get(0);

        Result<Transaction, BusinessError> r = useCase.updateTransactionStatus(t.id(), Status.CONFIRMED, LocalDate.of(2026, 5, 12));
        assertTrue(r.isSuccess());
        assertEquals(creditCard.id(), transactionRepository().findById(t.id()).orElseThrow().cardId());
    }

    // ── create (CRUD) ─────

    @Test
    @DisplayName("create persiste a transação exatamente como recebida")
    void createPersistsTransactionAsReceived() {
        val tx = new Transaction(
                UUID.randomUUID(), "compra importada", new BigDecimal("42.00"), LocalDate.of(2026, 5, 10),
                accountId, Status.CONFIRMED, Nature.EXPENSE, planned, null, null,
                1, 1, null, null);

        Result<Transaction, BusinessError> r = useCase.create(tx);

        assertTrue(r.isSuccess());
        Transaction saved = transactionRepository().findAll().get(0);
        assertEquals(1, saved.installmentNumber());
        assertEquals(1, saved.totalInstallments());
        assertNull(saved.groupId());
        assertEquals(0, new BigDecimal("42.00").compareTo(saved.amount()));
    }

    @Test
    @DisplayName("create com cardId de outra conta é rejeitado")
    void createRejectsCardFromAnotherAccount() {
        CreditCard creditCard = seedCard(UUID.randomUUID(), "1234");
        val tx = new Transaction(
                UUID.randomUUID(), "compra importada", new BigDecimal("42.00"), LocalDate.of(2026, 5, 10),
                accountId, Status.CONFIRMED, Nature.EXPENSE, planned, null, null,
                1, 1, null, creditCard.id());

        Result<Transaction, BusinessError> r = useCase.create(tx);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Transaction, BusinessError>) r).error());
        assertTrue(transactionRepository().findAll().isEmpty());
    }

    // ── deleteTransactions (exclusão em massa: categoria/tag DELETE) ─────

    @Test
    @DisplayName("deleteTransactions: dedupe (mesmo id repetido) apaga uma vez")
    void deleteTransactionsDedupesRepeatedIds() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        Transaction t = transactionRepository().findAll().get(0);

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id(), t.id()));

        assertTrue(r.isSuccess());
        assertTrue(transactionRepository().findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteTransactions: id inexistente é ignorado (idempotente)")
    void deleteTransactionsSkipsMissingIds() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        Transaction t = transactionRepository().findAll().get(0);

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id(), UUID.randomUUID()));

        assertTrue(r.isSuccess());
        assertTrue(transactionRepository().findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteTransactions: passar só uma perna expande para a irmã de transferência")
    void deleteTransactionsExpandsTransferSiblings() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        Transaction saida = transactionRepository().findAll().stream()
                .filter(t -> t.signal() < 0).findFirst().orElseThrow();

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(saida.id()));

        assertTrue(r.isSuccess());
        assertTrue(transactionRepository().findAll().isEmpty(), "as duas pernas removidas");
    }

    @Test
    @DisplayName("deleteTransactions: recalcula saldo da conta (sem atividade → snapshot antigo cai)")
    void deleteTransactionsRecalculatesBalance() {
        useCase.upsert(cmd(LocalDate.of(2026, 5, 10), Status.CONFIRMED, null));
        Transaction t = transactionRepository().findAll().get(0);
        balanceRepository().save(new Balance(accountRepository().findById(accountId).orElseThrow(), YearMonth.of(2026, 5), new BigDecimal("999.00")));

        Result<Void, BusinessError> r = useCase.deleteTransactions(List.of(t.id()));

        assertTrue(r.isSuccess());
        assertTrue(balanceRepository().findByAccount(accountId).isEmpty(), "sem atividade → snapshots removidos");
    }
}
