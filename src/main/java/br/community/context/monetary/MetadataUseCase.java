package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.*;
import br.community.context.monetary._1_application.command.CategoryCommand;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.command.TagCommand;
import br.community.context.monetary._1_application.service.*;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MetadataUseCase {

    private final TagService tagService;
    private final ClosingService closingService;
    private final CategoryService categoryService;
    private final CostCenterService costCenterService;
    private final TransactionService transactionService;

    public Result<List<MonetaryCategory>, DomainError> listCategories() {
        return Result.success(categoryService.findAll());
    }

    public Result<MonetaryCategory, DomainError> findCategoryById(UUID id) {
        return categoryService.findById(id);
    }

    public Result<MonetaryCategory, DomainError> createCategory(CategoryCommand cmd) {
        if (cmd.parentId() != null) {
            val validation = categoryService.validateParent(cmd.parentId(), cmd.nature());
            if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);
        }

        val nameConflict = categoryService.validateUniqueName(cmd.name(), cmd.parentId(), null);
        if (nameConflict instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);

        val created = categoryService.save(UUID.randomUUID(), cmd.nature(), cmd.name(), cmd.parentId());
        return Result.success(created);
    }

    public Result<MonetaryCategory, DomainError> updateCategory(UUID id, CategoryCommand cmd) {
        return categoryService.findById(id)
                .flatMap(existing -> {
                    if (existing.isSystem()) {
                        return Result.<MonetaryCategory>failure(new DomainError.BusinessRule("Categoria de sistema não pode ser modificada"));
                    }
                    if (cmd.parentId() != null) {
                        val validation = categoryService.validateParent(cmd.parentId(), existing.nature());
                        if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.<MonetaryCategory>failure(error);
                    }

                    val nameConflict = categoryService.validateUniqueName(cmd.name(), cmd.parentId(), id);
                    if (nameConflict instanceof Result.Failure<Void, DomainError>(var error)) return Result.<MonetaryCategory>failure(error);

                    val updated = categoryService.save(id, existing.nature(), cmd.name(), cmd.parentId(), existing.isSystem());
                    return Result.<MonetaryCategory, DomainError>success(updated);
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
            DomainEventPublisher.upsert("CATEGORY", others);
        }

        deleteRecursive(id, others.id(), all);
        return Result.success();
    }

    private void deleteRecursive(UUID id, UUID othersId, List<MonetaryCategory> all) {
        if (id.equals(othersId)) return;

        transactionService.findAll().stream()
                .filter(t -> id.equals(t.categoryId()))
                .forEach(t -> {
                    val updated = new MonetaryTransaction(
                            t.id(), t.description(), t.amount(), t.date(),
                            othersId, t.accountId(), t.status(), t.type(), t.paymentDate(),
                            t.groupId(), t.installmentNumber(), t.totalInstallments()
                    );
                    transactionService.save(updated);
                });

        all.stream()
                .filter(c -> id.equals(c.parentId()))
                .forEach(c -> deleteRecursive(c.id(), othersId, all));

        categoryService.deleteById(id);
        DomainEventPublisher.delete("CATEGORY", id.toString());
    }

    public Optional<YearMonth> getClosingPeriod() {
        return closingService.find();
    }

    public YearMonth setClosingPeriod(YearMonth ym) {
        return closingService.save(ym);
    }

    public void clearClosingPeriod() {
        closingService.clear();
    }

    public Result<Void, DomainError> validateDate(LocalDate date) {
        return closingService.validateDate(date);
    }

    public Result<List<MonetaryCenter>, DomainError> listCostCenters() {
        return Result.success(costCenterService.findAll());
    }

    public Result<MonetaryCenter, DomainError> createCostCenter(CostCenterCommand cmd) {
        val created = costCenterService.save(UUID.randomUUID(), cmd.description());
        return Result.success(created);
    }

    public Result<MonetaryCenter, DomainError> updateCostCenter(UUID id, CostCenterCommand cmd) {
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
        return Result.success(created);
    }

    public Result<Tag, DomainError> updateTag(UUID id, TagCommand cmd) {
        return tagService.findById(id)
                .map(existing -> tagService.save(id, cmd.name(), cmd.color()));
    }

    public Result<Void, DomainError> deleteTag(UUID id) {
        return tagService.deleteById(id);
    }
}
