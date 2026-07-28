/**
 * Tags — classificação livre/transversal de transações ({@code /api/{uuid}/tags}); mudanças
 * propagadas via SSE. {@code TagTransactionListener} reage a {@code TransactionsDeleted} (evento da base f000)
 * purgando vínculos {@code PERSON_TRANSACTION_TAG} das transações apagadas, best-effort.
 */
@NullMarked
package br.cdb.feature.f004;

import org.jspecify.annotations.NullMarked;
