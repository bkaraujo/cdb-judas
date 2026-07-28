package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f004._0_domain.TransactionCategoryOverlay;
import br.cdb.feature.f005._1_application.UserTransactionService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Único ponto do código que conhece f004 e f005 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link TransactionCategoryOverlay} tem só esta
 * implementação no classpath.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionCategoryOverlayAdapter implements TransactionCategoryOverlay {

    private final UserTransactionService userTransactionService;

    @Override
    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return userTransactionService.findTransactionIdsByCategories(personId, categoryIds);
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        userTransactionService.reassignCategory(oldCategoryId, newCategoryId, personId);
    }
}
