package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.f005._1_application.UserCategoryService;
import br.cdb.feature.f006._0_domain.TransferCategories;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Único ponto do código que conhece f005 e f006 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link TransferCategories} tem só esta implementação
 * no classpath.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransferCategoriesAdapter implements TransferCategories {

    private final UserCategoryService userCategoryService;

    @Override
    public UUID transferCategoryId(UUID personId, Transaction.Type nature) {
        return userCategoryService.findOrCreateTransferCategory(personId, nature).id();
    }
}
