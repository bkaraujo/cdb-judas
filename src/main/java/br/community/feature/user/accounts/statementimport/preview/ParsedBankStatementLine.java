package br.community.feature.user.accounts.statementimport.preview;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One kept movement from a bank statement (extrato de conta corrente), as printed by the bank.
 *
 * <p>Unlike a credit-card charge, the date is absolute ({@link LocalDate} — the bank prints the full
 * {@code dd/MM/yyyy}) and {@code amount} is <b>signed</b>: negative for debits, positive for credits.
 * The persistence {@code type} (expense/income) is derived from the sign by the import orchestration.
 * Balance lines ({@code Saldo Diário}/{@code Saldo final}) and the aggregate card-invoice payment
 * ({@code Pagamento de fatura do cartão}) are dropped by the parser and never reach here.
 */
@NullMarked
public record ParsedBankStatementLine(
        LocalDate date,
        String description,
        BigDecimal amount
) {}
