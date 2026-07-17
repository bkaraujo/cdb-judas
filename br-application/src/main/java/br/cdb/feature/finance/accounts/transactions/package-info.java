/**
 * Lançamentos financeiros (créditos, débitos, transferências e parcelamentos).
 *
 * <p>Rotas ({@code /api/{uuid}/accounts}):
 * <pre>
 * GET    /transactions                          — lista todos os lançamentos (filtros: limit, dateFrom, dateTo, status, type)
 * POST   /transactions/transfer                 — cria uma transferência entre contas
 * GET    /{accId}/transactions                  — lista lançamentos de uma conta específica
 * POST   /{accId}/transactions                  — cria lançamento em uma conta
 * PATCH  /{accId}/transactions/{txId}           — atualiza lançamento
 * PATCH  /{accId}/transactions/{txId}/status    — altera apenas o status e data de pagamento
 * DELETE /{accId}/transactions/{txId}           — remove lançamento (suporte a mode de exclusão)
 * </pre>
 *
 * <p>O campo {@code installments} no request dispara a criação automática de parcelas
 * pelo domínio. O parâmetro {@code mode} no DELETE controla se a exclusão é unitária
 * ou em lote (todas as parcelas do grupo).
 */
@NullMarked
package br.cdb.feature.finance.accounts.transactions;

import org.jspecify.annotations.NullMarked;
