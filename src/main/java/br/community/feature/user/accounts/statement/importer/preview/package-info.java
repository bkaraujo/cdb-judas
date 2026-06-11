/**
 * Abstrações de análise (parsing) e pré-processamento de extratos PDF.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Fazer o parse do PDF em linhas estruturadas: os {@code StatementParser} (em {@code provider})
 *       reconhecem o documento via {@code parseable} e produzem um {@code MonetaryDocument}</li>
 *   <li>Expandir parcelas em lançamentos individuais ({@code InstallmentExpander})</li>
 *   <li>Sugerir categoria automaticamente com base em palavras-chave ({@code CategoryGuesser})</li>
 *   <li>Identificar o cartão associado à fatura ({@code CardMatcher})</li>
 * </ul>
 *
 * <p>O {@code MonetaryDocument} (discriminado entre {@code Invoice} e {@code Statement}) é
 * processado pelo use case e encapsulado em {@code ImportPreviewOutcome}, discriminado entre
 * {@code Invoice} (fatura de cartão) e {@code Statement} (extrato bancário).
 */
@NullMarked
package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;
