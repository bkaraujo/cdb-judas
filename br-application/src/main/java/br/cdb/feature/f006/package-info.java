/**
 * Lançamentos e transferências (créditos, débitos, parcelas, transferência entre contas):
 * {@code /api/{uuid}/accounts/transactions} + {@code /api/{uuid}/accounts/{accId}/transactions}.
 * Filtros por data/status/tipo, patch de status, delete unitário/em grupo/futuro.
 *
 * <p>Publica {@code TransactionsDeleted} (evento) após excluir transações — reagido best-effort
 * pelo próprio {@code TransactionOverlayListener} (limpa o overlay {@code PERSON_TRANSACTION}) e
 * por {@code f004} (tags, purga vínculos); redundante com o {@code ON DELETE CASCADE} de
 * {@code MON_TRANSACTION}, mas cobre exclusões em cascata disparadas por outras fatias (tags/
 * categorias) que chamam o contexto direto sem passar pelo use case desta.
 *
 * <p>Fundida aqui na fase 6 de {@code .claude/plan.md}: a antiga fatia {@code f007} (importação de
 * extrato/fatura, preview→confirm, parsers BTG e Santander) — {@code /api/{uuid}/accounts/transactions
 * /import/preview} + {@code .../import/confirm}. Consolida o que antes era {@code finance.accounts
 * .statement} (parsing de PDF) e {@code finance.accounts.transactions.importer} (orquestração,
 * casamento de cartão, sugestão de categoria, expansão de parcelas) — path preservado, só o pacote
 * mudou (número da fatia não é mais parte da rota).
 */
@NullMarked
package br.cdb.feature.f006;

import org.jspecify.annotations.NullMarked;
