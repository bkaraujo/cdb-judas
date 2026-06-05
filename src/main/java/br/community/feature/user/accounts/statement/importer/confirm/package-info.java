/**
 * Comandos e resultado da etapa de confirmação da importação.
 *
 * <p>Contém:
 * <ul>
 *   <li>{@code BankStatementConfirmCommand} — comando para confirmar linhas de extrato bancário,
 *       carregando {@code accountId} e a lista de linhas selecionadas</li>
 *   <li>{@code ImportResult} — resultado da operação: contadores de registros criados,
 *       reconciliados e ignorados</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.accounts.statement.importer.confirm;

import org.jspecify.annotations.NullMarked;
