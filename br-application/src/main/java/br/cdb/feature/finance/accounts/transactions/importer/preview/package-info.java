/**
 * Projeção do preview da importação: as estruturas que o use case devolve após rotear o documento por
 * tipo e os DTOs que a borda HTTP serializa.
 *
 * <p>O parsing do PDF vive em {@link br.cdb.feature.finance.accounts.statement}; o casamento de
 * cartão/categoria e a expansão de parcelas, em {@code transactions.importer}. Aqui ficam apenas as
 * visões prontas para confirmação.
 *
 * <ul>
 *   <li>{@code ImportPreviewOutcome} — resultado selado, discriminado entre {@code Invoice}
 *       ({@code ImportPreview}) e {@code Statement} ({@code BankStatementPreview})</li>
 *   <li>{@code ImportPreview} / {@code PreviewRow} — preview de fatura: cartões candidatos, last4s e
 *       linhas expandidas (uma por compra à-vista, {@code N} por parcelada) com flag de duplicata e
 *       cartão sugerido por linha</li>
 *   <li>{@code BankStatementPreview} / {@code BankStatementPreviewRow} — preview de extrato: contas de
 *       destino candidatas, conta selecionada e movimentos com estado (novo/duplicado/reconciliável)</li>
 *   <li>{@code ImportPreviewResponse} / {@code BankStatementPreviewResponse} — DTOs de resposta HTTP,
 *       marcados com o tipo de documento</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.finance.accounts.transactions.importer.preview;

import org.jspecify.annotations.NullMarked;
