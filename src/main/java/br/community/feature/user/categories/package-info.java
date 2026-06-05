/**
 * Gestão de categorias de transações do usuário.
 *
 * <p>Rotas ({@code /api/{uuid}/categories}):
 * <pre>
 * GET    /categories       — lista todas as categorias
 * GET    /categories/{id}  — detalhe de uma categoria
 * POST   /categories       — cria categoria
 * PATCH  /categories/{id}  — atualiza categoria
 * DELETE /categories/{id}  — remove categoria
 * </pre>
 *
 * <p>Categorias são usadas para classificar transações e gerar agregações no dashboard.
 * Alterações são propagadas ao frontend via SSE através do
 * {@link br.community.feature.user.categories.core.CategoryStreamListener}.
 *
 * <p>Subpacote:
 * <ul>
 *   <li>{@link br.community.feature.user.categories.core} — entidade, requests e listener de stream</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.categories;

import org.jspecify.annotations.NullMarked;
