/**
 * Painel financeiro mensal consolidado do usuário ({@code GET /api/{uuid}/dashboard/result}).
 * Sem overlay/repositório próprio: agrega as transações de {@code f006} lidas por HTTP real
 * ({@code f000.InternalApi}).
 *
 * <p><b>Sem {@code *UseCase} de fronteira e sem {@code WriteUseCase}</b>: a fatia é somente-leitura,
 * então o par CQRS de f001–f006 aparece aqui só com o lado de leitura —
 * {@code _1_application.usecase.ReadUseCase} (era {@code DashboardService}), Context-wired e
 * resolvido pelo {@code DashboardResource} no {@code Context}. Um {@code WriteUseCase} vazio seria
 * código morto; ele nasce no dia em que o dashboard ganhar estado próprio (meta, orçamento).
 */
@NullMarked
package br.cdb.feature.f009;

import org.jspecify.annotations.NullMarked;
