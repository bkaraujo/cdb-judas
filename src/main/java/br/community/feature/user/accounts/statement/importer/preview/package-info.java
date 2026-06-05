/**
 * Abstrações de análise (parsing) e pré-processamento de extratos PDF.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Detectar o tipo de documento ({@code DocumentTypeDetector}) e o emissor ({@code IssuerDetector})</li>
 *   <li>Fazer o parse do PDF em linhas estruturadas via {@code CreditCardStatementParserRegistry}
 *       e {@code BankStatementParserRegistry}</li>
 *   <li>Expandir parcelas em lançamentos individuais ({@code InstallmentExpander})</li>
 *   <li>Sugerir categoria automaticamente com base em palavras-chave ({@code CategoryGuesser})</li>
 *   <li>Identificar o cartão associado à fatura ({@code CardMatcher})</li>
 * </ul>
 *
 * <p>Os resultados são encapsulados em {@code ImportPreviewOutcome}, discriminado entre
 * {@code Invoice} (fatura de cartão) e {@code Statement} (extrato bancário).
 */
package br.community.feature.user.accounts.statement.importer.preview;
