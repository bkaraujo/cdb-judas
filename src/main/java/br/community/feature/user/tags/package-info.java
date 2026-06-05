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
 * Mutações são propagadas ao frontend via SSE pelo
 * {@link br.community.feature.user.tags.core.TagStreamListener}.
 *
 * <p>Subpacote:
 * <ul>
 *   <li>{@link br.community.feature.user.tags.core} — entidade, request e listener de stream</li>
 * </ul>
 */
package br.community.feature.user.tags;
