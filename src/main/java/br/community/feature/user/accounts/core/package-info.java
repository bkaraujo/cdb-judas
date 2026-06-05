/**
 * Modelo de domínio e listeners de conta para a feature {@code accounts}.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code Account} — DTO de resposta que projeta {@code MonetaryAccount} + saldo calculado</li>
 *   <li>{@code AccountRequest} — payload de criação/atualização de conta</li>
 *   <li>{@code AccountStreamListener} — ouve eventos SSE do domínio e propaga atualizações ao frontend</li>
 * </ul>
 */
package br.community.feature.user.accounts.core;
