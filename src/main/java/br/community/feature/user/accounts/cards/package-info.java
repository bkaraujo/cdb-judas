/**
 * CRUD de cartões de crédito (entidade do contexto monetário, identificada só pelo last4).
 *
 * <p>Rota ({@code /api/{uuid}/accounts/{accountId}/cards}):
 * <pre>
 * GET    /cards          — lista os cartões da conta
 * POST   /cards          — cria cartão vinculado à conta
 * DELETE /cards/{cardId} — remove o cartão
 * </pre>
 *
 * <p>Cartão sempre pertence a uma conta real; todos os cartões de uma conta compartilham o limite
 * de crédito e o ciclo de fatura dela (ver {@code AccountRequest}/{@code AccountResponse}).
 */
@NullMarked
package br.community.feature.user.accounts.cards;

import org.jspecify.annotations.NullMarked;
