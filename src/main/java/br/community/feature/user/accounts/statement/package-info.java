/**
 * Extrato financeiro mensal por conta.
 *
 * <p>Rotas ({@code /api/{uuid}/accounts}):
 * <pre>
 * GET /accounts/{accountId}/statements/{yyyyMM}  — itens do extrato de uma conta no mês
 * GET /accounts/statements/{yyyyMM}              — resumo consolidado de todas as contas no mês
 * </pre>
 *
 * <p>Ambas as rotas aceitam o parâmetro opcional {@code ?status=} para filtrar
 * lançamentos por situação (ex.: {@code PAID}, {@code PENDING}).
 *
 * <p>Subpacote:
 * <ul>
 *   <li>{@link br.community.feature.user.accounts.statement.importer}
 *       — importação de extratos PDF (fatura de cartão e extrato bancário)</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.accounts.statement;

import org.jspecify.annotations.NullMarked;
