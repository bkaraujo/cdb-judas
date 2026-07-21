/**
 * Rótulos livres (tags) para organização pessoal de transações.
 *
 * <p>Rotas ({@code /api/{uuid}/tags}):
 * <pre>
 * GET    /tags       — lista todas as tags
 * GET    /tags/{personId}  — detalhe de uma tag
 * POST   /tags       — cria tag
 * PATCH  /tags/{personId}  — atualiza tag
 * DELETE /tags/{personId}  — remove tag
 * </pre>
 *
 * <p>Tags complementam as categorias como forma de marcação livre e transversal.
 * Mutações são propagadas ao frontend via SSE diretamente pelo {@code UserTagService}.
 */
@NullMarked
package br.cdb.feature.finance.tags;

import org.jspecify.annotations.NullMarked;
