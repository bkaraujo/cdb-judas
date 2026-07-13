/**
 * Rótulos livres (tags) para organização pessoal de transações.
 *
 * <p>Rotas ({@code /api/{uuid}/tags}):
 * <pre>
 * GET    /tags       — lista todas as tags
 * GET    /tags/{id}  — detalhe de uma tag
 * POST   /tags       — cria tag
 * PATCH  /tags/{id}  — atualiza tag
 * DELETE /tags/{id}  — remove tag
 * </pre>
 *
 * <p>Tags complementam as categorias como forma de marcação livre e transversal.
 * Mutações são propagadas ao frontend via SSE diretamente pelo {@code UserTagService}.
 *
 * <p>Subpacote:
 * <ul>
 *   <li>{@link br.cdb.feature.user.tags.core} — entidade e request</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.user.tags;

import org.jspecify.annotations.NullMarked;
