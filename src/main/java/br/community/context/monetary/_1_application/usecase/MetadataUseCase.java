package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.CategoryEvents;
import br.community.context.monetary._0_domain.event.TagEvents;
import br.community.context.monetary._0_domain.model.Category;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.CategoryCommand;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.command.TagCommand;
import br.community.context.monetary._1_application.service.CategoryService;
import br.community.context.monetary._1_application.service.CostCenterService;
import br.community.context.monetary._1_application.service.TagService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MetadataUseCase {

    private final TagService tagService;
    private final CategoryService categoryService;
    private final CostCenterService costCenterService;
    private final TransactionService transactionService;

    public Result<List<Category>, DomainError> listCategories() {
        return Result.success(categoryService.findAll());
    }

    public Result<Category, DomainError> findCategoryById(UUID id) {
        return categoryService.findById(id);
    }

    public Category findOrCreateUncategorizedCategory() {
        return categoryService.findOrCreateUncategorizedCategory();
    }

    public Result<Category, DomainError> createCategory(CategoryCommand cmd) {
        if (cmd.parentId() != null) {
            val validation = categoryService.validateParent(cmd.parentId(), cmd.nature());
            if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);
        }

        val nameConflict = categoryService.validateUniqueName(cmd.name(), cmd.parentId(), null);
        if (nameConflict instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);

        val created = categoryService.save(UUID.randomUUID(), cmd.nature(), cmd.name(), cmd.parentId());
        MessageBus.submit(new CategoryEvents.Created(created));
        return Result.success(created);
    }

    public Result<Category, DomainError> updateCategory(UUID id, CategoryCommand cmd) {
        return categoryService.findById(id)
                .flatMap(existing -> {
                    if (existing.isSystem()) {
                        return Result.<Category>failure(new DomainError.BusinessRule("Categoria de sistema não pode ser modificada"));
                    }
                    if (cmd.parentId() != null) {
                        val validation = categoryService.validateParent(cmd.parentId(), existing.nature());
                        if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.<Category>failure(error);
                    }

                    val nameConflict = categoryService.validateUniqueName(cmd.name(), cmd.parentId(), id);
                    if (nameConflict instanceof Result.Failure<Void, DomainError>(var error)) return Result.<Category>failure(error);

                    val updated = categoryService.save(id, existing.nature(), cmd.name(), cmd.parentId(), existing.isSystem());
                    MessageBus.submit(new CategoryEvents.Updated(updated));
                    return Result.<Category, DomainError>success(updated);
                });
    }

    public Result<Void, DomainError> deleteCategory(UUID id) {
        val all = categoryService.findAll();
        val root = all.stream().filter(c -> c.id().equals(id)).findFirst();
        if (root.isEmpty()) return Result.failure(new DomainError.NotFound("Category not found: " + id));
        if (root.get().isSystem()) {
            return Result.failure(new DomainError.BusinessRule("Categoria de sistema não pode ser excluída"));
        }

        val nature = root.get().nature();
        val others = categoryService.findOrCreateOthersCategory(nature);
        if (!others.id().equals(id)) {
            MessageBus.submit(new CategoryEvents.Updated(others));
        }

        deleteRecursive(id, others.id(), all);
        return Result.success();
    }

    private void deleteRecursive(UUID id, UUID othersId, List<Category> all) {
        if (id.equals(othersId)) return;

        transactionService.findAll().stream()
                .filter(t -> id.equals(t.categoryId()))
                .forEach(t -> {
                    val updated = new Transaction(
                            t.id(), t.description(), t.amount(), t.date(),
                            othersId, t.accountId(), t.status(), t.type(), t.costCenterId(), t.paymentDate(),
                            t.groupId(), t.installmentNumber(), t.totalInstallments(), t.notes()
                    );
                    transactionService.save(updated);
                });

        all.stream()
                .filter(c -> id.equals(c.parentId()))
                .forEach(c -> deleteRecursive(c.id(), othersId, all));

        categoryService.deleteById(id);
        MessageBus.submit(new CategoryEvents.Deleted(id));
    }

    public Result<List<CostCenter>, DomainError> listCostCenters() {
        return Result.success(costCenterService.findAll());
    }

    public Result<CostCenter, DomainError> createCostCenter(CostCenterCommand cmd) {
        val created = costCenterService.save(UUID.randomUUID(), cmd.description());
        return Result.success(created);
    }

    public Result<CostCenter, DomainError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        val updated = costCenterService.save(id, cmd.description());
        return Result.success(updated);
    }

    public Result<Void, DomainError> deleteCostCenter(UUID id) {
        costCenterService.deleteById(id);
        return Result.success();
    }

    public Result<List<Tag>, DomainError> listTags() {
        return Result.success(tagService.findAll());
    }

    public Result<Tag, DomainError> createTag(TagCommand cmd) {
        val created = tagService.save(UUID.randomUUID(), cmd.name(), cmd.color());
        MessageBus.submit(new TagEvents.Created(created));
        return Result.success(created);
    }

    public Result<Tag, DomainError> updateTag(UUID id, TagCommand cmd) {
        return tagService.findById(id)
                .map(existing -> {
                    val updated = tagService.save(id, cmd.name(), cmd.color());
                    MessageBus.submit(new TagEvents.Updated(updated));
                    return updated;
                });
    }

    public Result<Void, DomainError> deleteTag(UUID id) {
        return tagService.deleteById(id)
                .ifSuccess(ignored -> MessageBus.submit(new TagEvents.Deleted(id)));
    }
}
