/**
 * Cartões de crédito ({@code /api/{uuid}/accounts/{accountId}/cards}). Extraída de {@code f002}
 * (era passthrough sem modelo/repositório próprio, fundida lá; cresceu até justificar fatia
 * própria — ver .claude/refactor.md). Só {@code CardUseCase} sobre a engine
 * {@code CreditCardUseCase} (Registry-wired, ex-contexto monetário — ver {@code fNNN._1_application.usecase}).
 * {@code f002.AccountResponse} mantém uma projeção
 * somente-leitura (record {@code Card}) do mesmo shape — f003 é dono das mutações.
 */
@NullMarked
package br.cdb.feature.f003;

import org.jspecify.annotations.NullMarked;
