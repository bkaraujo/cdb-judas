package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryNature;
import br.community.context.monetary._0_domain.repository.CategoryRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<MonetaryCategory> findAll() {
        return categoryRepository.findAll();
    }

    public Result<MonetaryCategory, DomainError> findById(UUID id) {
        return categoryRepository.findById(id)
                .<Result<MonetaryCategory, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Category not found: " + id)));
    }

    public Result<Void, DomainError> validateNotMacroCategory(UUID categoryId) {
        return findById(categoryId).flatMap(cat -> {
            if (cat.parentId() == null) {
                return Result.failure(new DomainError.BusinessRule(
                        "Não é permitido lançar transações em macro-categorias. Selecione uma subcategoria."));
            }
            return Result.success();
        });
    }

    public MonetaryCategory save(UUID id, MonetaryNature nature, String name, @Nullable UUID parentId) {
        return save(id, nature, name, parentId, false);
    }

    public MonetaryCategory save(UUID id, MonetaryNature nature, String name, @Nullable UUID parentId, boolean isSystem) {
        return categoryRepository.save(new MonetaryCategory(id, nature, name, parentId, isSystem));
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            categoryRepository.deleteById(id);
            return null;
        });
    }

    public Result<Void, DomainError> validateParent(UUID parentId, MonetaryNature nature) {
        return categoryRepository.findById(parentId)
                .<Result<Void, DomainError>>map(parent -> {
                    if (parent.parentId() != null) {
                        return Result.failure(new DomainError.BusinessRule("Subcategoria não pode ter outra subcategoria como pai"));
                    }
                    if (parent.nature() != nature) {
                        return Result.failure(new DomainError.BusinessRule("Natureza da subcategoria deve ser igual à do pai"));
                    }
                    return Result.success();
                })
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Category not found: " + parentId)));
    }

    public Result<Void, DomainError> validateUniqueName(String name, @Nullable UUID parentId, @Nullable UUID excludeId) {
        val duplicate = categoryRepository.findAll().stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .findFirst();

        if (duplicate.isPresent()) {
            return Result.failure(new DomainError.BusinessRule("Já existe uma categoria com o nome '" + name + "' neste nível"));
        }

        return Result.success();
    }

    public MonetaryCategory findOrCreateOthersCategory(MonetaryNature nature) {
        return categoryRepository.findAll().stream()
                .filter(c -> "Outros".equalsIgnoreCase(c.name()) && c.nature() == nature && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new MonetaryCategory(UUID.randomUUID(), nature, "Outros", null)));
    }

    public MonetaryCategory findOrCreateUncategorizedCategory() {
        return categoryRepository.findAll().stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()) && c.nature() == MonetaryNature.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new MonetaryCategory(UUID.randomUUID(), MonetaryNature.EXPENSE, "Sem categoria", null, true)));
    }

    public MonetaryCategory findOrCreateTransferCategory() {
        val others = categoryRepository.findAll().stream()
                .filter(c -> "Outros".equalsIgnoreCase(c.name()) && c.nature() == MonetaryNature.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new MonetaryCategory(UUID.randomUUID(), MonetaryNature.EXPENSE, "Outros", null, true)));

        return categoryRepository.findAll().stream()
                .filter(c -> "Transferência".equalsIgnoreCase(c.name()) && others.id().equals(c.parentId()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new MonetaryCategory(UUID.randomUUID(), MonetaryNature.EXPENSE, "Transferência", others.id(), true)));
    }
}
