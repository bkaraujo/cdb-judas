package br.community.context.monetary;

import br.commons.Result;
import br.commons.annotation.Facade;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.*;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MonetaryContext implements Facade {

    private final AccountUseCase ucAccount;
    private final TransactionUseCase ucTransaction;
    private final MetadataUseCase ucMetadata;

    // ── Account operations ─────────────────────────────────────────

    public Result<List<Account>, DomainError> listAccounts() {
        return ucAccount.listAccounts();
    }

    public Result<Account, DomainError> findAccount(UUID id) {
        return ucAccount.findAccount(id);
    }

    public Result<Account, DomainError> createAccount(AccountCommand cmd) {
        return ucAccount.createAccount(cmd);
    }

    public Result<Account, DomainError> updateAccount(UUID id, AccountCommand cmd) {
        return ucAccount.updateAccount(id, cmd);
    }

    public Result<Void, DomainError> deleteAccount(UUID id) {
        return ucAccount.deleteAccount(id);
    }

    // ── Balance operations ─────────────────────────────────────────

    public Result<MonthlyBalance, DomainError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return ucAccount.getMonthlyBalance(accountId, period);
    }

    public Result<List<MonthlyBalance>, DomainError> getYearBalances(UUID accountId, int year) {
        return ucAccount.getYearBalances(accountId, year);
    }

    // ── Credit card operations ─────────────────────────────────────

    public Result<List<Account>, DomainError> listCreditCards() {
        return ucAccount.listCreditCards();
    }

    public Result<List<Account>, DomainError> listCreditCardsByAccount(UUID accountId) {
        return ucAccount.listCreditCardsByAccount(accountId);
    }

    public Result<Account, DomainError> createCreditCard(CreditCardCommand cmd) {
        return ucAccount.createCreditCard(cmd);
    }

    public Result<Account, DomainError> updateCreditCard(UUID id, CreditCardCommand cmd) {
        return ucAccount.updateCreditCard(id, cmd);
    }

    public Result<Void, DomainError> deleteCreditCard(UUID id) {
        return ucAccount.deleteCreditCard(id);
    }

    // ── Transaction operations ─────────────────────────────────────

    public Result<List<Transaction>, DomainError> listTransactions() {
        return ucTransaction.listTransactions();
    }

    public Result<List<Transaction>, DomainError> listPendingTransactions() {
        return ucTransaction.listPendingTransactions();
    }

    public Result<Transaction, DomainError> findTransaction(UUID id) {
        return ucTransaction.findTransaction(id);
    }

    public Result<Transaction, DomainError> createTransaction(TransactionCommand cmd) {
        return ucTransaction.createTransaction(cmd);
    }

    public Result<Transaction, DomainError> updateTransaction(UUID id, TransactionCommand cmd) {
        return ucTransaction.updateTransaction(id, cmd);
    }

    public Result<Transaction, DomainError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.updateTransactionStatus(id, status, paymentDate);
    }

    public Result<Void, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return ucTransaction.deleteTransaction(id, mode);
    }

    public Result<Transaction, DomainError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount);
    }

    public Result<Transaction, DomainError> createImportedTransaction(ImportedTransactionCommand cmd) {
        return ucTransaction.createImported(cmd);
    }

    // ── Cost center operations ─────────────────────────────────────

    public Result<List<CostCenter>, DomainError> listCostCenters() {
        return ucMetadata.listCostCenters();
    }

    public Result<CostCenter, DomainError> createCostCenter(CostCenterCommand cmd) {
        return ucMetadata.createCostCenter(cmd);
    }

    public Result<CostCenter, DomainError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        return ucMetadata.updateCostCenter(id, cmd);
    }

    public Result<Void, DomainError> deleteCostCenter(UUID id) {
        return ucMetadata.deleteCostCenter(id);
    }
}
