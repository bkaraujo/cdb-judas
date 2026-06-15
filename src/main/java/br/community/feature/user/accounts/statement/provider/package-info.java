/**
 * Parsers específicos por banco/emissor de extratos e faturas em PDF.
 *
 * <p>Implementações disponíveis:
 * <ul>
 *   <li>{@code BTGStatementParser}       — extrato bancário BTG</li>
 *   <li>{@code SantanderStatementParser} — extrato bancário Santander</li>
 *   <li>{@code BTGInvoiceParser}         — fatura do cartão de crédito BTG</li>
 *   <li>{@code SantanderInvoiceParser}   — fatura do cartão de crédito Santander</li>
 * </ul>
 *
 * <p>Cada parser implementa {@link br.community.feature.user.accounts.statement.StatementParser} e é
 * autocontido: seu {@code parseable} reconhece o próprio documento (emissor + tipo), compondo os
 * auxiliares {@code DocumentText} (marcadores/CNPJ do texto) e {@code BankStatements} (assinatura de
 * extrato, negada pelos parsers de fatura para que um extrato nunca seja confundido com uma fatura),
 * sem duplicar a lógica de marcadores. O use case itera a coleção e usa o único capaz.
 */
@NullMarked
package br.community.feature.user.accounts.statement.provider;

import org.jspecify.annotations.NullMarked;
