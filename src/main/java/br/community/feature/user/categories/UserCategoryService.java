package br.community.feature.user.categories;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.accounts.transactions.UserTransactionService;
import br.community.feature.user.categories.core.CategoryResponse;
import br.community.feature.user.stream.SSE;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserCategoryService {

    private static final String TYPE = "CATEGORY";

    private final UserCategoryRepository repo;
    private final UserTransactionService userTransactionService;
    private final SSE sse;

    public List<UserCategory> findAll(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Result<UserCategory, DomainError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserCategory, DomainError>>map(Result::success)
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

    public Result<Void, DomainError> validateParent(UUID parentId, Transaction.Type nature) {
        return repo.findById(parentId)
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

    public Result<Void, DomainError> validateUniqueName(UUID userId, String name, @Nullable UUID parentId, @Nullable UUID excludeId) {
        val duplicate = repo.findAllByUser(userId).stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .findFirst();

        if (duplicate.isPresent()) {
            return Result.failure(new DomainError.BusinessRule("Já existe uma categoria com o nome '" + name + "' neste nível"));
        }
        return Result.success();
    }

    public UserCategory create(UUID userId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        val saved = repo.save(new UserCategory(UUID.randomUUID(), userId, nature, name, parentId));
        upsert(saved);
        return saved;
    }

    public UserCategory update(UUID id, String name, @Nullable UUID parentId) {
        val existing = repo.findById(id).orElseThrow(() -> new IllegalStateException("Category not found: " + id));
        val saved = repo.save(new UserCategory(id, existing.userId(), existing.nature(), name, parentId,
                existing.isSystem(), existing.active(), existing.createdAt(), existing.updatedAt()));
        upsert(saved);
        return saved;
    }

    public UserCategory findOrCreateOthersCategory(UUID userId, Transaction.Type nature) {
        return repo.findAllByUser(userId).stream()
                .filter(c -> "Outros".equalsIgnoreCase(c.name()) && c.nature() == nature && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> {
                    val created = repo.save(new UserCategory(UUID.randomUUID(), userId, nature, "Outros", null));
                    upsert(created);
                    return created;
                });
    }

    public UserCategory findOrCreateUncategorizedCategory(UUID userId) {
        return repo.findAllByUser(userId).stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()) && c.nature() == Transaction.Type.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> {
                    val created = repo.save(new UserCategory(UUID.randomUUID(), userId, Transaction.Type.EXPENSE, "Sem categoria", null, true));
                    upsert(created);
                    return created;
                });
    }

    public Result<Void, DomainError> deleteById(UUID id, UUID userId) {
        val all = repo.findAllByUser(userId);
        val root = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (root.isEmpty()) return Result.failure(new DomainError.NotFound("Category not found: " + id));
        if (root.get().isSystem()) {
            return Result.failure(new DomainError.BusinessRule("Categoria de sistema não pode ser excluída"));
        }

        val nature = root.get().nature();
        val others = findOrCreateOthersCategory(userId, nature);

        deleteRecursive(id, others.id(), userId, all);
        return Result.success();
    }

    private void deleteRecursive(UUID id, UUID othersId, UUID userId, List<UserCategory> all) {
        if (id.equals(othersId)) return;

        userTransactionService.reassignCategory(id, othersId, userId);

        all.stream()
                .filter(c -> id.equals(c.parentId()))
                .forEach(c -> deleteRecursive(c.id(), othersId, userId, all));

        repo.deleteById(id);
        delete(userId, id);
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(UserCategory category) {
        try {
            sse.dispatch(category.userId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", CategoryResponse.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID userId, UUID categoryId) {
        try {
            sse.dispatch(userId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", categoryId.toString()));
        } catch (Exception ignored) {}
    }
}
