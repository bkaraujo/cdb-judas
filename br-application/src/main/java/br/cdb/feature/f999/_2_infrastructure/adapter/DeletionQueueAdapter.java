package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f999._1_application.DeletionQueueService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Único ponto do código que conhece f002 e f999 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link DeletionQueue} tem só esta implementação no
 * classpath.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class DeletionQueueAdapter implements DeletionQueue {

    private final DeletionQueueService service;

    @Override
    public void enqueueAccountDeleted(UUID accountId, UUID personId) {
        service.enqueue(DeletionQueueService.TYPE_ACCOUNT_DELETED, accountId, personId);
    }

    @Override
    public void enqueueTransactionDeleted(UUID transactionId, UUID personId) {
        service.enqueue(DeletionQueueService.TYPE_TRANSACTION_DELETED, transactionId, personId);
    }
}
