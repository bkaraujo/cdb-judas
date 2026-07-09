/**
 * Modelo de domínio e publicação SSE de conta para a feature {@code accounts}.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code AccountResponse} — DTO de resposta que projeta {@code Account} + saldo calculado</li>
 *   <li>{@code AccountRequest} — payload de criação/atualização de conta</li>
 *   <li>{@code AccountStreamPublisher} — despachado pelos Resources após a mutação (conta, cartão,
 *       transação, transferência, importação) para propagar a atualização ao frontend via SSE</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.accounts.core;

import org.jspecify.annotations.NullMarked;
