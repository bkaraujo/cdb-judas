package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f006._1_application.UserTransactionService;
import br.cdb.feature.f007._0_domain.TransactionOverlaySink;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Único ponto do código que conhece f006 e f007 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link TransactionOverlaySink} tem só esta
 * implementação no classpath.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionOverlaySinkAdapter implements TransactionOverlaySink {

    private final UserTransactionService userTransactionService;

    @Override
    public void save(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId) {
        userTransactionService.save(transactionId, accountId, personId, categoryId);
    }
}
