/**
 * Accounts, com balance fundido aqui: fatia somente-leitura/passthrough do contexto monetário, sem
 * modelo ou repositório próprio, então não justifica um hexágono fNNN à parte. Cards teve o mesmo
 * tratamento no início, mas cresceu até justificar fatia própria — ver f003 (.claude/refactor.md).
 * {@code AccountResponse} mantém {@code cards[]} embutido via uma projeção somente-leitura própria
 * ({@code AccountResponse.Card}, lendo {@code MonetaryUseCases.ucCreditCard()} direto do contexto);
 * a mutação de cartão é de f003.
 */
@NullMarked
package br.cdb.feature.f002;

import org.jspecify.annotations.NullMarked;
