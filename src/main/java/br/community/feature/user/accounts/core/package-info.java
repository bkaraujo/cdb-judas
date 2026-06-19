/**
 * Modelo de domínio e listeners de conta para a feature {@code accounts}.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code AccountResponse} — DTO de resposta que projeta {@code Account} + saldo calculado</li>
 *   <li>{@code AccountRequest} — payload de criação/atualização de conta</li>
 *   <li>{@code AccountStreamListener} — ouve eventos SSE do domínio e propaga atualizações ao frontend</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.accounts.core;

import org.jspecify.annotations.NullMarked;
