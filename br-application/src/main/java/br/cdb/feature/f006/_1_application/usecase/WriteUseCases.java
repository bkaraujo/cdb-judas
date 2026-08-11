package br.cdb.feature.f006._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f004.F004Api;
import br.cdb.feature.f006._0_domain.event.TransactionEvents;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionCategoryService;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.cdb.feature.f006._1_application.service.TransactionTagService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Toda a mutação de transação da fatia {@code f006} — o par de {@link ReadUseCases}, que ficou com a
 * leitura. Context-wired como as demais classes ex-contexto ({@code Context.tryGet(WriteUseCases.class)},
 * nunca {@code @Inject}).
 *
 * <p>Duas camadas de operação convivem aqui, de propósito:
 * <ul>
 *   <li><b>Engine</b> ({@link #upsert}, {@link #delete}, {@link #createTransfer}, {@link #create},
 *       {@code saveCategory}…): aceita qualquer comando bem-formado, publica só evento de domínio
 *       ({@link TransactionEvents}).</li>
 *   <li><b>Entrada da fatia</b> ({@link #createTransaction}, {@link #updateTransaction},
 *       {@link #updateTransactionStatus}, {@link #deleteTransaction}): aplica a
 *       política de usuário — guarda de propriedade ({@link UserGuards}) e período fechado — e
 *       publica os eventos de aplicação ({@link AccountStreamEvents.Refresh} para o SSE, cujo
 *       dispatch é de {@code f999}; {@link TransactionsDeleted} para a cascata best-effort do
 *       listener de overlay ({@code F006Module})/f004).</li>
 * </ul>
 * A entrada de <b>transferência</b> saiu daqui para {@link TransferUseCase}, que orquestra as
 * operações de engine {@link #createTransfer}/{@link #saveTransferCategories} que continuam nesta
 * classe.
 * É o que sobrou da antiga fronteira {@code f006._1_application.TransactionUseCase}, dissolvida aqui:
 * os {@code *Resource} chamam esta classe direto.
 */
@NullMarked
public class WriteUseCases {

    private final TransactionService service = Context.tryGet(TransactionService.class);
    private final TransactionCategoryService categoryService = Context.tryGet(TransactionCategoryService.class);
    private final TransactionTagService tagService = Context.tryGet(TransactionTagService.class);
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);
    private final CreditCardService creditCardService = Context.tryGet(CreditCardService.class);
    private final ReadUseCases reads = Context.tryGet(ReadUseCases.class);
    /** Cliente da API pública de f002 — o fechamento contábil é dela. */
    private final F002Api f002 = Context.tryGet(F002Api.class);
    /** Cliente da API pública de f004 — a posse da tag é dela. */
    private final F004Api f004 = Context.tryGet(F004Api.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    // ── Entrada da fatia: política de usuário + eventos de aplicação ───────────

    public Result<Transaction, BusinessError> createTransaction(UUID personId, TransactionCommand.Create cmd, @Nullable UUID categoryId, List<UUID> tagIds) {
        if (guards().ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (validateClosing(cmd.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (f004.ownsTags(tagIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return upsert(cmd).map(t -> {
            // Vincula a categoria da primeira parcela e depois a das irmãs do grupo
            saveCategory(t.id(), personId, categoryId);
            saveCategoryForGroup(t, personId, categoryId);
            saveTags(t.id(), personId, tagIds);
            MessageBus.submit(new AccountStreamEvents.Refresh(t.accountId(), personId.toString()));
            return t.withCategory(categoryId).withTags(tagIds);
        });
    }

    public Result<Transaction, BusinessError> updateTransaction(UUID personId, TransactionCommand.Update cmd, @Nullable UUID categoryId, List<UUID> tagIds) {
        if (guards().ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (f004.ownsTags(tagIds) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }

        UUID previous = null;
        if (reads.transaction(cmd.id()) instanceof Result.Success(var existing)) {
            if (guards().ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            val guard = validateClosing(existing.date(), cmd.date());
            if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            previous = existing.accountId();
        }

        val previousAccountId = previous;
        return upsert(cmd).map(t -> {
            val hadCategory = reads.withCategory(t, personId).categoryId() != null;
            saveCategory(t.id(), personId, categoryId);
            saveTags(t.id(), personId, tagIds);
            val transferSiblings = reads.transferSiblingsOf(t, personId);
            // Se for grupo de parcelas (nunca transferência — pernas de transferência carregam
            // categoria por natureza da própria perna, nunca a da perna editada; ver
            // saveTransferCategories/transfer()): atualiza a categoria de todos os membros.
            if (t.groupId() != null && !hadCategory && transferSiblings.isEmpty()) {
                saveCategoryForGroup(t, personId, categoryId);
            }
            publishAccountUpdate(personId, t.accountId(), previousAccountId, transferSiblings);
            return t.withCategory(categoryId).withTags(tagIds);
        });
    }

    public Result<Transaction, BusinessError> updateTransactionStatus(
            UUID personId, UUID txId, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return reads.transaction(txId)
                .flatMap(existing -> guards().ownsAccount(existing.accountId()))
                .flatMap(ignored -> updateTransactionStatus(txId, status, paymentDate).map(t -> {
                    MessageBus.submit(new AccountStreamEvents.Refresh(t.accountId(), personId.toString()));
                    return reads.withCategory(t, personId);
                }));
    }

    /** Não limpa vínculo de categoria/tag direto: publica {@link TransactionsDeleted} e deixa os
     *  listeners reagirem, best-effort. */
    public Result<Void, BusinessError> deleteTransaction(UUID txId, TransactionScope scope) {
        UUID accountId = null;
        if (reads.transaction(txId) instanceof Result.Success(var existing)) {
            if (guards().ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            if (validateClosing(existing.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            accountId = existing.accountId();
        }

        val affected = accountId;
        return delete(new TransactionCommand.Delete(txId, scope)).flatMap(ids -> {
            MessageBus.submit(new TransactionsDeleted(ids));
            if (affected != null) MessageBus.submit(new AccountStreamEvents.Refresh(affected, HTTPRequest.personId()));
            return Result.success();
        });
    }

    /**
     * Publica a conta atual, a conta anterior (se mudou) e — quando a transação editada é perna de
     * transferência — a(s) conta(s) da(s) perna(s) irmã(s): {@code updateTransfer} espelha
     * data/valor/status na perna oposta, uma mutação real fora da conta da perna editada.
     */
    private static void publishAccountUpdate(UUID personId, UUID accountId, @Nullable UUID previousAccountId,
                                             List<Transaction> transferSiblings) {
        MessageBus.submit(new AccountStreamEvents.Refresh(accountId, personId.toString()));
        if (previousAccountId != null && !previousAccountId.equals(accountId)) {
            MessageBus.submit(new AccountStreamEvents.Refresh(previousAccountId, personId.toString()));
        }
        for (val sib : transferSiblings) {
            MessageBus.submit(new AccountStreamEvents.Refresh(sib.accountId(), personId.toString()));
        }
    }

    /** Guarda de período fechado: busca o fechamento uma vez ({@link F002Api#closingPeriod}) e
     *  valida todas as datas localmente — {@code updateTransaction} checa data antiga + nova.
     *  Visível ao pacote porque {@link TransferUseCase} aplica a mesma política na sua entrada. */
    Result<Void, BusinessError> validateClosing(LocalDate... dates) {
        val closed = f002.closingPeriod();
        for (val date : dates) {
            if (closed.covers(date)) {
                return Result.failure(new BusinessError.BusinessRule("Período fechado. Lançamentos até %s não podem ser alterados.", closed.label()));
            }
        }
        return Result.success();
    }

    // ── Engine: aceita qualquer comando bem-formado ────────────────────────────

    public Result<Transaction, BusinessError> upsert(TransactionCommand.Upsert cmd) {
        return switch (cmd) {
            case TransactionCommand.Create create ->
                    validateCard(create.accountId(), create.cardId()).flatMap(ignored -> dispatchCreate(create));
            case TransactionCommand.Update update ->
                    service.findById(update.id()).flatMap(existing -> dispatchUpdate(update.id(), existing, update));
        };
    }

    private Result<Transaction, BusinessError> dispatchCreate(TransactionCommand.Create cmd) {
        val count = installmentCount(cmd);
        return count == 1 ? createSingle(cmd) : createInstallments(cmd, count);
    }

    private static int installmentCount(TransactionCommand.Create cmd) {
        return cmd.installments() != null && cmd.installments() > 1 ? cmd.installments() : 1;
    }

    private Result<Transaction, BusinessError> createSingle(TransactionCommand.Create cmd) {
        val saved = service.save(toEntity(UUID.randomUUID(), cmd.description(), cmd.amount(), cmd.date(),
                cmd.accountId(), cmd.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(), null, null, null));
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Result<Transaction, BusinessError> createInstallments(TransactionCommand.Create cmd, int installmentsCount) {
        val groupId = UUID.randomUUID();
        val batch = new ArrayList<Transaction>();

        for (int i = 1; i <= installmentsCount; i++) {
            val date = cmd.date().plusMonths(i - 1);
            val status = (i == 1) ? cmd.status() : Transaction.Status.PENDING;
            batch.add(toEntity(UUID.randomUUID(), cmd.description(), cmd.amount(), date, cmd.accountId(), status,
                    cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(), groupId, i, installmentsCount));
        }

        Transaction first = null;
        for (val t : batch) {
            val saved = service.save(t);
            if (first == null) first = saved;
        }

        if (first != null) MessageBus.submit(new TransactionEvents.Created(first));

        return Result.success(first);
    }

    private Result<Transaction, BusinessError> dispatchUpdate(UUID id, Transaction existing, TransactionCommand.Update cmd) {
        val transferSiblings = service.findTransferSiblings(existing);
        if (!transferSiblings.isEmpty()) return updateTransfer(existing, transferSiblings, cmd);
        return validateCard(cmd.accountId(), cmd.cardId()).flatMap(ignored -> updateNonTransfer(id, existing, cmd));
    }

    private Result<Transaction, BusinessError> updateNonTransfer(UUID id, Transaction existing, TransactionCommand.Update cmd) {
        val isFuture = cmd.scope() instanceof TransactionScope.Future && existing.groupId() != null;

        if (!isFuture) {
            val updated = service.save(toEntity(id, cmd.description(), cmd.amount(), cmd.date(), cmd.accountId(),
                    cmd.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(),
                    existing.groupId(), existing.installmentNumber(), existing.totalInstallments()));
            MessageBus.submit(new TransactionEvents.Updated(existing));
            MessageBus.submit(new TransactionEvents.Updated(updated));
            return Result.<Transaction, BusinessError>success(updated);
        }

        val groupId = existing.groupId();
        val installmentNumber = existing.installmentNumber();
        if (groupId == null) return Result.<Transaction, BusinessError>success(existing);

        val all = service.findByGroupId(groupId).stream()
                .filter(t -> t.installmentNumber() >= installmentNumber)
                .sorted(Comparator.comparing(Transaction::installmentNumber))
                .toList();

        Transaction firstSaved = null;
        for (val t : all) {
            val currentNumber = t.installmentNumber();
            val newDate = cmd.date().plusMonths(currentNumber - installmentNumber);
            val updated = service.save(toEntity(t.id(), cmd.description(), cmd.amount(), newDate, cmd.accountId(),
                    t.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(),
                    t.groupId(), t.installmentNumber(), t.totalInstallments()));
            if (t.id().equals(id)) firstSaved = updated;
        }

        if (firstSaved != null) {
            MessageBus.submit(new TransactionEvents.Updated(existing));
            MessageBus.submit(new TransactionEvents.Updated(firstSaved));
        }

        return Result.success(firstSaved);
    }

    /** A transfer stays an inseparable pair when edited. */
    private Result<Transaction, BusinessError> updateTransfer(Transaction edited, List<Transaction> siblings, TransactionCommand.Update cmd) {
        val newAccount = cmd.accountId();
        if (siblings.stream().anyMatch(sib -> newAccount.equals(sib.accountId()))) {
            return Result.failure(new BusinessError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val absAmount = cmd.amount().abs();
        val updatedEdited = service.save(withTransferEdits(edited, newAccount, absAmount, cmd.date(), cmd.status()));
        MessageBus.submit(new TransactionEvents.Updated(edited));
        MessageBus.submit(new TransactionEvents.Updated(updatedEdited));

        for (val sib : siblings) {
            val updatedSib = service.save(withTransferEdits(sib, sib.accountId(), absAmount, cmd.date(), cmd.status()));
            MessageBus.submit(new TransactionEvents.Updated(updatedSib));
        }
        return Result.success(updatedEdited);
    }

    /** Transfer legs never carry a card — cardId is always null. */
    private static Transaction withTransferEdits(Transaction leg, UUID accountId, BigDecimal absAmount, LocalDate date, Transaction.Status status) {
        return new Transaction(
                leg.id(), leg.description(), absAmount, date,
                accountId, status, leg.type(), leg.costCenterId(),
                Transaction.Status.CONFIRMED.equals(status) ? date : null,
                leg.groupId(), leg.installmentNumber(), leg.totalInstallments(), leg.notes(), null);
    }

    public Result<Transaction, BusinessError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return service.findById(id)
                .map(existing -> {
                    val saved = service.save(new Transaction(
                            existing.id(), existing.description(), existing.amount(), existing.date(),
                            existing.accountId(), status, existing.type(), existing.costCenterId(), paymentDate,
                            existing.groupId(), existing.installmentNumber(), existing.totalInstallments(), existing.notes(),
                            existing.cardId()
                    ));
                    MessageBus.submit(new TransactionEvents.Updated(saved));
                    return saved;
                });
    }

    /** Ids das transações apagadas (par de transferência / lote FUTURE / unitário). */
    public Result<List<UUID>, BusinessError> delete(TransactionCommand.Delete command) {
        return service.findById(command.id()).flatMap(existing -> {
            val transferSiblings = service.findTransferSiblings(existing);
            if (!transferSiblings.isEmpty()) return deleteTransferGroup(existing, transferSiblings);

            val isFuture = command.scope() instanceof TransactionScope.Future && existing.groupId() != null;
            val groupId = existing.groupId();

            if (!isFuture || groupId == null) {
                return service.deleteById(command.id()).map(ignored -> {
                    MessageBus.submit(new TransactionEvents.Deleted(existing));
                    return List.of(command.id());
                });
            }

            val installmentNumber = existing.installmentNumber();
            val toDelete = service.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() >= installmentNumber)
                    .toList();

            for (val t : toDelete) {
                val delRes = service.deleteById(t.id());
                if (delRes instanceof Result.Failure<Void, BusinessError>(BusinessError error)) return Result.<List<UUID>>failure(error);
            }

            MessageBus.submit(new TransactionEvents.Deleted(existing));
            return Result.success(toDelete.stream().map(Transaction::id).toList());
        });
    }

    private Result<List<UUID>, BusinessError> deleteTransferGroup(Transaction leg, List<Transaction> siblings) {
        val legs = new ArrayList<Transaction>();
        legs.add(leg);
        legs.addAll(siblings);
        for (val t : legs) {
            if (service.deleteById(t.id()) instanceof Result.Failure<Void, BusinessError>(BusinessError error)) {
                return Result.failure(error);
            }
        }
        for (val t : legs) MessageBus.submit(new TransactionEvents.Deleted(t));
        return Result.success(legs.stream().map(Transaction::id).toList());
    }

    /**
     * Exclusão em massa (categoria/tag). Ids ausentes são ignorados (idempotente); irmãos de
     * transferência são expandidos para nunca deixar uma perna órfã. Ao final, recalcula o saldo
     * uma única vez por conta distinta — sem eventos por transação individual. O SSE de conta é
     * responsabilidade de quem chama (feature), não deste use case.
     */
    public Result<Void, BusinessError> deleteTransactions(List<UUID> ids) {
        val toDelete = new LinkedHashMap<UUID, Transaction>();
        for (val id : ids) {
            if (toDelete.containsKey(id)) continue;
            if (!(service.findById(id) instanceof Result.Success<Transaction, BusinessError>(var existing))) continue;

            toDelete.put(existing.id(), existing);
            for (val sibling : service.findTransferSiblings(existing)) {
                toDelete.putIfAbsent(sibling.id(), sibling);
            }
        }

        val accountIds = new LinkedHashSet<UUID>();
        for (val tx : toDelete.values()) {
            accountIds.add(tx.accountId());
            service.deleteById(tx.id());
        }

        for (val accountId : accountIds) {
            balanceService.recalculate(accountId);
        }

        return Result.success();
    }

    public Result<Transaction, BusinessError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            return Result.failure(new BusinessError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val groupId = UUID.randomUUID();
        val absAmount = amount.abs();
        val outId = UUID.randomUUID();
        val inId = UUID.randomUUID();

        val outflow = new Transaction(
                outId, "Transferência (saída)", absAmount, date,
                fromAccountId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), date,
                groupId, 1, 2, null, null
        );
        val inflow = new Transaction(
                inId, "Transferência (entrada)", absAmount, date,
                toAccountId, Transaction.Status.CONFIRMED, Transaction.Type.INCOME, CostCenter.VARIAVEL.id(), date,
                groupId, 2, 2, null, null
        );

        val savedOut = service.save(outflow);
        val savedIn = service.save(inflow);
        MessageBus.submit(new TransactionEvents.Created(savedOut));
        MessageBus.submit(new TransactionEvents.Created(savedIn));
        return Result.success(savedOut);
    }

    // ── Vínculo transação↔categoria (F006_TRANSACTION_CATEGORY) ────────────────
    // Tabela à parte de F006_TRANSACTION: Transaction.categoryId não entra no save(Transaction).

    /** Upsert do vínculo; {@code categoryId} nulo apaga a linha. */
    public void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId) {
        categoryService.saveCategory(transactionId, personId, categoryId);
    }

    public void deleteCategory(UUID transactionId) {
        categoryService.deleteCategoryByTransaction(transactionId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        categoryService.reassignCategory(oldCategoryId, newCategoryId, personId);
    }

    // ── Vínculo transação↔tag (F006_TRANSACTION_TAG) ────────────────────────────

    /** Substitui todos os vínculos da transação por {@code tagIds}. */
    public void saveTags(UUID transactionId, UUID personId, List<UUID> tagIds) {
        tagService.replaceTags(transactionId, personId, tagIds);
    }

    public void deleteTags(UUID transactionId) {
        tagService.deleteTagsByTransaction(transactionId);
    }

    /** Aplica {@code categoryId} às demais parcelas do grupo de {@code first} (no-op fora de grupo). */
    public void saveCategoryForGroup(Transaction first, UUID personId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        reads.transactionsInGroup(groupId, personId).stream()
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> saveCategory(t.id(), personId, categoryId));
    }

    /**
     * Categoria de transferência por natureza da perna — a saída (EXPENSE) recebe
     * {@code expenseCategoryId}, a entrada (INCOME) {@code incomeCategoryId} — aplicada a
     * {@code first} e a todas as pernas do grupo. Devolve a categoria de {@code first}, que o
     * chamador usa para montar a resposta.
     */
    public UUID saveTransferCategories(Transaction first, UUID personId, UUID expenseCategoryId, UUID incomeCategoryId) {
        val categoryId = transferCategoryFor(first, expenseCategoryId, incomeCategoryId);
        saveCategory(first.id(), personId, categoryId);

        val groupId = first.groupId();
        if (groupId != null) {
            reads.transactionsInGroup(groupId, personId).stream()
                    .filter(t -> !t.id().equals(first.id()))
                    .forEach(t -> saveCategory(t.id(), personId,
                            transferCategoryFor(t, expenseCategoryId, incomeCategoryId)));
        }
        return categoryId;
    }

    private static UUID transferCategoryFor(Transaction leg, UUID expenseCategoryId, UUID incomeCategoryId) {
        return leg.type() == Transaction.Type.EXPENSE ? expenseCategoryId : incomeCategoryId;
    }

    /** Persiste uma transação já montada pelo chamador. Valida a invariante de cartão. */
    public Result<Transaction, BusinessError> create(Transaction tx) {
        return validateCard(tx.accountId(), tx.cardId()).flatMap(ignored -> {
            val saved = service.save(tx);
            MessageBus.submit(new TransactionEvents.Created(saved));
            return Result.success(saved);
        });
    }

    private Transaction toEntity(UUID id, String description, BigDecimal amount, LocalDate date, UUID accountId,
                                 Transaction.Status status, Transaction.Type type, UUID costCenterId,
                                 @Nullable String notes, @Nullable UUID cardId,
                                 @Nullable UUID groupId, @Nullable Integer installmentNumber, @Nullable Integer totalInstallments) {
        return new Transaction(id, description, amount.abs(), date,
                accountId, status, type, costCenterId, null,
                groupId, installmentOrDefault(installmentNumber), installmentOrDefault(totalInstallments), notes,
                cardId);
    }

    private static int installmentOrDefault(@Nullable Integer value) {
        return value == null ? 1 : value;
    }

    /** No-op when {@code cardId} is absent; otherwise the card must exist and belong to {@code accountId}. */
    private Result<Void, BusinessError> validateCard(UUID accountId, @Nullable UUID cardId) {
        if (cardId == null) return Result.success();
        return creditCardService.findById(cardId).flatMap(card -> validateCardOwner(accountId, card));
    }

    private static Result<Void, BusinessError> validateCardOwner(UUID accountId, CreditCard creditCard) {
        if (!accountId.equals(creditCard.accountId())) {
            return Result.failure(new BusinessError.BusinessRule("CreditCard does not belong to account: %s", creditCard.id()));
        }
        return Result.success();
    }
}
