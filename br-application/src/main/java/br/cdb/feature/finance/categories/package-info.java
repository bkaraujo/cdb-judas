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
 * Alterações são propagadas ao frontend via SSE diretamente pelo {@code UserCategoryService}
 * (sem indireção por evento de domínio, já que {@code UserCategory} é uma entidade da feature,
 * não um modelo do contexto {@code monetary}).
 */
@NullMarked
package br.cdb.feature.finance.categories;

import org.jspecify.annotations.NullMarked;
