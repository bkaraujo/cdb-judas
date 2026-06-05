/**
 * Gestão de contas bancárias e cartões de crédito do usuário.
 *
 * <p>Rotas principais ({@code /api/{uuid}/accounts}):
 * <pre>
 * GET    /accounts          — lista todas as contas (filtro opcional: ?type=card)
 * GET    /accounts/{id}     — detalhe de uma conta
 * POST   /accounts          — cria conta
 * PATCH  /accounts/{id}     — atualiza conta
 * DELETE /accounts/{id}     — remove conta
 * </pre>
 *
 * <p>Sub-recursos organizados em subpacotes:
 * <ul>
 *   <li>{@link br.community.feature.user.accounts.balance}      — saldo mensal/anual por conta</li>
 *   <li>{@link br.community.feature.user.accounts.closing}      — período de fechamento de fatura</li>
 *   <li>{@link br.community.feature.user.accounts.statement}    — extrato mensal por conta</li>
 *   <li>{@link br.community.feature.user.accounts.transactions} — lançamentos financeiros</li>
 * </ul>
 *
 * <p>A feature delega toda a lógica de domínio ao {@code MonetaryContext}
 * e escuta eventos via {@link br.community.feature.user.accounts.core.AccountStreamListener}.
 */
package br.community.feature.user.accounts;
