package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.model.MonetaryNature;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.monetary._1_application.service.CategoryService;
import br.community.context.monetary._1_application.service.ClosingService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cobre §4 (lançamentos + parcelamento + edição/exclusão) e §5 (período fechado). */

class TransactionUseCaseTest {

    private InMemoryRepositories.Transactions txRepo;
    private InMemoryRepositories.Closings closingRepo;
    private TransactionUseCase useCase;

    private final UUID accountId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID costCenterId = MonetaryCenter.VARIAVEL_ID;

    @BeforeEach
    void setUp() {
        txRepo = new InMemoryRepositories.Transactions();
        closingRepo = new InMemoryRepositories.Closings();
        InMemoryRepositories.Categories catRepo = new InMemoryRepositories.Categories();
        catRepo.save(new MonetaryCategory(categoryId, MonetaryNature.EXPENSE, "Subcategoria", UUID.randomUUID()));
        useCase = new TransactionUseCase(new TransactionService(txRepo), new ClosingService(closingRepo), new CategoryService(catRepo));
    }

    private TransactionCommand cmd(LocalDate date, String status, Integer installments) {
        return new TransactionCommand("desc", new BigDecimal("10.00"), date, categoryId, accountId,
                costCenterId, status, "expense", installments, null);
    }

    @Test
    @DisplayName("§4.2 1 parcela: salva único lançamento sem groupId")
    void singleInstallmentCreatesOne() {
        Result<MonetaryTransaction, DomainError> r = useCase.createTransaction(
                cmd(LocalDate.of(2026, 5, 10), "confirmed", null));
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
        MonetaryTransaction t = txRepo.findAll().get(0);
        assertNull(t.groupId());
        assertNull(t.installmentNumber());
        assertEquals("confirmed", t.status());
    }

    @Test
    @DisplayName("§4.2 N parcelas: N lançamentos, mesmo groupId, datas +i meses, status pending exceto 1ª")
    void nInstallmentsExpandsAndShiftsDates() {
        LocalDate base = LocalDate.of(2026, 5, 10);
        Result<MonetaryTransaction, DomainError> r = useCase.createTransaction(
                cmd(base, "confirmed", 3));
        assertTrue(r.isSuccess());
        List<MonetaryTransaction> all = txRepo.findAll();
        assertEquals(3, all.size());
        UUID groupId = all.get(0).groupId();
        assertNotNull(groupId);
        for (MonetaryTransaction t : all) {
            assertEquals(groupId, t.groupId());
            assertEquals(3, t.totalInstallments());
            int n = t.installmentNumber();
            assertEquals(base.plusMonths(n - 1), t.date());
            assertEquals(n == 1 ? "confirmed" : "pending", t.status());
        }
    }

    @Test
    @DisplayName("§4.2 / §5 se uma parcela cai em período fechado nenhuma é salva")
    void anyInstallmentInClosedPeriodAborts() {
        closingRepo.save(YearMonth.of(2026, 5));
        Result<MonetaryTransaction, DomainError> r = useCase.createTransaction(
                cmd(LocalDate.of(2026, 5, 10), "confirmed", 3));
        assertTrue(r.isFailure());
        assertEquals(0, txRepo.findAll().size(), "nenhuma parcela persistida");
    }

    @Test
    @DisplayName("§5 cria em período fechado falha")
    void createInClosedPeriodFails() {
        closingRepo.save(YearMonth.of(2026, 5));
        Result<MonetaryTransaction, DomainError> r = useCase.createTransaction(
                cmd(LocalDate.of(2026, 5, 1), "confirmed", null));
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§5 mês > fechamento é permitido")
    void createAfterClosingPeriodOk() {
        closingRepo.save(YearMonth.of(2026, 5));
        Result<MonetaryTransaction, DomainError> r = useCase.createTransaction(
                cmd(LocalDate.of(2026, 6, 1), "confirmed", null));
        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("§4.3 update default só altera o selecionado")
    void updateDefaultModeOnlyOne() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 3));
        List<MonetaryTransaction> all = txRepo.findAll();
        MonetaryTransaction first = all.stream().filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), categoryId, accountId, costCenterId, "confirmed", "expense", null, null);
        Result<MonetaryTransaction, DomainError> r = useCase.updateTransaction(first.id(), upd);
        assertTrue(r.isSuccess());

        MonetaryTransaction reload = txRepo.findById(first.id()).orElseThrow();
        assertEquals(new BigDecimal("20.00"), reload.amount());
        assertEquals(LocalDate.of(2026, 5, 15), reload.date());

        // outras parcelas inalteradas
        long unchanged = txRepo.findAll().stream()
                .filter(t -> !t.id().equals(first.id()))
                .filter(t -> "desc".equals(t.description()))
                .count();
        assertEquals(2, unchanged);
    }

    @Test
    @DisplayName("§4.3 update default valida fechamento da data original e nova")
    void updateDefaultValidatesBothDates() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", null));
        MonetaryTransaction t = txRepo.findAll().get(0);

        closingRepo.save(YearMonth.of(2026, 5));
        TransactionCommand upd = new TransactionCommand("x", new BigDecimal("1.00"),
                LocalDate.of(2026, 6, 1), categoryId, accountId, costCenterId, "confirmed", "expense", null, null);
        assertTrue(useCase.updateTransaction(t.id(), upd).isFailure(), "data original em fechamento");

        closingRepo.clear();
        closingRepo.save(YearMonth.of(2026, 7));
        TransactionCommand upd2 = new TransactionCommand("x", new BigDecimal("1.00"),
                LocalDate.of(2026, 7, 1), categoryId, accountId, costCenterId, "confirmed", "expense", null, null);
        assertTrue(useCase.updateTransaction(t.id(), upd2).isFailure(), "nova data em fechamento");
    }

    @Test
    @DisplayName("§4.3 FUTURE: atualiza atual+futuras, preserva status, ajusta datas proporcionalmente")
    void updateFutureModeShiftsDatesAndPreservesStatus() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 4));
        MonetaryTransaction second = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();

        LocalDate newDate = LocalDate.of(2026, 7, 20);
        TransactionCommand upd = new TransactionCommand("future", new BigDecimal("99.00"),
                newDate, categoryId, accountId, costCenterId, "confirmed", "expense", null, "FUTURE");
        Result<MonetaryTransaction, DomainError> r = useCase.updateTransaction(second.id(), upd);
        assertTrue(r.isSuccess());

        // installments 1 inalterado
        MonetaryTransaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        assertEquals("desc", first.description());
        assertEquals(new BigDecimal("10.00"), first.amount());

        // installments 2,3,4 atualizados, com datas shift e status original preservado (pending)
        for (int n = 2; n <= 4; n++) {
            int finalN = n;
            MonetaryTransaction t = txRepo.findAll().stream()
                    .filter(x -> x.installmentNumber() == finalN).findFirst().orElseThrow();
            assertEquals("future", t.description());
            assertEquals(new BigDecimal("99.00"), t.amount());
            assertEquals(newDate.plusMonths(n - 2), t.date());
            assertEquals("pending", t.status());
        }
    }

    @Test
    @DisplayName("§4.3 FUTURE falha se alguma nova data cai em fechamento")
    void updateFutureFailsOnClosedPeriod() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 3));
        MonetaryTransaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        closingRepo.save(YearMonth.of(2026, 8));
        TransactionCommand upd = new TransactionCommand("desc", new BigDecimal("10.00"),
                LocalDate.of(2026, 9, 10), categoryId, accountId, costCenterId, "confirmed", "expense", null, "FUTURE");
        // existing.date é 2026-05-10 (em fechamento) → falha de imediato
        assertTrue(useCase.updateTransaction(first.id(), upd).isFailure());
    }

    @Test
    @DisplayName("§4.5 updateStatus ignora período fechado")
    void updateStatusBypassesClosing() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "pending", null));
        MonetaryTransaction t = txRepo.findAll().get(0);
        closingRepo.save(YearMonth.of(2026, 12));
        Result<MonetaryTransaction, DomainError> r = useCase.updateTransactionStatus(
                t.id(), "confirmed", LocalDate.of(2026, 5, 12));
        assertTrue(r.isSuccess());
        assertEquals("confirmed", txRepo.findById(t.id()).orElseThrow().status());
        assertEquals(LocalDate.of(2026, 5, 12), txRepo.findById(t.id()).orElseThrow().paymentDate());
    }

    @Test
    @DisplayName("§4.4 delete default exclui só o lançamento, valida fechamento")
    void deleteDefaultMode() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 2));
        MonetaryTransaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        Result<Void, DomainError> r = useCase.deleteTransaction(first.id(), null);
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
    }

    @Test
    @DisplayName("§4.4 delete default falha em período fechado")
    void deleteDefaultClosed() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", null));
        MonetaryTransaction t = txRepo.findAll().get(0);
        closingRepo.save(YearMonth.of(2026, 5));
        assertTrue(useCase.deleteTransaction(t.id(), null).isFailure());
        assertEquals(1, txRepo.findAll().size());
    }

    @Test
    @DisplayName("§4.4 delete FUTURE exclui atual+futuras do grupo")
    void deleteFutureMode() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 4));
        MonetaryTransaction second = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 2).findFirst().orElseThrow();
        Result<Void, DomainError> r = useCase.deleteTransaction(second.id(), "FUTURE");
        assertTrue(r.isSuccess());
        assertEquals(1, txRepo.findAll().size());
        assertEquals(1, txRepo.findAll().get(0).installmentNumber());
    }

    @Test
    @DisplayName("§4.4 delete FUTURE falha se alguma futura está em fechamento")
    void deleteFutureFailsOnClosed() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 3));
        MonetaryTransaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();
        closingRepo.save(YearMonth.of(2026, 5));
        assertTrue(useCase.deleteTransaction(first.id(), "FUTURE").isFailure());
        assertEquals(3, txRepo.findAll().size(), "nada removido");
    }

    @Test
    @DisplayName("listTransactions ordena por data desc")
    void listOrdersByDateDesc() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", null));
        useCase.createTransaction(cmd(LocalDate.of(2026, 6, 10), "confirmed", null));
        useCase.createTransaction(cmd(LocalDate.of(2026, 4, 10), "confirmed", null));
        List<MonetaryTransaction> list = ((Result.Success<List<MonetaryTransaction>, DomainError>)
                useCase.listTransactions()).value();
        assertEquals(LocalDate.of(2026, 6, 10), list.get(0).date());
        assertEquals(LocalDate.of(2026, 4, 10), list.get(list.size() - 1).date());
    }

    @Test
    @DisplayName("lançamento criado guarda o centro de custo do comando")
    void persistsCostCenter() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", null));
        assertEquals(costCenterId, txRepo.findAll().get(0).costCenterId());
    }

    // ── Transferências: par indissociável ─────────────────────

    /** Persiste as duas pernas de uma transferência (saída + entrada) sob um mesmo groupId. */
    private UUID saveTransferPair(LocalDate date, UUID fromAccount, UUID toAccount) {
        UUID groupId = UUID.randomUUID();
        txRepo.save(new MonetaryTransaction(UUID.randomUUID(), "Transferência (saída)", new BigDecimal("-50.00"),
                date, categoryId, fromAccount, "confirmed", "expense", costCenterId, date, groupId, 1, 2));
        txRepo.save(new MonetaryTransaction(UUID.randomUUID(), "Transferência (entrada)", new BigDecimal("50.00"),
                date, categoryId, toAccount, "confirmed", "income", costCenterId, date, groupId, 2, 2));
        return groupId;
    }

    @Test
    @DisplayName("excluir uma perna da transferência remove ambas")
    void deleteTransferLegRemovesBothLegs() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        MonetaryTransaction entrada = txRepo.findAll().stream()
                .filter(t -> "income".equals(t.type())).findFirst().orElseThrow();

        Result<Void, DomainError> r = useCase.deleteTransaction(entrada.id(), null);
        assertTrue(r.isSuccess());
        assertEquals(0, txRepo.findAll().size(), "as duas pernas removidas");
    }

    @Test
    @DisplayName("excluir perna de transferência ignora o mode (FUTURE remove ambas)")
    void deleteTransferLegIgnoresMode() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        MonetaryTransaction saida = txRepo.findAll().stream()
                .filter(t -> "expense".equals(t.type())).findFirst().orElseThrow();

        assertTrue(useCase.deleteTransaction(saida.id(), "FUTURE").isSuccess());
        assertEquals(0, txRepo.findAll().size());
    }

    @Test
    @DisplayName("excluir perna em período fechado não remove nenhuma")
    void deleteTransferLegClosedAborts() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        closingRepo.save(YearMonth.of(2026, 5));
        MonetaryTransaction entrada = txRepo.findAll().stream()
                .filter(t -> "income".equals(t.type())).findFirst().orElseThrow();

        assertTrue(useCase.deleteTransaction(entrada.id(), null).isFailure());
        assertEquals(2, txRepo.findAll().size(), "nada removido");
    }

    @Test
    @DisplayName("editar uma perna desfaz o par: sobra avulsa, oposta removida")
    void updateTransferLegDissolvesPair() {
        saveTransferPair(LocalDate.of(2026, 5, 10), accountId, UUID.randomUUID());
        MonetaryTransaction entrada = txRepo.findAll().stream()
                .filter(t -> "income".equals(t.type())).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("ajuste", new BigDecimal("70.00"),
                LocalDate.of(2026, 5, 12), categoryId, accountId, costCenterId, "confirmed", "income", null, null);
        Result<MonetaryTransaction, DomainError> r = useCase.updateTransaction(entrada.id(), upd);
        assertTrue(r.isSuccess());

        List<MonetaryTransaction> all = txRepo.findAll();
        assertEquals(1, all.size(), "perna oposta removida");
        MonetaryTransaction survivor = all.get(0);
        assertEquals(entrada.id(), survivor.id());
        assertNull(survivor.groupId(), "lançamento degrupado (avulso)");
        assertNull(survivor.installmentNumber());
        assertNull(survivor.totalInstallments());
        assertEquals(new BigDecimal("70.00"), survivor.amount());
    }

    @Test
    @DisplayName("grupo de parcelas (tipo único) não é tratado como transferência ao editar")
    void updateInstallmentGroupNotTreatedAsTransfer() {
        useCase.createTransaction(cmd(LocalDate.of(2026, 5, 10), "confirmed", 3));
        MonetaryTransaction first = txRepo.findAll().stream()
                .filter(t -> t.installmentNumber() == 1).findFirst().orElseThrow();

        TransactionCommand upd = new TransactionCommand("upd", new BigDecimal("20.00"),
                LocalDate.of(2026, 5, 15), categoryId, accountId, costCenterId, "confirmed", "expense", null, null);
        assertTrue(useCase.updateTransaction(first.id(), upd).isSuccess());

        assertEquals(3, txRepo.findAll().size(), "parcelas preservadas");
        MonetaryTransaction reload = txRepo.findById(first.id()).orElseThrow();
        assertNotNull(reload.groupId(), "ainda parte do grupo de parcelas");
    }
}
