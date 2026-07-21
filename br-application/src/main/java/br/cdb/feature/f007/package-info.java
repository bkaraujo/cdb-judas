/**
 * Categorias macro/subcategoria de transações ({@code /api/{uuid}/categories}); mudanças
 * propagadas via SSE. Exclusão segue o contrato uniforme (MOVE reatribui a subárvore, DELETE
 * apaga as transações vinculadas via facade — publica {@code TransactionsDeleted}, f005).
 */
@NullMarked
package br.cdb.feature.f007;

import org.jspecify.annotations.NullMarked;
