package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f002._0_domain.TransactionAccountOverlay;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Único ponto do código que conhece f002 e f006 ao mesmo tempo — resolvido por CDI sem
 * {@code @Produces}/{@code Registry}, já que {@link TransactionAccountOverlay} tem só esta
 * implementação no classpath.
 *
 * <p>Ambos os métodos viraram no-op na fusão de contextos (fase 1 de {@code .claude/plan.md}):
 * {@code COD_ACCOUNT}/{@code COD_PERSON} agora são colunas nativas de {@code F006_TRANSACTION}
 * (não mais um overlay redundante em {@code PERSON_TRANSACTION}) — o próprio contexto monetário já
 * reatribui a conta ao mover transações (ver {@code AccountUseCase.deleteMove}), e a exclusão da
 * conta já apaga a linha da transação por inteiro, sem precisar de uma purga separada aqui.</p>
 */
@NullMarked
@Singleton
public class TransactionAccountOverlayAdapter implements TransactionAccountOverlay {

    @Override
    public void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId) {
        // no-op — ver Javadoc da classe.
    }

    @Override
    public void deleteByAccountAndPerson(UUID accountId, UUID personId) {
        // no-op — ver Javadoc da classe.
    }
}
