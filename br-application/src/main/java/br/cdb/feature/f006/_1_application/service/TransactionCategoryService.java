package br.cdb.feature.f006._1_application.service;

import br.cdb.feature.f006._0_domain.repository.TransactionCategoryRepository;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

/** Context-wired, sem CDI ({@code Context.tryGet(TransactionCategoryService.class)}) — usado direto
 *  por {@code WriteUseCases}/{@code ReadUseCases}. */
@NullMarked
public class TransactionCategoryService {

    private final TransactionCategoryRepository repository = Context.get(TransactionCategoryRepository.class);

    public void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId) {
        repository.saveCategory(transactionId, personId, categoryId);
    }

    public Optional<UUID> findCategory(UUID transactionId, UUID personId) {
        return repository.findCategory(transactionId, personId);
    }

    public Map<UUID, UUID> findCategoriesByPerson(UUID personId) {
        return repository.findCategoriesByPerson(personId);
    }

    public void deleteCategoryByTransaction(UUID transactionId) {
        repository.deleteCategoryByTransaction(transactionId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        repository.reassignCategory(oldCategoryId, newCategoryId, personId);
    }

    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return repository.findTransactionIdsByCategories(personId, categoryIds);
    }
}
