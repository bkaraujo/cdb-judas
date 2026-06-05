package br.community.context.monetary;

import br.commons.Result;
import br.commons.annotation.Facade;
import br.community.context.monetary._0_domain.model.*;
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
import java.util.Optional;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MonetaryContext implements Facade {

    private final AccountUseCase ucAccount;
    private final TransactionUseCase ucTransaction;
    private final MetadataUseCase ucMetadata;

    // ── Account operations ─────────────────────────────────────────

    public Result<List<MonetaryAccount>, DomainError> listAccounts() {
        return ucAccount.listAccounts();
    }

    public Result<MonetaryAccount, DomainError> findAccount(UUID id) {
        return ucAccount.findAccount(id);
    }

    public Result<MonetaryAccount, DomainError> createAccount(AccountCommand cmd) {
        return ucAccount.createAccount(cmd);
    }

    public Result<MonetaryAccount, DomainError> updateAccount(UUID id, AccountCommand cmd) {
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

    public Result<List<MonetaryAccount>, DomainError> listCreditCards() {
        return ucAccount.listCreditCards();
    }

    public Result<List<MonetaryAccount>, DomainError> listCreditCardsByAccount(UUID accountId) {
        return ucAccount.listCreditCardsByAccount(accountId);
    }

    public Result<MonetaryAccount, DomainError> createCreditCard(CreditCardCommand cmd) {
        return ucAccount.createCreditCard(cmd);
    }

    public Result<MonetaryAccount, DomainError> updateCreditCard(UUID id, CreditCardCommand cmd) {
        return ucAccount.updateCreditCard(id, cmd);
    }

    public Result<Void, DomainError> deleteCreditCard(UUID id) {
        return ucAccount.deleteCreditCard(id);
    }

    // ── Transaction operations ─────────────────────────────────────

    public Result<List<MonetaryTransaction>, DomainError> listTransactions() {
        return ucTransaction.listTransactions();
    }

    public Result<List<MonetaryTransaction>, DomainError> listPendingTransactions() {
        return ucTransaction.listPendingTransactions();
    }

    public Result<MonetaryTransaction, DomainError> createTransaction(TransactionCommand cmd) {
        return ucTransaction.createTransaction(cmd);
    }

    public Result<MonetaryTransaction, DomainError> updateTransaction(UUID id, TransactionCommand cmd) {
        return ucTransaction.updateTransaction(id, cmd);
    }

    public Result<MonetaryTransaction, DomainError> updateTransactionStatus(UUID id, String status, @Nullable LocalDate paymentDate) {
        return ucTransaction.updateTransactionStatus(id, status, paymentDate);
    }

    public Result<Void, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return ucTransaction.deleteTransaction(id, mode);
    }

    public Result<MonetaryTransaction, DomainError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount);
    }

    // ── Imported transactions (used by the statement-import feature) ─

    public Result<MonetaryTransaction, DomainError> createImportedTransaction(ImportedTransactionCommand cmd) {
        return ucTransaction.createImported(cmd);
    }

    // ── Cost center operations ─────────────────────────────────────

    public Result<List<MonetaryCenter>, DomainError> listCostCenters() {
        return ucMetadata.listCostCenters();
    }

    public Result<MonetaryCenter, DomainError> createCostCenter(CostCenterCommand cmd) {
        return ucMetadata.createCostCenter(cmd);
    }

    public Result<MonetaryCenter, DomainError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        return ucMetadata.updateCostCenter(id, cmd);
    }

    // ── Category operations ────────────────────────────────────────

    public Result<List<MonetaryCategory>, DomainError> listCategories() {
        return ucMetadata.listCategories();
    }

    public Result<MonetaryCategory, DomainError> findCategoryById(UUID id) {
        return ucMetadata.findCategoryById(id);
    }

    public Result<MonetaryCategory, DomainError> createCategory(CategoryCommand cmd) {
        return ucMetadata.createCategory(cmd);
    }

    public Result<MonetaryCategory, DomainError> updateCategory(UUID id, CategoryCommand cmd) {
        return ucMetadata.updateCategory(id, cmd);
    }

    public MonetaryCategory findOrCreateUncategorizedCategory() {
        return ucMetadata.findOrCreateUncategorizedCategory();
    }

    public Result<Void, DomainError> deleteCategory(UUID id) {
        return ucMetadata.deleteCategory(id);
    }

    public Result<Void, DomainError> deleteCostCenter(UUID id) {
        return ucMetadata.deleteCostCenter(id);
    }

    // ── Tag operations ─────────────────────────────────────────────

    public Result<List<Tag>, DomainError> listTags() {
        return ucMetadata.listTags();
    }

    public Result<Tag, DomainError> createTag(TagCommand cmd) {
        return ucMetadata.createTag(cmd);
    }

    public Result<Tag, DomainError> updateTag(UUID id, TagCommand cmd) {
        return ucMetadata.updateTag(id, cmd);
    }

    public Result<Void, DomainError> deleteTag(UUID id) {
        return ucMetadata.deleteTag(id);
    }

    // ── Closing operations ─────────────────────────────────────────

    public Optional<YearMonth> getClosingPeriod() {
        return ucMetadata.getClosingPeriod();
    }

    public YearMonth setClosingPeriod(YearMonth ym) {
        return ucMetadata.setClosingPeriod(ym);
    }

    public void clearClosingPeriod() {
        ucMetadata.clearClosingPeriod();
    }
}
