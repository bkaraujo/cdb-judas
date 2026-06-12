/**
 * Parsers específicos por banco/emissor de extratos PDF.
 *
 * <p>Implementações disponíveis:
 * <ul>
 *   <li>{@code BTGStatementParser}              — extrato bancário BTG</li>
 *   <li>{@code SantanderStatementParser}        — extrato bancário Santander</li>
 *   <li>{@code BTGInvoiceParser}         — fatura do cartão de crédito BTG</li>
 *   <li>{@code SantanderInvoiceParser}   — fatura do cartão de crédito Santander</li>
 * </ul>
 *
 * <p>Cada parser implementa {@code StatementParser} e é autocontido: seu {@code parseable} reconhece
 * o próprio documento (emissor + tipo), compondo os auxiliares {@code DocumentText}/{@code BankStatements}
 * para não duplicar a lógica de marcadores e CNPJ. O use case itera a coleção e usa o único capaz.
 */
@NullMarked
package br.community.feature.user.accounts.transactions.importer.provider;

import org.jspecify.annotations.NullMarked;
