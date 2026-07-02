/**
 * Gestão de contas bancárias e cartões de crédito do usuário.
 *
 * <p>Rotas principais ({@code /api/{uuid}/accounts}):
 * <pre>
 * GET    /accounts          — lista todas as contas (com limites e cartões embutidos)
 * GET    /accounts/{id}     — detalhe de uma conta
 * POST   /accounts          — cria conta (aceita limite de crédito/cheque especial e ciclo de fatura)
 * PATCH  /accounts/{id}     — atualiza conta
 * DELETE /accounts/{id}     — remove conta
 * </pre>
 *
 * <p>Sub-recursos organizados em subpacotes:
 * <ul>
 *   <li>{@link br.community.feature.user.accounts.balance}      — saldo mensal/anual por conta</li>
 *   <li>{@link br.community.feature.user.accounts.cards}        — CRUD de cartões (entidade do contexto monetário, identificada só pelo last4)</li>
 *   <li>{@link br.community.feature.user.accounts.closing}      — período de fechamento de fatura</li>
 *   <li>{@link br.community.feature.user.accounts.statement}    — parsing de extratos/faturas PDF (suporte à importação, sem rota própria)</li>
 *   <li>{@link br.community.feature.user.accounts.transactions} — lançamentos financeiros (inclui importação de extrato/fatura)</li>
 * </ul>
 *
 * <p>A feature delega toda a lógica de domínio ao {@code MonetaryContext}
 * e escuta eventos via {@link br.community.feature.user.accounts.core.AccountStreamListener}.
 */
@NullMarked
package br.community.feature.user.accounts;

import org.jspecify.annotations.NullMarked;
