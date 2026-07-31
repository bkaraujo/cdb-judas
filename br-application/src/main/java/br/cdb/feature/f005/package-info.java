/**
 * Categorias macro/subcategoria de transações ({@code /api/{uuid}/categories}); mudanças
 * propagadas via SSE. Exclusão segue o contrato uniforme (MOVE reatribui a subárvore, DELETE
 * apaga as transações vinculadas — publica {@code TransactionsDeleted}, evento da base f000).
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: a fatia é um par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (toda leitura, incl. o endpoint interno
 * {@code /categories/transfer} lido por f006 via {@code InternalApi}) e {@code WriteUseCase} (toda
 * mutação, incl. a cascata de exclusão e a leitura cross-slice das transações vinculadas). O
 * {@code CategoryResource} resolve os dois direto no {@code Context}, como em f002/f003/f004/f006;
 * {@code UserCategoryService} mudou-se para {@code _1_application.service} e deixou de ser bean CDI
 * na mesma mudança. Os nomes simples coincidem com os pares das outras fatias — quem precisa de mais
 * de um usa o nome completo.
 *
 * <p>Categoria não usa {@code UserGuards}: o escopo por pessoa está na própria query
 * ({@code F005_CATEGORY.COD_PERSON}). A exceção é a categoria de sistema "Transferência", global e
 * compartilhada por todas as pessoas.
 */
@NullMarked
package br.cdb.feature.f005;

import org.jspecify.annotations.NullMarked;
