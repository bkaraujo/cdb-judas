package br.community.context.monetary;

import br.commons.Result;
import br.commons.annotation.Facade;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.monetary._1_application.command.TransactionPolicy;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.CardUseCase;
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
    private final CardUseCase ucCard;

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

    public Result<List<UUID>, DomainError> deleteAccount(UUID id, TransactionPolicy policy) {
        return ucAccount.deleteAccount(id, policy);
    }

    // ── Balance operations ─────────────────────────────────────────

    public Result<MonthlyBalance, DomainError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return ucAccount.getMonthlyBalance(accountId, period);
    }

    public Result<List<MonthlyBalance>, DomainError> getYearBalances(UUID accountId, int year) {
        return ucAccount.getYearBalances(accountId, year);
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

    public Result<List<UUID>, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return ucTransaction.deleteTransaction(id, mode);
    }

    public Result<Void, DomainError> deleteTransactions(List<UUID> ids) {
        return ucTransaction.deleteTransactions(ids);
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

    // ── Card operations ─────────────────────────────────────────────

    public Result<List<Card>, DomainError> listCards() {
        return ucCard.listCards();
    }

    public Result<List<Card>, DomainError> listCardsByAccount(UUID accountId) {
        return ucCard.listCardsByAccount(accountId);
    }

    public Result<Card, DomainError> createCard(CardCommand cmd) {
        return ucCard.createCard(cmd);
    }

    public Result<List<UUID>, DomainError> deleteCard(UUID id, TransactionPolicy policy) {
        return ucCard.deleteCard(id, policy);
    }

    public Result<Card, DomainError> setCardActive(UUID id, boolean active) {
        return ucCard.setActive(id, active);
    }
}
