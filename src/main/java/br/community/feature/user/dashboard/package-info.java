/**
 * Painel financeiro mensal consolidado do usuário.
 *
 * <p>Rota:
 * <pre>GET /api/{uuid}/dashboard/result?month={m}&amp;year={yyyy}</pre>
 *
 * <p>Retorna o resultado mensal ({@code MonthlyResult}) com agregações de receitas,
 * despesas e saldo líquido calculadas pelo {@code DashboardService} a partir de
 * todos os lançamentos do período.
 *
 * <p>Subpacote:
 * <ul>
 *   <li>{@link br.community.feature.user.dashboard.core} — serviço de cálculo do resultado mensal</li>
 * </ul>
 */
package br.community.feature.user.dashboard;
