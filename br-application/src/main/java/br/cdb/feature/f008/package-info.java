/**
 * Tags — classificação livre/transversal de transações ({@code /api/{uuid}/tags}); mudanças
 * propagadas via SSE. {@code TagTransactionListener} reage a {@code TransactionsDeleted} (f005)
 * purgando vínculos {@code PERSON_TRANSACTION_TAG} das transações apagadas, best-effort.
 */
@NullMarked
package br.cdb.feature.f008;

import org.jspecify.annotations.NullMarked;
