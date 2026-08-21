package br.cdb.feature.f005;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._1_application.usecase.ReadUseCase;
import br.cdb.feature.f005._1_application.usecase.WriteUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura de categoria da fatia {@code f005} — o par de {@code WriteUseCaseTest}. O fake
 * modela {@code COD_PERSON} (personId é campo de {@link Category}), então o escopo por pessoa é
 * exercitável fora do HTTP; o caminho completo é de {@code F005CategoryResourceTest}.
 */
class ReadUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private ReadUseCase reads;

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        reads = new ReadUseCase();
    }

    private Category seedCategory(UUID personId, String name, Nature nature, UUID parentId) {
        return categoryRepository().save(new Category(UUID.randomUUID(), personId, nature, name, parentId));
    }

    @Test
    @DisplayName("categories devolve só as categorias da pessoa")
    void listsCategoriesOfPerson() {
        seedCategory(PERSON_ID, "Moradia", Nature.EXPENSE, null);
        seedCategory(PERSON_ID, "Salário", Nature.INCOME, null);
        seedCategory(OTHER_PERSON_ID, "De outro dono", Nature.EXPENSE, null);

        val names = reads.categories(PERSON_ID).stream().map(Category::name).toList();

        assertEquals(List.of("Moradia", "Salário"), names);
    }

    @Test
    @DisplayName("categories inclui a categoria global de transferência quando ela já existe")
    void listsIncludeGlobalTransferCategory() {
        seedCategory(PERSON_ID, "Moradia", Nature.EXPENSE, null);
        val transfer = reads.transferCategory(PERSON_ID, Nature.EXPENSE);

        val ids = reads.categories(PERSON_ID).stream().map(Category::id).toList();

        assertTrue(ids.contains(transfer.id()), "global de transferência entra na listagem de qualquer pessoa");
    }

    @Test
    @DisplayName("transferCategory semeia a global uma vez e reusa depois (id fixo por natureza)")
    void transferCategoryIsGlobalAndStable() {
        val first = reads.transferCategory(PERSON_ID, Nature.EXPENSE);
        val again = reads.transferCategory(OTHER_PERSON_ID, Nature.EXPENSE);
        val income = reads.transferCategory(PERSON_ID, Nature.INCOME);

        assertEquals(first.id(), again.id(), "mesma categoria para qualquer pessoa");
        assertTrue(first.isSystem());
        assertNotEquals(first.id(), income.id(), "uma por natureza");
    }

    @Test
    @DisplayName("category inexistente → NotFound")
    void findUnknownCategory() {
        val r = reads.category(UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Category, BusinessError>) r).error());
    }

    @Test
    @DisplayName("subtreeIds devolve a raiz + descendentes em pré-ordem")
    void subtreeIdsCollectsDescendants() {
        val root = seedCategory(PERSON_ID, "Moradia", Nature.EXPENSE, null);
        val child = seedCategory(PERSON_ID, "Aluguel", Nature.EXPENSE, root.id());
        seedCategory(PERSON_ID, "Salário", Nature.INCOME, null);

        val r = reads.subtreeIds(root.id(), PERSON_ID);

        assertTrue(r.isSuccess());
        assertEquals(List.of(root.id(), child.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
    }

    @Test
    @DisplayName("subtreeIds de categoria de outra pessoa → NotFound")
    void subtreeIdsHidesCategoryOfAnotherPerson() {
        val foreign = seedCategory(OTHER_PERSON_ID, "De outro dono", Nature.EXPENSE, null);

        val r = reads.subtreeIds(foreign.id(), PERSON_ID);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }
}
