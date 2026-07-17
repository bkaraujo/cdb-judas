/**
 * Confirmação da importação: comandos e payloads do segundo passo (após o preview) e a resposta comum
 * aos dois fluxos.
 *
 * <p>A confirmação recebe as linhas que o usuário manteve e persiste os lançamentos. O servidor
 * re-deriva o destino de cada linha (inserir / reconciliar / pular) contra as transações atuais, de
 * modo que a importação é idempotente e independe do preview.
 *
 * <ul>
 *   <li>{@code BankStatementConfirmRequest} / {@code BankStatementConfirmCommand} — payload e comando do
 *       fluxo de extrato bancário (conta de destino + linhas; {@code amount} com sinal)</li>
 *   <li>{@code ImportConfirmResponse} — resultado comum: {@code created} / {@code reconciled} /
 *       {@code skipped}</li>
 * </ul>
 *
 * <p>O fluxo de fatura de cartão reusa o {@code ImportConfirm} do contexto monetário; o request
 * unificado de entrada ({@code StatementConfirmRequest}, discriminado por {@code type}) vive no pacote
 * {@code importer}.
 */
@NullMarked
package br.cdb.feature.user.accounts.transactions.importer.confirm;

import org.jspecify.annotations.NullMarked;
