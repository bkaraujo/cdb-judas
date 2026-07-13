/**
 * Contrato uniforme de exclusão para entidades com transações vinculadas (conta, cartão,
 * categoria, tag): 409 com {@code code=LINKED_TRANSACTIONS} + contagem quando a exclusão simples
 * não é possível; o cliente escolhe {@code ?strategy=MOVE|DELETE} (+ {@code targetId} para MOVE) —
 * tag ganha ainda {@code DETACH}, que apenas desvincula sem tocar nas transações.
 *
 * <p>{@link br.cdb.feature.user.deletion.Deletions} concentra o parsing da estratégia e a
 * construção do 409 rico, evitando duplicação entre os 4 recursos que implementam o contrato
 * (accounts, accounts/{id}/cards, categories, tags).
 */
@NullMarked
package br.cdb.feature.user.deletion;

import org.jspecify.annotations.NullMarked;
