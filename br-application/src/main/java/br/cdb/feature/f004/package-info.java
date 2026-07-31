/**
 * Tags — classificação livre/transversal de transações ({@code /api/{uuid}/tags}); mudanças
 * propagadas via SSE. {@code TagTransactionListener} reage a {@code TransactionsDeleted} (evento da base f000)
 * purgando vínculos {@code PERSON_TRANSACTION_TAG} das transações apagadas, best-effort.
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: a fatia é um par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (toda leitura) e {@code WriteUseCase} (toda mutação,
 * incl. a publicação de {@code TagEvents}). O {@code TagResource} resolve os dois direto no
 * {@code Context}, como em f002/f003/f006; os serviços da fatia deixaram de ser beans CDI na mesma
 * mudança. Os nomes simples coincidem com os pares de f002/f003 — quem precisa de mais de um usa o
 * nome completo.
 *
 * <p>Tag não usa {@code UserGuards}: o escopo por pessoa está na própria query
 * ({@code F004_TAG.COD_PERSON}), com o {@code personId} vindo do {@code /api/{uuid}/…} já validado
 * pelo {@code OwnershipFilter}.
 */
@NullMarked
package br.cdb.feature.f004;

import org.jspecify.annotations.NullMarked;
