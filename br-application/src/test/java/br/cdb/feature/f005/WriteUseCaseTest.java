package br.cdb.feature.f005;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.cdb.feature.f000._0_domain.event.CategoryReassigned;
import br.cdb.feature.f005._0_domain.Category;
import br.cdb.feature.f005._1_application.usecase.ReadUseCase;
import br.cdb.feature.f005._1_application.usecase.WriteUseCase;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a mutação de categoria da fatia {@code f005} — o par de {@code ReadUseCaseTest}. Os caminhos
 * que leem as transações vinculadas ({@code strategy} nula e {@code DELETE}) passam por
 * {@code InternalApi} (HTTP real contra f006), que não é instanciável fora do Quarkus: quem os cobre
 * é {@code F005CategoryResourceTest}. Aqui ficam create/update, o MOVE (que só valida e publica
 * eventos) e as recusas que acontecem antes de qualquer leitura cross-slice.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();

    private WriteUseCase useCase;
    private Events events;

    /** Captura a cascata de exclusão — o efeito do MOVE só é observável pelos eventos. */
    static class Events {
        final List<CategoryReassigned> reassigned = new ArrayList<>();
        final List<CategoryDeleted> deleted = new ArrayList<>();

        @MessageListener
        public MessageResult onReassigned(CategoryReassigned event) {
            reassigned.add(event);
            return MessageResult.CONSUMED;
        }

        @MessageListener
        public MessageResult onDeleted(CategoryDeleted event) {
            deleted.add(event);
            return MessageResult.CONSUMED;
        }
    }

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        useCase = new WriteUseCase();

        events = new Events();
        MessageBus.subscribe(events);
    }

    private Category seedCategory(String name, Transaction.Type nature, UUID parentId) {
        return categoryRepository().save(new Category(UUID.randomUUID(), PERSON_ID, nature, name, parentId));
    }

    private Category seedSystemCategory(String name) {
        return categoryRepository().save(
                new Category(UUID.randomUUID(), PERSON_ID, Transaction.Type.EXPENSE, name, null, true));
    }

    @Test
    @DisplayName("createCategory cria macro-categoria da pessoa")
    void createsRootCategory() {
        val r = useCase.createCategory(PERSON_ID, "Moradia", Transaction.Type.EXPENSE, null);

        assertTrue(r.isSuccess());
        val created = ((Result.Success<Category, BusinessError>) r).value();
        assertEquals(PERSON_ID, created.personId());
        assertNull(created.parentId());
        assertTrue(categoryRepository().findById(created.id()).isPresent());
    }

    @Test
    @DisplayName("createCategory com pai de outra natureza → BusinessRule")
    void rejectsParentOfAnotherNature() {
        val parent = seedCategory("Salário", Transaction.Type.INCOME, null);

        val r = useCase.createCategory(PERSON_ID, "Aluguel", Transaction.Type.EXPENSE, parent.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Category, BusinessError>) r).error());
    }

    @Test
    @DisplayName("createCategory com subcategoria como pai → BusinessRule (só dois níveis)")
    void rejectsSubcategoryAsParent() {
        val root = seedCategory("Moradia", Transaction.Type.EXPENSE, null);
        val child = seedCategory("Aluguel", Transaction.Type.EXPENSE, root.id());

        val r = useCase.createCategory(PERSON_ID, "Condomínio", Transaction.Type.EXPENSE, child.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Category, BusinessError>) r).error());
    }

    @Test
    @DisplayName("createCategory com nome duplicado no mesmo nível → BusinessRule")
    void rejectsDuplicateNameOnSameLevel() {
        seedCategory("Moradia", Transaction.Type.EXPENSE, null);

        val r = useCase.createCategory(PERSON_ID, "moradia", Transaction.Type.EXPENSE, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Category, BusinessError>) r).error());
    }

    @Test
    @DisplayName("updateCategory troca nome e mantém o dono")
    void updatesCategory() {
        val category = seedCategory("Moradia", Transaction.Type.EXPENSE, null);

        val r = useCase.updateCategory(PERSON_ID, category.id(), "Casa", null, null);

        assertTrue(r.isSuccess());
        val updated = ((Result.Success<Category, BusinessError>) r).value();
        assertEquals("Casa", updated.name());
        assertEquals(PERSON_ID, updated.personId());
    }

    @Test
    @DisplayName("updateCategory de categoria de sistema → BusinessRule")
    void rejectsUpdateOfSystemCategory() {
        val system = seedSystemCategory("Transferência");

        val r = useCase.updateCategory(PERSON_ID, system.id(), "Outra", null, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<Category, BusinessError>) r).error());
    }

    @Test
    @DisplayName("deleteCategory de categoria de sistema → BusinessRule (antes de qualquer leitura)")
    void rejectsDeleteOfSystemCategory() {
        val system = seedSystemCategory("Transferência");

        val r = useCase.deleteCategory(PERSON_ID, system.id(), null, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<?, BusinessError>) r).error());
    }

    @Test
    @DisplayName("deleteCategory MOVE publica reatribuição da subárvore + exclusão")
    void moveReassignsSubtreeAndDeletes() {
        val root = seedCategory("Moradia", Transaction.Type.EXPENSE, null);
        val child = seedCategory("Aluguel", Transaction.Type.EXPENSE, root.id());
        val targetRoot = seedCategory("Casa", Transaction.Type.EXPENSE, null);
        val target = seedCategory("Prestação", Transaction.Type.EXPENSE, targetRoot.id());

        val r = useCase.deleteCategory(PERSON_ID, root.id(), DeletionStrategy.MOVE, target.id());

        assertTrue(r.isSuccess());
        assertEquals(1, events.reassigned.size());
        assertEquals(List.of(root.id(), child.id()), events.reassigned.getFirst().oldCategoryIds());
        assertEquals(target.id(), events.reassigned.getFirst().newCategoryId());
        assertEquals(List.of(root.id(), child.id()), events.deleted.getFirst().categoryIds());
    }

    @Test
    @DisplayName("deleteCategory MOVE para macro-categoria → BusinessRule, sem evento")
    void moveRejectsRootTarget() {
        val root = seedCategory("Moradia", Transaction.Type.EXPENSE, null);
        val target = seedCategory("Casa", Transaction.Type.EXPENSE, null);

        val r = useCase.deleteCategory(PERSON_ID, root.id(), DeletionStrategy.MOVE, target.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<?, BusinessError>) r).error());
        assertTrue(events.reassigned.isEmpty());
        assertTrue(events.deleted.isEmpty());
    }

    @Test
    @DisplayName("deleteCategory MOVE para dentro da própria subárvore → BusinessRule")
    void moveRejectsTargetInsideSubtree() {
        val root = seedCategory("Moradia", Transaction.Type.EXPENSE, null);
        val child = seedCategory("Aluguel", Transaction.Type.EXPENSE, root.id());

        val r = useCase.deleteCategory(PERSON_ID, root.id(), DeletionStrategy.MOVE, child.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<?, BusinessError>) r).error());
        assertTrue(events.reassigned.isEmpty());
    }
}
