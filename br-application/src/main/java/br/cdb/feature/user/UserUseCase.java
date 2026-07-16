package br.cdb.feature.user;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.core.web.security.CurrentUser;
import br.cdb.feature.user.accounts.closing.ClosingService;
import br.cdb.feature.user.accounts.core.AccountStreamPublisher;
import br.cdb.feature.user.accounts.core.UserAccount;
import br.cdb.feature.user.accounts.core.UserAccountService;
import br.cdb.feature.user.accounts.transactions.UserTransaction;
import br.cdb.feature.user.accounts.transactions.UserTransactionService;
import br.cdb.feature.user.accounts.transactions.importer.ImportError;
import br.cdb.feature.user.accounts.transactions.importer.ImportResult;
import br.cdb.feature.user.accounts.transactions.importer.StatementImportService;
import br.cdb.feature.user.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.cdb.feature.user.accounts.transactions.importer.preview.ImportPreviewOutcome;
import br.cdb.feature.user.categories.UserCategory;
import br.cdb.feature.user.categories.UserCategoryService;
import br.cdb.feature.user.dashboard.core.DashboardService;
import br.cdb.feature.user.deletion.DeletionOutcome;
import br.cdb.feature.user.deletion.DeletionStrategy;
import br.cdb.feature.user.deletion.Deletions;
import br.cdb.feature.user.profile.PreferencesPatch;
import br.cdb.feature.user.profile.Profile;
import br.cdb.feature.user.profile.UserProfileService;
import br.cdb.feature.user.tags.UserTag;
import br.cdb.feature.user.tags.UserTagService;
import br.cdb.feature.user.tags.UserTransactionTagService;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Único use case da fatia {@code feature.user}: orquestra os use cases do contexto monetário
 * (via {@link MonetaryContext}) e os serviços de feature (overlays, SSE, fechamento, importação).
 * Os {@code *Resource} apenas traduzem formato (HTTP ⇄ comandos/views) e delegam aqui.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserUseCase {

    private final AccountUseCase ucAccount = MonetaryContext.ucAccount();
    private final CreditCardUseCase ucCreditCard = MonetaryContext.ucCreditCard();
    private final TransactionUseCase ucTransaction = MonetaryContext.ucTransaction();

    private final UserGuards guards;
    private final UserAccountService userAccountService;
    private final UserTransactionService userTransactionService;
    private final UserTransactionTagService tagLinkService;
    private final UserCategoryService userCategoryService;
    private final UserTagService userTagService;
    private final ClosingService closingService;
    private final DashboardService dashboardService;
    private final UserProfileService profileService;
    private final StatementImportService statementImportService;
    private final AccountStreamPublisher accountStreamPublisher;

    // ── Views ──────────────────────────────────────────────────────

    /** Conta do contexto + overlay do usuário + cartões; {@code transactions} é a lista completa
     *  (o saldo corrente do DTO é derivado dela). */
    @NullMarked
    public record AccountView(
            Account account,
            @Nullable UserAccount overlay,
            List<CreditCard> cards,
            List<Transaction> transactions
    ) {}

    @NullMarked
    public record TransactionView(Transaction transaction, @Nullable UserTransaction overlay) {}

    @NullMarked
    public record TransactionFilter(@Nullable UUID accountId, @Nullable Integer limit,
                                    @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo,
                                    @Nullable String status, @Nullable String type) {}

    /** Preview + nomes de conta por id (rotulam as opções de cartão na resposta). */
    @NullMarked
    public record ImportPreviewView(ImportPreviewOutcome outcome, Map<UUID, String> accountNamesById) {}

    // ── Accounts ───────────────────────────────────────────────────

    public Result<List<AccountView>, BusinessError> accounts() {
        val userId = CurrentUser.getId();

        val views = new ArrayList<AccountView>();
        for (val overlay : userAccountService.findByUser(userId)) {
            val account = ucAccount.findAccount(overlay.accountId());
            if (account.isFailure()) {
                Logger.warn("overlay %s aponta para conta inexistente %s", overlay.userId(), overlay.accountId());
                continue;
            }

            views.add(new AccountView(
                    account.get(),
                    overlay,
                    ucCreditCard.list(overlay.accountId()).getOrElse(List.of()),
                    transactionsOf(overlay.accountId())
            ));
        }

        return Result.success(views);
    }

    public Result<AccountView, BusinessError> account(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> {
            val overlay = userAccountService.find(CurrentUser.getId(), accountId);

            val account = ucAccount.findAccount(accountId);
            if (account.isFailure()) {
                Logger.warn("Conta %s não pertence ao usuário", accountId.toString());
                return Result.failure(new BusinessError.NotFound(accountId.toString()));
            }

            return Result.success(new AccountView(
                    account.get(),
                    overlay,
                    ucCreditCard.list(accountId).getOrElse(List.of()),
                    transactionsOf(accountId)
            ));
        });
    }

    public Result<AccountView, BusinessError> createAccount(AccountCommand.Create cmd, String color) {
        val userId = CurrentUser.getId();
        return ucAccount.upsert(cmd).map(account -> {
            val overlay = new UserAccount(userId, account.id(), color);
            userAccountService.save(overlay);
            accountStreamPublisher.upsert(account.id());
            return new AccountView(account, overlay, List.of(), List.of());
        });
    }

    public Result<AccountView, BusinessError> updateAccount(AccountCommand.Update cmd, String color) {
        return guards.ownsAccount(cmd.id()).flatMap(ignored -> {
            val userId = CurrentUser.getId();
            return ucAccount.upsert(cmd).map(account -> {
                val overlay = new UserAccount(userId, account.id(), color);
                userAccountService.save(overlay);
                accountStreamPublisher.upsert(account.id());
                return new AccountView(account, overlay, cardsOf(account.id()), List.of());
            });
        });
    }

    /**
     * USER_ACCOUNT/USER_TRANSACTION referenciam MON_ACCOUNT (FK) — o overlay precisa sumir/ser
     * re-keyed <em>antes</em> do contexto apagar a conta. MOVE é validado aqui primeiro (fronteira
     * da feature) para nunca reatribuir para um alvo inválido; Block/Purge não têm alvo a validar e
     * contam com o contexto como backstop (race → 409/400 simples, sem corrupção de dados).
     */
    public Result<DeletionOutcome, BusinessError> deleteAccount(
            UUID userId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        val guard = strategy == DeletionStrategy.MOVE
                ? guards.ownsAccounts(id, Objects.requireNonNull(targetId))
                : guards.ownsAccount(id);
        if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);

        if (strategy == null) {
            val count = (int) allTransactions().stream().filter(t -> id.equals(t.accountId())).count();
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
        }

        if (strategy == DeletionStrategy.MOVE) {
            val target = Objects.requireNonNull(targetId);
            val targetCheck = validateAccountMoveTarget(id, target);
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            userTransactionService.reassignAccount(id, target, userId);
        } else {
            userTransactionService.deleteByAccountAndUser(id, userId);
        }
        userAccountService.delete(CurrentUser.getId(), id);

        return ucAccount.delete(new AccountCommand.Delete(id, Deletions.toPolicy(strategy, targetId))).map(ids -> {
            if (strategy != DeletionStrategy.MOVE) {
                ids.forEach(tagLinkService::deleteByTransaction);
            }
            accountStreamPublisher.delete(id);
            if (strategy == DeletionStrategy.MOVE) {
                accountStreamPublisher.upsert(Objects.requireNonNull(targetId));
            }
            return new DeletionOutcome.Completed();
        });
    }

    private Result<Void, BusinessError> validateAccountMoveTarget(UUID sourceId, UUID targetId) {
        return ucAccount.findAccount(targetId).flatMap(target -> {
            if (target.id().equals(sourceId)) {
                return Result.failure(new BusinessError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.failure(new BusinessError.BusinessRule("Target account is inactive: " + targetId));
            }
            return Result.success();
        });
    }

    // ── Balances ───────────────────────────────────────────────────

    public Result<AccountBalance, BusinessError> monthlyBalance(UUID accountId, YearMonth period) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucAccount.getMonthlyBalance(accountId, period));
    }

    public Result<List<AccountBalance>, BusinessError> yearBalances(UUID accountId, int year) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucAccount.getYearBalances(accountId, year));
    }

    // ── Credit cards ───────────────────────────────────────────────

    public Result<List<CreditCard>, BusinessError> cards(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucCreditCard.list(accountId));
    }

    public Result<CreditCard, BusinessError> createCard(CardCommand.Create cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> ucCreditCard.upsert(cmd))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(cmd.accountId()));
    }

    public Result<DeletionOutcome, BusinessError> deleteCard(
            UUID accountId, UUID cardId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return guards.ownsCard(accountId, cardId).flatMap(ignored -> {
            if (strategy == null) {
                val count = (int) allTransactions().stream().filter(t -> cardId.equals(t.cardId())).count();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            }

            return ucCreditCard.delete(new CardCommand.Delete(cardId, Deletions.toPolicy(strategy, targetId))).map(ids -> {
                // MOVE mantém o cartão de destino na mesma conta: sem re-key de overlay a fazer.
                if (strategy != DeletionStrategy.MOVE) {
                    ids.forEach(id -> {
                        userTransactionService.deleteByTransaction(id);
                        tagLinkService.deleteByTransaction(id);
                    });
                }
                accountStreamPublisher.upsert(accountId);
                return new DeletionOutcome.Completed();
            });
        });
    }

    public Result<CreditCard, BusinessError> setCardActive(UUID accountId, UUID cardId, boolean active) {
        return guards.ownsCard(accountId, cardId)
                .flatMap(ignored -> ucCreditCard.upsert(new CardCommand.Update(cardId, active)))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(accountId));
    }

    // ── Transactions ───────────────────────────────────────────────

    public Result<List<TransactionView>, BusinessError> transactions(UUID userId, TransactionFilter filter) {
        val accountId = filter.accountId();
        val guard = accountId == null ? Result.<BusinessError>success() : guards.ownsAccount(accountId);

        return guard.flatMap(ignored -> {
            val dateFrom = filter.dateFrom();
            val dateTo = filter.dateTo();
            val status = filter.status();
            val type = filter.type();
            val limit = filter.limit();

            return ucTransaction.transactions().map(all -> {
                val overlays = userTransactionService.indexByTransaction(userId);
                val filtered = all.stream()
                        .filter(t -> accountId == null || accountId.equals(t.accountId()))
                        .filter(t -> dateFrom == null || !t.date().isBefore(dateFrom))
                        .filter(t -> dateTo == null || !t.date().isAfter(dateTo))
                        .filter(t -> status == null || status.equalsIgnoreCase(t.status().name()))
                        .filter(t -> type == null || type.equalsIgnoreCase(t.type().name()))
                        .toList();
                val page = (limit != null && limit > 0 && limit < filtered.size())
                        ? filtered.subList(0, limit)
                        : filtered;
                return page.stream().map(t -> new TransactionView(t, overlays.get(t.id()))).toList();
            });
        });
    }

    public Result<TransactionView, BusinessError> createTransaction(UUID userId, TransactionCommand.Create cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (closingService.validateDate(cmd.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return ucTransaction.upsert(cmd).map(t -> {
            // Cria o USER_TRANSACTION da primeira parcela e depois o das irmãs do grupo
            val overlay = userTransactionService.save(t.id(), t.accountId(), userId, categoryId);
            saveUserTransactionForGroup(t, userId, categoryId);
            accountStreamPublisher.upsert(t.accountId());
            return new TransactionView(t, overlay);
        });
    }

    public Result<TransactionView, BusinessError> updateTransaction(UUID userId, TransactionCommand.Update cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }

        val txId = cmd.id();
        UUID previous = null;
        if (ucTransaction.findTransaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            val guard = closingService.validateDate(existing.date())
                    .flatMap(ignored -> closingService.validateDate(cmd.date()));
            if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            previous = existing.accountId();
        }

        val previousAccountId = previous;
        return ucTransaction.upsert(cmd).map(t -> {
            // A PK do USER_TRANSACTION inclui a conta: se a atualização moveu a transação de conta,
            // a linha antiga da chave composta ficaria órfã (o save abaixo faria INSERT, não UPDATE).
            if (previousAccountId != null && !previousAccountId.equals(t.accountId())) {
                userTransactionService.deleteByTransactionAccountAndUser(t.id(), previousAccountId, userId);
            }
            val existingOverlay = userTransactionService.find(t.id(), t.accountId(), userId);
            val overlay = userTransactionService.save(t.id(), t.accountId(), userId, categoryId);
            // Se for grupo de parcelas: atualiza a categoria de todos os membros
            if (t.groupId() != null && existingOverlay.isEmpty()) {
                saveUserTransactionForGroup(t, userId, categoryId);
            }
            publishAccountUpdate(t.accountId(), previousAccountId);
            return new TransactionView(t, overlay);
        });
    }

    public Result<TransactionView, BusinessError> updateTransactionStatus(
            UUID userId, UUID txId, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.findTransaction(txId)
                .flatMap(existing -> guards.ownsAccount(existing.accountId()))
                .flatMap(ignored -> ucTransaction.updateTransactionStatus(txId, status, paymentDate).map(t -> {
                    val overlay = userTransactionService.find(t.id(), t.accountId(), userId).orElse(null);
                    accountStreamPublisher.upsert(t.accountId());
                    return new TransactionView(t, overlay);
                }));
    }

    public Result<Void, BusinessError> deleteTransaction(UUID txId, TransactionScope scope) {
        UUID accountId = null;
        if (ucTransaction.findTransaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            if (closingService.validateDate(existing.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            accountId = existing.accountId();
        }

        val affected = accountId;
        return ucTransaction.delete(new TransactionCommand.Delete(txId, scope)).map(ids -> {
            ids.forEach(id -> {
                userTransactionService.deleteByTransaction(id);
                tagLinkService.deleteByTransaction(id);
            });
            if (affected != null) accountStreamPublisher.upsert(affected);
            return null;
        });
    }

    public Result<TransactionView, BusinessError> transfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (guards.ownsAccounts(fromAccountId, toAccountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (closingService.validateDate(date) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount).map(t -> {
            accountStreamPublisher.upsert(fromAccountId);
            accountStreamPublisher.upsert(toAccountId);
            return new TransactionView(t, null);
        });
    }

    /** Publica a conta atual e, se a transação mudou de conta, também a conta anterior. */
    private void publishAccountUpdate(UUID accountId, @Nullable UUID previousAccountId) {
        accountStreamPublisher.upsert(accountId);
        if (previousAccountId != null && !previousAccountId.equals(accountId)) {
            accountStreamPublisher.upsert(previousAccountId);
        }
    }

    private void saveUserTransactionForGroup(Transaction first, UUID userId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        allTransactions().stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), t.accountId(), userId, categoryId));
    }

    // ── Statement import ───────────────────────────────────────────

    public Result<ImportPreviewView, ImportError> importPreview(byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (accountId != null && guards.ownsAccount(accountId).isFailure()) {
            return new Result.Failure<>(new ImportError.AccountNotFound());
        }
        return statementImportService.preview(fileBytes, password, accountId)
                .map(outcome -> new ImportPreviewView(outcome, accountNamesById()));
    }

    public Result<ImportResult, BusinessError> confirmInvoiceImport(ImportConfirmCommand cmd) {
        for (val row : cmd.rows()) {
            if (guards.ownsCard(row.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
        }
        return statementImportService.confirm(cmd)
                .ifSuccess(ignored -> affectedAccountIds(cmd.rows()).forEach(accountStreamPublisher::upsert));
    }

    public Result<ImportResult, BusinessError> confirmStatementImport(BankStatementConfirmCommand cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> statementImportService.confirmStatement(cmd))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(cmd.accountId()));
    }

    /** Nome da conta a que cada cartão pertence, para rotular as opções de cartão do preview. */
    private Map<UUID, String> accountNamesById() {
        return ucAccount.listAccounts().getOrElse(List.of()).stream()
                .collect(Collectors.toMap(Account::id, Account::name));
    }

    /** Contas distintas donas dos cartões das linhas confirmadas. */
    private List<UUID> affectedAccountIds(List<ImportConfirmCommand.Row> rows) {
        val accountByCard = ucCreditCard.list().getOrElse(List.of()).stream()
                .collect(Collectors.toMap(CreditCard::id, CreditCard::accountId));
        return rows.stream()
                .map(row -> accountByCard.get(row.cardId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // ── Closing ────────────────────────────────────────────────────

    public Optional<YearMonth> closing() {
        return closingService.find();
    }

    public YearMonth saveClosing(YearMonth period) {
        return closingService.save(period);
    }

    public void clearClosing() {
        closingService.clear();
    }

    // ── Categories ─────────────────────────────────────────────────

    public List<UserCategory> categories(UUID userId) {
        return userCategoryService.findAll(userId);
    }

    public Result<UserCategory, BusinessError> createCategory(UUID userId, String name, Transaction.Type nature, @Nullable UUID parentId) {
        if (parentId != null
                && userCategoryService.validateParent(parentId, nature) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return userCategoryService.validateUniqueName(userId, nature.name(), name, parentId, null)
                .map(ignored -> userCategoryService.create(userId, name, nature, parentId));
    }

    public Result<UserCategory, BusinessError> updateCategory(UUID userId, UUID id, String name, @Nullable UUID parentId, @Nullable Boolean active) {
        return userCategoryService.findById(id).flatMap(existing -> {
            if (existing.isSystem()) {
                return Result.failure(new BusinessError.BusinessRule("Categoria de sistema não pode ser modificada"));
            }
            return userCategoryService.validateUniqueName(userId, existing.nature().name(), name, parentId, id)
                    .map(ignored -> userCategoryService.update(id, name, parentId, active));
        });
    }

    public Result<DeletionOutcome, BusinessError> deleteCategory(
            UUID userId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return userCategoryService.subtreeIds(id, userId).flatMap(subtree -> {
            if (strategy == null) {
                val count = userTransactionService.findTransactionIdsByCategories(userId, subtree).size();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
                userCategoryService.deletePlain(subtree, userId);
                return Result.success(new DeletionOutcome.Completed());
            }

            val result = strategy == DeletionStrategy.MOVE
                    ? moveCategorySubtree(id, Objects.requireNonNull(targetId), userId)
                    : deleteCategoryWithTransactions(subtree, userId);
            return result.map(ignored -> new DeletionOutcome.Completed());
        });
    }

    /** Reatribui toda a subárvore para {@code targetId} (já validado pelo service) e apaga a subárvore. */
    private Result<Void, BusinessError> moveCategorySubtree(UUID id, UUID targetId, UUID userId) {
        return userCategoryService.validateMoveTarget(id, targetId, userId).map(subtree -> {
            subtree.forEach(nodeId -> userTransactionService.reassignCategory(nodeId, targetId, userId));
            userCategoryService.deletePlain(subtree, userId);
            return null;
        });
    }

    /** Apaga as transações vinculadas à subárvore inteira e depois a subárvore. */
    private Result<Void, BusinessError> deleteCategoryWithTransactions(List<UUID> subtreeIds, UUID userId) {
        val txIds = userTransactionService.findTransactionIdsByCategories(userId, subtreeIds);
        return deleteLinkedTransactions(txIds, () -> userCategoryService.deletePlain(subtreeIds, userId));
    }

    // ── Tags ───────────────────────────────────────────────────────

    public List<UserTag> tags(UUID userId) {
        return userTagService.findAll(userId);
    }

    public UserTag createTag(UUID userId, String name, String color) {
        return userTagService.create(userId, name, color);
    }

    public Result<UserTag, BusinessError> updateTag(UUID id, String name, String color) {
        return userTagService.update(id, name, color);
    }

    public Result<DeletionOutcome, BusinessError> deleteTag(
            UUID userId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        if (strategy == null) {
            val count = userTagService.linkedTransactionIds(userId, id).size();
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            return userTagService.deleteById(id).map(ignored -> new DeletionOutcome.Completed());
        }

        val result = switch (strategy) {
            case MOVE -> userTagService.deleteMoving(id, Objects.requireNonNull(targetId), userId);
            case DELETE -> deleteTagWithTransactions(id, userId);
            case DETACH -> userTagService.deleteDetached(id, userId);
        };
        return result.map(ignored -> new DeletionOutcome.Completed());
    }

    /** Apaga as transações vinculadas à tag e depois a tag em si. */
    private Result<Void, BusinessError> deleteTagWithTransactions(UUID id, UUID userId) {
        return userTagService.findById(id).flatMap(existing -> {
            val txIds = userTagService.linkedTransactionIds(userId, id);
            return deleteLinkedTransactions(txIds, () -> userTagService.deleteById(id));
        });
    }

    /** Apaga as transações (via facade) + overlay/vínculo de tag, executa {@code afterCleanup} (ex.: apagar
     *  categoria/tag agora desvinculada) e por fim publica o SSE de conta para cada conta afetada — resolvidas
     *  antes do delete, quando as transações ainda existem. */
    private Result<Void, BusinessError> deleteLinkedTransactions(List<UUID> txIds, Runnable afterCleanup) {
        val affectedAccountIds = accountIdsOfTransactions(txIds);

        if (ucTransaction.deleteTransactions(txIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        txIds.forEach(txId -> {
            userTransactionService.deleteByTransaction(txId);
            tagLinkService.deleteByTransaction(txId);
        });

        afterCleanup.run();
        affectedAccountIds.forEach(accountStreamPublisher::upsert);
        return Result.success();
    }

    private Set<UUID> accountIdsOfTransactions(List<UUID> txIds) {
        val txIdSet = Set.copyOf(txIds);
        return ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> txIdSet.contains(t.id()))
                .map(Transaction::accountId)
                .collect(Collectors.toSet());
    }

    // ── Dashboard ──────────────────────────────────────────────────

    public Result<DashboardService.MonthlyResult, BusinessError> monthlyResult(int month, int year) {
        return dashboardService.getMonthlyResult(month, year);
    }

    // ── Profile (self) ─────────────────────────────────────────────

    public Result<Profile, BusinessError> profile() {
        return profileService.getProfile(CurrentUser.getId());
    }

    /** PATCH parcial: aplica nome e/ou preferências de forma independente (merge). */
    public Result<Profile, BusinessError> updateProfile(@Nullable String name, @Nullable PreferencesPatch preferences) {
        val id = CurrentUser.getId();

        var result = name != null
                ? profileService.updateName(id, name)
                : profileService.getProfile(id);

        if (preferences != null) {
            result = result.flatMap(ignored -> profileService.updatePreferences(id, preferences));
        }
        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────

    private List<CreditCard> cardsOf(UUID accountId) {
        return ucCreditCard.list(accountId).getOrElse(List.of());
    }

    private List<Transaction> transactionsOf(UUID accountId) {
        return ucTransaction.transactions(accountId).getOrElse(List.of());
    }

    private List<Transaction> allTransactions() {
        return ucTransaction.transactions().getOrElse(List.of());
    }
}
