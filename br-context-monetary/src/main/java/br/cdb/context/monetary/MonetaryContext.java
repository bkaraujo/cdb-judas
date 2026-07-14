package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.*;
import br.cdb.context.monetary._1_application.command.*;
import br.cdb.context.monetary._1_application.event.TransactionEventListener;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CardUseCase;
import br.cdb.context.monetary._1_application.usecase.MetadataUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.annotation.Facade;
import br.commons.business.BusinessError;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
public class MonetaryContext implements Facade {

    private final CardUseCase ucCard = Registry.tryGet(CardUseCase.class);
    private final AccountUseCase ucAccount = Registry.tryGet(AccountUseCase.class);
    private final MetadataUseCase ucMetadata = Registry.tryGet(MetadataUseCase.class);
    private final TransactionUseCase ucTransaction = Registry.tryGet(TransactionUseCase.class);

    private MonetaryContext() {}

    public static synchronized MonetaryContext instance() {
        return Registry.tryGet(MonetaryContext.class, () -> {
            MessageBus.subscribe(new TransactionEventListener());
            return new MonetaryContext();
        });
    }

    // ── Account operations ─────────────────────────────────────────

    public Result<List<Account>, BusinessError> listAccounts() {
        return ucAccount.listAccounts();
    }

    public Result<Account, BusinessError> findAccount(UUID id) {
        return ucAccount.findAccount(id);
    }

    public Result<Account, BusinessError> createAccount(AccountCommand cmd) {
        return ucAccount.createAccount(cmd);
    }

    public Result<Account, BusinessError> updateAccount(UUID id, AccountCommand cmd) {
        return ucAccount.updateAccount(id, cmd);
    }

    public Result<List<UUID>, BusinessError> deleteAccount(UUID id, TransactionPolicy policy) {
        return ucAccount.deleteAccount(id, policy);
    }

    // ── Balance operations ─────────────────────────────────────────

    public Result<AccountBalance, BusinessError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return ucAccount.getMonthlyBalance(accountId, period);
    }

    public Result<List<AccountBalance>, BusinessError> getYearBalances(UUID accountId, int year) {
        return ucAccount.getYearBalances(accountId, year);
    }

    // ── Transaction operations ─────────────────────────────────────

    public Result<List<Transaction>, BusinessError> listTransactions() {
        return ucTransaction.listTransactions();
    }

    public Result<List<Transaction>, BusinessError> listPendingTransactions() {
        return ucTransaction.listPendingTransactions();
    }

    public Result<Transaction, BusinessError> findTransaction(UUID id) {
        return ucTransaction.findTransaction(id);
    }

    public Result<Transaction, BusinessError> createTransaction(TransactionCommand cmd) {
        return ucTransaction.createTransaction(cmd);
    }

    public Result<Transaction, BusinessError> updateTransaction(UUID id, TransactionCommand cmd) {
        return ucTransaction.updateTransaction(id, cmd);
    }

    public Result<Transaction, BusinessError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.updateTransactionStatus(id, status, paymentDate);
    }

    public Result<List<UUID>, BusinessError> deleteTransaction(UUID id, @Nullable String mode) {
        return ucTransaction.deleteTransaction(id, mode);
    }

    public Result<Void, BusinessError> deleteTransactions(List<UUID> ids) {
        return ucTransaction.deleteTransactions(ids);
    }

    public Result<Transaction, BusinessError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount);
    }

    public Result<Transaction, BusinessError> createImportedTransaction(ImportedTransactionCommand cmd) {
        return ucTransaction.createImported(cmd);
    }

    // ── Cost center operations ─────────────────────────────────────

    public Result<List<CostCenter>, BusinessError> listCostCenters() {
        return ucMetadata.listCostCenters();
    }

    public Result<CostCenter, BusinessError> createCostCenter(CostCenterCommand cmd) {
        return ucMetadata.createCostCenter(cmd);
    }

    public Result<CostCenter, BusinessError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        return ucMetadata.updateCostCenter(id, cmd);
    }

    public Result<Void, BusinessError> deleteCostCenter(UUID id) {
        return ucMetadata.deleteCostCenter(id);
    }

    // ── CreditCard operations ─────────────────────────────────────────────

    public Result<List<CreditCard>, BusinessError> listCards() {
        return ucCard.listCards();
    }

    public Result<List<CreditCard>, BusinessError> listCardsByAccount(UUID accountId) {
        return ucCard.listCardsByAccount(accountId);
    }

    public Result<CreditCard, BusinessError> createCard(CardCommand cmd) {
        return ucCard.createCard(cmd);
    }

    public Result<List<UUID>, BusinessError> deleteCard(UUID id, TransactionPolicy policy) {
        return ucCard.deleteCard(id, policy);
    }

    public Result<CreditCard, BusinessError> setCardActive(UUID id, boolean active) {
        return ucCard.setActive(id, active);
    }
}
