/**
 * Lançamentos e transferências (créditos, débitos, parcelas, transferência entre contas):
 * {@code /api/{uuid}/accounts/transactions} + {@code /api/{uuid}/accounts/{accId}/transactions}.
 * Filtros por data/status/tipo, patch de status, delete unitário/em grupo/futuro.
 *
 * <p>Publica {@code TransactionsDeleted} (evento) após excluir transações — reagido best-effort
 * pelo listener de overlay da própria fatia (assinado em {@code F006Module.initialize()}, limpa
 * {@code F006_TRANSACTION_CATEGORY}/{@code F006_TRANSACTION_TAG}) e por {@code f004} (tags, purga
 * vínculos); cobre também as exclusões em cascata disparadas por outras fatias (conta/tag/
 * categoria), que nunca tocam essas tabelas direto. O mesmo listener reage a {@code TransactionImported}
 * (publicado por {@code f007}, a importação de extrato/fatura — fatia própria desde
 * {@code .claude/plan.md}) e a {@code CategoryReassigned}.
 */
@NullMarked
package br.cdb.feature.f006;

import org.jspecify.annotations.NullMarked;
