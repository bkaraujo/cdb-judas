/**
 * Modelo de domínio e listeners de categoria.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code Category}               — DTO de resposta de categoria</li>
 *   <li>{@code CreateRequest}           — payload de criação</li>
 *   <li>{@code UpdateRequest}           — payload de atualização parcial</li>
 *   <li>{@code CategoryStreamListener} — ouve eventos do domínio e envia updates via SSE</li>
 * </ul>
 */
package br.community.feature.user.categories.core;
