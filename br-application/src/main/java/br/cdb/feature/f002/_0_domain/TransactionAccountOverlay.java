package br.cdb.feature.f002._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Porta que a fatia de contas ({@code f002}) exige do overlay de transações ({@code
 * PERSON_TRANSACTION}) para manter a integridade referencial ao apagar/reatribuir uma conta —
 * {@code PERSON_TRANSACTION.COD_ACCOUNT} referencia {@code MON_ACCOUNT}, então o overlay precisa
 * ser re-keyed (MOVE) ou apagado <em>antes</em> do contexto remover a conta, de forma síncrona (não
 * dá para virar reação a evento pós-delete). Implementada por um adapter em
 * {@code f999._2_infrastructure.adapter}, que delega à fatia dona do overlay (f005) — nem f002 nem
 * f005 dependem uma da outra.
 */
@NullMarked
public interface TransactionAccountOverlay {

    /** Re-keya o overlay das transações da conta de origem para a de destino (estratégia MOVE). */
    void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId);

    /** Apaga o overlay das transações da conta da pessoa (estratégias BLOCK/PURGE). */
    void deleteByAccountAndPerson(UUID accountId, UUID personId);
}
