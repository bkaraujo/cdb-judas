package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f999._1_application.DeletionQueueService;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Único ponto do código que conhece f002 e f999 ao mesmo tempo. Sem CDI: {@code F999Module} publica
 * esta implementação na porta {@link DeletionQueue} do {@link Context} (logo depois do
 * {@code DeletionQueueService}, resolvido aqui em campo), e quem a consome é
 * {@code f002.WriteUseCase}, também Context-wired.
 */
@NullMarked
public class DeletionQueueAdapter implements DeletionQueue {

    private final DeletionQueueService service = Context.get(DeletionQueueService.class);

    @Override
    public void enqueueAccountDeleted(UUID accountId, UUID personId) {
        service.enqueue(DeletionQueueService.TYPE_ACCOUNT_DELETED, accountId, personId);
    }

    @Override
    public void enqueueTransactionDeleted(UUID transactionId, UUID personId) {
        service.enqueue(DeletionQueueService.TYPE_TRANSACTION_DELETED, transactionId, personId);
    }
}
