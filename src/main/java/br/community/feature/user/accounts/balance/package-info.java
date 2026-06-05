/**
 * Consulta de saldo de conta por período.
 *
 * <p>Rota:
 * <pre>GET /api/{uuid}/accounts/{id}/balance?period={yyyyMM}|year={yyyy}</pre>
 *
 * <p>Dois modos de consulta:
 * <ul>
 *   <li>{@code ?period=yyyyMM} — saldo mensal de uma única competência</li>
 *   <li>{@code ?year=yyyy}     — lista de saldos para todos os meses do ano</li>
 * </ul>
 *
 * <p>Exatamente um dos parâmetros deve ser informado; caso contrário a requisição
 * é rejeitada com erro de validação.
 */
@NullMarked
package br.community.feature.user.accounts.balance;

import org.jspecify.annotations.NullMarked;
