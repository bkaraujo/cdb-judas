package br.cdb.feature.f005._1_application;

import br.cdb.feature.f005._0_domain.Category;
import br.cdb.feature.f005._0_domain.CategoryRepository;
import br.cdb.feature.f005._0_domain.event.CategoryEvents;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@NullMarked
@Singleton
public class UserCategoryService {

    /**
     * Categoria de sistema global "Transferência" (uma por natureza), IDs fixos semeados em
     * bancos de dev pré-existentes por {@code ContextMergeMigration.seedSystemTransferCategories}
     * sob a pessoa reservada {@link #SYSTEM_PERSON_ID}; num banco fresh/teste (onde a migração é
     * no-op) {@link #findOrCreateTransferCategory} semeia sob os mesmos IDs no primeiro uso. Nunca
     * mais por-pessoa — evita recriar "Transferência" a cada transferência (era o bug: cada chamada
     * gerava sua própria cópia por não enxergar a global, que pertence a outra pessoa).
     */
    private static final UUID SYSTEM_PERSON_ID = UUID.fromString("f9990000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_CATEGORY_EXPENSE_ID = UUID.fromString("f9990000-0000-0000-0000-000000000002");
    private static final UUID TRANSFER_CATEGORY_INCOME_ID = UUID.fromString("f9990000-0000-0000-0000-000000000003");
    private static final String TRANSFER_CATEGORY_NAME = "Transferência";

    private final CategoryRepository repo = Context.get(CategoryRepository.class);

    /** Categorias da pessoa + as 2 globais de transferência (pertencem a {@link #SYSTEM_PERSON_ID},
     *  por isso não vêm de {@code findAllByPerson(personId)} — sem elas a tela de categorias e a
     *  resolução de nome de qualquer perna de transferência não enxergam a categoria do sistema). */
    public List<Category> findAll(UUID personId) {
        val categories = new ArrayList<>(repo.findAllByPerson(personId));
        repo.findById(TRANSFER_CATEGORY_EXPENSE_ID).ifPresent(categories::add);
        repo.findById(TRANSFER_CATEGORY_INCOME_ID).ifPresent(categories::add);
        return categories;
    }

    public Result<Category, BusinessError> findById(UUID id) {
        return repo.findById(id)
                .<Result<Category, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Category not found: " + id)));
    }

    public Result<Void, BusinessError> validateNotMacroCategory(UUID categoryId) {
        return findById(categoryId).flatMap(cat -> {
            if (cat.parentId() == null) {
                return Result.failure(new BusinessError.BusinessRule(
                        "Não é permitido lançar transações em macro-categorias. Selecione uma subcategoria."));
            }
            return Result.success();
        });
    }

    public Result<Void, BusinessError> validateParent(UUID parentId, Transaction.Type nature) {
        return repo.findById(parentId)
                .<Result<Void, BusinessError>>map(parent -> {
                    if (parent.parentId() != null) {
                        return Result.failure(new BusinessError.BusinessRule("Subcategoria não pode ter outra subcategoria como pai"));
                    }
                    if (parent.nature() != nature) {
                        return Result.failure(new BusinessError.BusinessRule("Natureza da subcategoria deve ser igual à do pai"));
                    }
                    return Result.success();
                })
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Category not found: " + parentId)));
    }

    public Result<Void, BusinessError> validateUniqueName(UUID personId, String nature, String name, @Nullable UUID parentId, @Nullable UUID excludeId) {
        val optional = repo.findByNature(personId, Transaction.Type.valueOf(nature)).stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .findFirst();

        if (optional.isPresent()) {
            return Result.failure(new BusinessError.BusinessRule("Já existe uma categoria com o nome '" + name + "' neste nível"));
        }
        return Result.success();
    }

    public Category create(UUID personId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        val saved = repo.save(new Category(UUID.randomUUID(), personId, nature, name, parentId));
        MessageBus.submit(new CategoryEvents.Created(saved));
        return saved;
    }

    public Category update(UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        val existing = repo.findById(id).orElseThrow(() -> new IllegalStateException("Category not found: " + id));
        val saved = repo.save(new Category(id, existing.personId(), existing.nature(), name, parentId,
                existing.isSystem(), active != null ? active : existing.active(), existing.createdAt(), existing.updatedAt()));
        MessageBus.submit(new CategoryEvents.Updated(saved));
        return saved;
    }

    public Category findOrCreateUncategorizedCategory(UUID personId) {
        return repo.findAllByPerson(personId).stream()
                .filter(c -> "Sem categoria".equalsIgnoreCase(c.name()) && c.nature() == Transaction.Type.EXPENSE && c.parentId() == null)
                .findFirst()
                .orElseGet(() -> {
                    val created = repo.save(new Category(UUID.randomUUID(), personId, Transaction.Type.EXPENSE, "Sem categoria", null, true));
                    MessageBus.submit(new CategoryEvents.Created(created));
                    return created;
                });
    }

    /**
     * Categoria de sistema global "Transferência" da natureza pedida (uma para EXPENSE, outra para
     * INCOME), compartilhada por todas as pessoas — usada por cada perna de uma transferência.
     * {@code personId} do chamador não influencia o resultado; a autocura sob {@link #SYSTEM_PERSON_ID}
     * só é exercitada em banco fresh/teste, onde {@code ContextMergeMigration} não roda.
     */
    public Category findOrCreateTransferCategory(Transaction.Type nature) {
        val id = nature == Transaction.Type.EXPENSE ? TRANSFER_CATEGORY_EXPENSE_ID : TRANSFER_CATEGORY_INCOME_ID;
        return repo.findById(id).orElseGet(() -> {
            val created = repo.save(new Category(id, SYSTEM_PERSON_ID, nature, TRANSFER_CATEGORY_NAME, null, true));
            MessageBus.submit(new CategoryEvents.Created(created));
            return created;
        });
    }

    /** Ids da categoria + toda a subárvore (pré-ordem). Falha se {@code personId} não existe ou é de sistema. */
    public Result<List<UUID>, BusinessError> subtreeIds(UUID id, UUID personId) {
        val all = repo.findAllByPerson(personId);
        val root = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (root.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + id));
        if (root.get().isSystem()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de sistema não pode ser excluída"));
        }

        val ids = new ArrayList<UUID>();
        collectSubtree(id, all, ids);
        return Result.success(ids);
    }

    /** Valida o alvo do MOVE (existe, subcategoria, mesma natureza, ativa, fora da subárvore) e, se ok, devolve a subárvore.
     *  Não reatribui transações nem apaga linhas — isso é orquestrado pelo {@link CategoryUseCase}, que precisa
     *  da subárvore validada antes de tocar no vínculo transação↔categoria (fatia vizinha, f006). */
    public Result<List<UUID>, BusinessError> validateMoveTarget(UUID id, UUID targetId, UUID personId) {
        val all = repo.findAllByPerson(personId);
        val rootOpt = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (rootOpt.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + id));
        val root = rootOpt.get();

        val targetOpt = all.stream().filter(c -> c.id().equals(targetId)).findFirst();
        if (targetOpt.isEmpty()) return Result.failure(new BusinessError.NotFound("Category not found: " + targetId));
        val target = targetOpt.get();

        if (target.parentId() == null) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino deve ser uma subcategoria"));
        }
        if (target.nature() != root.nature()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino deve ter a mesma natureza"));
        }
        if (!target.active()) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino está inativa"));
        }

        val subtree = new ArrayList<UUID>();
        collectSubtree(id, all, subtree);
        if (subtree.contains(targetId)) {
            return Result.failure(new BusinessError.BusinessRule("Categoria de destino não pode pertencer à subárvore excluída"));
        }
        return Result.success(subtree);
    }

    /** Subárvore sem nenhum vínculo (ou já desvinculada por quem chama): apaga direto. */
    public void deletePlain(List<UUID> subtreeIds, UUID personId) {
        deleteSubtreeRows(subtreeIds, personId);
    }

    private void collectSubtree(UUID id, List<Category> all, List<UUID> acc) {
        acc.add(id);
        all.stream().filter(c -> id.equals(c.parentId())).forEach(c -> collectSubtree(c.id(), all, acc));
    }

    private void deleteSubtreeRows(List<UUID> ids, UUID personId) {
        for (val nodeId : ids) {
            repo.deleteById(nodeId);
            MessageBus.submit(new CategoryEvents.Deleted(nodeId, personId));
        }
    }
}
