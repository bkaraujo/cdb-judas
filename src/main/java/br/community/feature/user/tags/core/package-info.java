/**
 * Modelo de domínio e listener de tag.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code Tag}               — DTO de resposta de tag</li>
 *   <li>{@code TagRequest}        — payload de criação/atualização</li>
 *   <li>{@code TagStreamListener} — ouve eventos do domínio e envia updates via SSE</li>
 * </ul>
 */
package br.community.feature.user.tags.core;
