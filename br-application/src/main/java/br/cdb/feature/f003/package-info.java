/**
 * Cartões de crédito ({@code /api/{uuid}/accounts/{accountId}/cards}). Extraída de {@code f002}
 * (era passthrough sem modelo/repositório próprio, fundida lá; cresceu até justificar fatia
 * própria — ver .claude/refactor.md). Sem {@code _0_domain}/módulo CDI próprio, mesmo precedente
 * de {@code f009}: sem tabela/porta própria, só {@code CardUseCase} sobre
 * {@code MonetaryUseCases.ucCreditCard()}. {@code f002.AccountResponse} mantém uma projeção
 * somente-leitura (record {@code Card}) do mesmo shape — f003 é dono das mutações.
 */
@NullMarked
package br.cdb.feature.f003;

import org.jspecify.annotations.NullMarked;
