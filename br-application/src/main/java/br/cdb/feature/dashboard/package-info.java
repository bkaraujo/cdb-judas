/**
 * Painel financeiro mensal consolidado do usuário.
 *
 * <p>Rota:
 * <pre>GET /api/{uuid}/dashboard/result?month={m}&amp;year={yyyy}</pre>
 *
 * <p>Retorna o resultado mensal ({@code MonthlyResult}) com agregações de receitas,
 * despesas e saldo líquido calculadas pelo {@code DashboardService} a partir de
 * todos os lançamentos do período.
 */
@NullMarked
package br.cdb.feature.dashboard;

import org.jspecify.annotations.NullMarked;
