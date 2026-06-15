package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._1_application.command.CategoryCommand;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.command.TagCommand;
import br.community.context.monetary._1_application.service.*;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
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

/** Cobre §3 (categorias), §5 (fechamento), §9 (centro de custo / tags). */

class MetadataUseCaseTest {

    private InMemoryRepositories.Categories categoryRepo;
    private InMemoryRepositories.Tags tagRepo;
    private InMemoryRepositories.CostCenters costCenterRepo;
    private InMemoryRepositories.Transactions txRepo;
    private InMemoryRepositories.Closings closingRepo;
    private MetadataUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepo = new InMemoryRepositories.Categories();
        tagRepo = new InMemoryRepositories.Tags();
        costCenterRepo = new InMemoryRepositories.CostCenters();
        txRepo = new InMemoryRepositories.Transactions();
        closingRepo = new InMemoryRepositories.Closings();
        useCase = new MetadataUseCase(
                new TagService(tagRepo),
                new ClosingService(closingRepo),
                new CategoryService(categoryRepo),
                new CostCenterService(costCenterRepo),
                new TransactionService(txRepo));
    }

    private MonetaryCategory seedRoot(String name, MonetaryTransaction.Type nature) {
        return categoryRepo.save(new MonetaryCategory(UUID.randomUUID(), nature, name, null));
    }

    // ── §3 Categorias ──────────────────────────────────────────────

    @Test
    @DisplayName("§3 cria categoria raiz")
    void createsRoot() {
        Result<MonetaryCategory, DomainError> r = useCase.createCategory(
                new CategoryCommand("Casa", MonetaryTransaction.Type.EXPENSE, null));
        assertTrue(r.isSuccess());
        assertNull(((Result.Success<MonetaryCategory, DomainError>) r).value().parentId());
    }

    @Test
    @DisplayName("§3.2 subcategoria com pai raiz e mesma natureza")
    void createsValidSubcategory() {
        MonetaryCategory root = seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        Result<MonetaryCategory, DomainError> r = useCase.createCategory(
                new CategoryCommand("Luz", MonetaryTransaction.Type.EXPENSE, root.id()));
        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("§3.2 pai que já é subcategoria é proibido (máx 2 níveis)")
    void rejectsGrandchild() {
        MonetaryCategory root = seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        MonetaryCategory sub = categoryRepo.save(
                new MonetaryCategory(UUID.randomUUID(), MonetaryTransaction.Type.EXPENSE, "Luz", root.id()));
        Result<MonetaryCategory, DomainError> r = useCase.createCategory(
                new CategoryCommand("Geladeira", MonetaryTransaction.Type.EXPENSE, sub.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class,
                ((Result.Failure<MonetaryCategory, DomainError>) r).error());
    }

    @Test
    @DisplayName("§3.2 natureza diferente do pai falha")
    void rejectsNatureMismatch() {
        MonetaryCategory root = seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        Result<MonetaryCategory, DomainError> r = useCase.createCategory(
                new CategoryCommand("Outros", MonetaryTransaction.Type.INCOME, root.id()));
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§3.4 dois nomes iguais no mesmo nível bloqueia")
    void rejectsDuplicateName() {
        seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        Result<MonetaryCategory, DomainError> r = useCase.createCategory(
                new CategoryCommand("casa", MonetaryTransaction.Type.EXPENSE, null));
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§3.1 update preserva a natureza original")
    void updatePreservesNature() {
        MonetaryCategory root = seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        Result<MonetaryCategory, DomainError> r = useCase.updateCategory(root.id(),
                new CategoryCommand("Casa2", MonetaryTransaction.Type.INCOME, null));
        assertTrue(r.isSuccess());
        MonetaryCategory after = ((Result.Success<MonetaryCategory, DomainError>) r).value();
        assertEquals(MonetaryTransaction.Type.EXPENSE, after.nature(), "natureza não muda");
        assertEquals("Casa2", after.name());
    }

    @Test
    @DisplayName("§3.3 delete reatribui transações para 'Outros' e exclui sub-categorias")
    void deleteReassignsTransactionsAndRecursiveSubs() {
        MonetaryCategory root = seedRoot("Casa", MonetaryTransaction.Type.EXPENSE);
        MonetaryCategory sub = categoryRepo.save(
                new MonetaryCategory(UUID.randomUUID(), MonetaryTransaction.Type.EXPENSE, "Luz", root.id()));

        UUID txId = UUID.randomUUID();
        txRepo.save(new MonetaryTransaction(txId, "x", new BigDecimal("1.00"),
                LocalDate.of(2026, 5, 1), sub.id(), UUID.randomUUID(),
                MonetaryTransaction.Status.CONFIRMED, MonetaryTransaction.Type.EXPENSE, MonetaryCenter.VARIAVEL_ID, null, null, 1, 1, null));

        Result<Void, DomainError> r = useCase.deleteCategory(root.id());
        assertTrue(r.isSuccess());

        // categoria raiz e subcategoria sumiram
        assertTrue(categoryRepo.findById(root.id()).isEmpty());
        assertTrue(categoryRepo.findById(sub.id()).isEmpty());

        // "Outros" criado, transação reatribuída para ele
        MonetaryCategory others = categoryRepo.findAll().stream()
                .filter(c -> "Outros".equalsIgnoreCase(c.name()) && c.nature() == MonetaryTransaction.Type.EXPENSE)
                .findFirst().orElseThrow();
        assertEquals(others.id(), txRepo.findById(txId).orElseThrow().categoryId());
    }

    @Test
    @DisplayName("§3.3 deletar inexistente retorna NotFound")
    void deleteUnknownCategory() {
        Result<Void, DomainError> r = useCase.deleteCategory(UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<Void, DomainError>) r).error());
    }

    // ── §5 Closing ────────────────────────────────────────────────

    @Test
    @DisplayName("§5 set / clear closing")
    void closingLifecycle() {
        useCase.setClosingPeriod(YearMonth.of(2026, 4));
        assertEquals(YearMonth.of(2026, 4), useCase.getClosingPeriod().orElseThrow());
        useCase.clearClosingPeriod();
        assertTrue(useCase.getClosingPeriod().isEmpty());
    }

    @Test
    @DisplayName("§5 validateDate: data ≤ fechamento → falha; depois → sucesso")
    void closingValidateDate() {
        useCase.setClosingPeriod(YearMonth.of(2026, 5));
        assertTrue(useCase.validateDate(LocalDate.of(2026, 5, 31)).isFailure());
        assertTrue(useCase.validateDate(LocalDate.of(2026, 4, 1)).isFailure());
        assertTrue(useCase.validateDate(LocalDate.of(2026, 6, 1)).isSuccess());
    }

    // ── §9 CostCenter / Tags ─────────────────────────────────────

    @Test
    @DisplayName("§9.1 CRUD de centro de custo")
    void costCenterCrud() {
        Result<MonetaryCenter, DomainError> created = useCase.createCostCenter(new CostCenterCommand("Filial"));
        assertTrue(created.isSuccess());
        UUID id = ((Result.Success<MonetaryCenter, DomainError>) created).value().id();

        useCase.updateCostCenter(id, new CostCenterCommand("Matriz"));
        assertEquals("Matriz", costCenterRepo.findById(id).orElseThrow().description());

        useCase.deleteCostCenter(id);
        assertTrue(costCenterRepo.findById(id).isEmpty());
    }

    @Test
    @DisplayName("§9.2 update de tag inexistente retorna NotFound")
    void updateUnknownTagNotFound() {
        Result<Tag, DomainError> r = useCase.updateTag(UUID.randomUUID(),
                new TagCommand("name", "#112233"));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<Tag, DomainError>) r).error());
    }

    @Test
    @DisplayName("§9.2 update tag existente sucesso")
    void updateExistingTag() {
        Result<Tag, DomainError> created = useCase.createTag(new TagCommand("old", "#aabbcc"));
        UUID id = ((Result.Success<Tag, DomainError>) created).value().id();
        Result<Tag, DomainError> upd = useCase.updateTag(id, new TagCommand("new", "#112233"));
        assertTrue(upd.isSuccess());
        assertEquals("new", tagRepo.findById(id).orElseThrow().name());
    }

    @Test
    @DisplayName("§9.2 list tags")
    void listTags() {
        useCase.createTag(new TagCommand("a", "#111111"));
        useCase.createTag(new TagCommand("b", "#222222"));
        List<Tag> list = ((Result.Success<List<Tag>, DomainError>) useCase.listTags()).value();
        assertEquals(2, list.size());
    }
}
