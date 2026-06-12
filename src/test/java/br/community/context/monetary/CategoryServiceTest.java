package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryTransaction.Type;
import br.community.context.monetary._1_application.service.CategoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {

    private final CategoryService service = new CategoryService(new InMemoryRepositories.Categories());

    @Test
    void findOrCreateUncategorizedCreatesSystemTopLevelExpenseCategory() {
        var category = service.findOrCreateUncategorizedCategory();

        assertEquals("Sem categoria", category.name());
        assertEquals(Type.EXPENSE, category.nature());
        assertNull(category.parentId());
        assertTrue(category.isSystem());
    }

    @Test
    void findOrCreateUncategorizedIsIdempotent() {
        var first = service.findOrCreateUncategorizedCategory();
        var second = service.findOrCreateUncategorizedCategory();

        assertEquals(first.id(), second.id());
        assertEquals(1L, service.findAll().stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()))
                .map(MonetaryCategory::id)
                .distinct()
                .count());
    }
}
