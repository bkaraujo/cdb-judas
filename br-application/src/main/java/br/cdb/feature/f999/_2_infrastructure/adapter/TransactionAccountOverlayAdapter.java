package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f002._0_domain.TransactionAccountOverlay;
import br.cdb.feature.f006._1_application.UserTransactionService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Único ponto do código que conhece f002 e f006 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link TransactionAccountOverlay} tem só esta
 * implementação no classpath.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionAccountOverlayAdapter implements TransactionAccountOverlay {

    private final UserTransactionService userTransactionService;

    @Override
    public void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId) {
        userTransactionService.reassignAccount(oldAccountId, newAccountId, personId);
    }

    @Override
    public void deleteByAccountAndPerson(UUID accountId, UUID personId) {
        userTransactionService.deleteByAccountAndPerson(accountId, personId);
    }
}
