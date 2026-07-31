/**
 * Accounts, com balance e o período de fechamento fundidos aqui. Cards teve o mesmo tratamento no
 * início, mas cresceu até justificar fatia própria — ver f003 (.claude/refactor.md).
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: a fatia é um par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (toda leitura, incl. a guarda de propriedade da entrada
 * HTTP e a composição da {@code AccountView}) e {@code WriteUseCase} (toda mutação, incl. política de
 * usuário, publicação de SSE e cascata de exclusão). Os {@code *Resource} resolvem os dois direto no
 * {@code Context} ({@code Context.tryGet(...)}), como em f006.
 *
 * <p>{@code AccountResponse} mantém {@code cards[]} embutido via uma projeção somente-leitura própria
 * ({@code AccountResponse.Card}, lendo a engine {@code CreditCardUseCase} de f003 direto — Context-wired,
 * ex-contexto monetário); a mutação de cartão é de f003.
 */
@NullMarked
package br.cdb.feature.f002;

import org.jspecify.annotations.NullMarked;
