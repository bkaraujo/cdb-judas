/**
 * Parsers específicos por banco/emissor de extratos PDF.
 *
 * <p>Implementações disponíveis:
 * <ul>
 *   <li>{@code BtgBankStatementParser}              — extrato bancário BTG</li>
 *   <li>{@code BtgCreditCardStatementParser}         — fatura do cartão de crédito BTG</li>
 *   <li>{@code SantanderCreditCardStatementParser}   — fatura do cartão de crédito Santander</li>
 * </ul>
 *
 * <p>Cada parser implementa a interface {@code BankStatementParser} ou
 * {@code CreditCardStatementParser} definida em {@code preview} e é registrado
 * automaticamente nos respectivos registries pelo mecanismo de injeção do Spring.
 */
@NullMarked
package br.community.feature.user.accounts.statement.importer.provider;

import org.jspecify.annotations.NullMarked;
