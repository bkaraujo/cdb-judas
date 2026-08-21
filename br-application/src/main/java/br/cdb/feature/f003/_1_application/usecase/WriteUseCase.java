package br.cdb.feature.f003._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.event.CreditCardEvents;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Toda a mutação de cartão da fatia {@code f003} — o par de {@link ReadUseCase}, mesmo arranjo CQRS
 * de {@code f002}/{@code f006}. Context-wired como as demais classes ex-contexto
 * ({@code Context.tryGet(WriteUseCase.class)}, nunca {@code @Inject}); o {@code AccountCardResource}
 * escreve <b>só</b> por aqui. {@code f002} mantém apenas uma projeção somente-leitura do cartão
 * (ver {@code AccountResponse.Card}) — f003 é dono das mutações.
 *
 * <p>As duas camadas convivem no mesmo tipo: métodos de <b>entrada</b> ({@link #createCard},
 * {@link #deleteCard}, {@link #setCardActive}) aplicam a política de usuário ({@link UserGuards},
 * bean CDI resolvido <b>por chamada</b> — {@code @RequestScoped}, nunca guardado em campo) e
 * publicam SSE ({@link AccountStreamEvents#Refresh}, despachado por {@code f999}) e a cascata de
 * exclusão ({@link TransactionsDeleted}); os de <b>engine</b> ({@link #upsert}, {@link #delete})
 * aceitam qualquer comando bem-formado e só publicam o evento de domínio da fatia
 * ({@link CreditCardEvents}).
 *
 * <p>Nota: o nome simples coincide com o {@code WriteUseCase} de {@code f002} — quem precisa dos dois
 * referencia um deles pelo nome completo.
 */
@NullMarked
public class WriteUseCase {

    private static final Pattern LAST4 = Pattern.compile("\\d{4}");

    private final CreditCardService service = Context.tryGet(CreditCardService.class);
    private final AccountService accountService = Context.tryGet(AccountService.class);
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);
    private final TransactionService transactionService = Context.tryGet(TransactionService.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    // ── Cartões (entrada HTTP) ─────────────────────────────────────

    public Result<CreditCard, BusinessError> createCard(CreditCardCommand.Create cmd) {
        return guards().ownsAccount(cmd.accountId()).flatMap(ignored -> upsert(cmd))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(cmd.accountId(), HTTPRequest.personId())));
    }

    public Result<DeletionOutcome, BusinessError> deleteCard(
            UUID accountId, UUID cardId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return guards().ownsCard(accountId, cardId).flatMap(ignored -> {
            if (strategy == null) {
                val count = reads.transactionCount(HTTPRequest.personId(), cardId);
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            }

            return delete(new CreditCardCommand.Delete(cardId, Deletions.toPolicy(strategy, targetId))).map(ids -> {
                // MOVE mantém o cartão de destino na mesma conta: sem re-key de overlay a fazer.
                if (strategy != DeletionStrategy.MOVE) {
                    MessageBus.submit(new TransactionsDeleted(ids));
                }
                MessageBus.submit(new AccountStreamEvents.Refresh(accountId, HTTPRequest.personId()));
                return new DeletionOutcome.Completed();
            });
        });
    }

    public Result<CreditCard, BusinessError> setCardActive(UUID accountId, UUID cardId, boolean active) {
        return guards().ownsCard(accountId, cardId)
                .flatMap(ignored -> upsert(new CreditCardCommand.Update(cardId, active)))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(accountId, HTTPRequest.personId())));
    }

    // ── Cartões (engine — sem política de usuário) ─────────────────

    public Result<CreditCard, BusinessError> upsert(CreditCardCommand.Upsert cmd) {
        return switch (cmd) {
            case CreditCardCommand.Create(var accountId, var last4) -> create(accountId, last4);
            case CreditCardCommand.Update(var id, var active) -> setActive(id, active);
        };
    }

    private Result<CreditCard, BusinessError> create(UUID accountId, String last4) {
        if (!LAST4.matcher(last4).matches()) {
            return Result.failure(new BusinessError.Validation("creditCard.last4InvalidLength"));
        }

        return accountService
                .findById(accountId)
                .flatMap(account -> createCardFor(last4, account))
                .ifSuccess(account -> MessageBus.submit(new CreditCardEvents.Created(account)));
    }

    private Result<CreditCard, BusinessError> createCardFor(String last4, Account account) {
        if (!account.active()) {
            return Result.failure(new BusinessError.BusinessRule("account.inactive", account.id()));
        }

        val duplicate = service.findByAccount(account.id()).stream()
                .anyMatch(c -> c.last4().equals(last4));
        if (duplicate) {
            return Result.failure(new BusinessError.Conflict("creditCard.alreadyRegistered", last4));
        }

        val saved = service.save(new CreditCard(UUID.randomUUID(), last4, account.id(), true));
        return Result.success(saved);
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, BusinessError> delete(CreditCardCommand.Delete command) {
        return service.findById(command.id()).flatMap(card -> switch (command.policy()) {
            case TransactionPolicy.Block ignored -> deleteBlock(card);
            case TransactionPolicy.Move(var targetId) -> deleteMove(card, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(card);
        })
                .ifSuccess(_ -> MessageBus.submit(new CreditCardEvents.Deleted(command.id())));
    }

    private Result<List<UUID>, BusinessError> deleteBlock(CreditCard creditCard) {
        if (!transactionService.findByCard(creditCard.id()).isEmpty()) {
            return Result.failure(new BusinessError.Conflict("creditCard.hasLinkedTransactions", creditCard.id()));
        }
        service.deleteById(creditCard.id());
        return Result.success(List.of());
    }

    private Result<List<UUID>, BusinessError> deleteMove(CreditCard creditCard, UUID targetId) {
        return service.findById(targetId).flatMap(target -> {
            if (target.id().equals(creditCard.id())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("creditCard.transferTargetMustDiffer", targetId));
            }
            if (!target.accountId().equals(creditCard.accountId())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("creditCard.transferTargetMustBeSameAccount", targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("creditCard.transferTargetInactive", targetId));
            }

            val movedIds = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
            transactionService.reassignCard(creditCard.id(), target.id());
            service.deleteById(creditCard.id());
            return Result.success(movedIds);
        });
    }

    private Result<List<UUID>, BusinessError> deletePurge(CreditCard creditCard) {
        val ids = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        service.deleteById(creditCard.id());

        return accountService.findById(creditCard.accountId()).map(account -> {
            balanceService.recalculate(account.id());
            return ids;
        });
    }

    private Result<CreditCard, BusinessError> setActive(UUID id, boolean active) {
        return service.findById(id)
                .map(card -> service.save(new CreditCard(card.id(), card.last4(), card.accountId(), active)))
                .ifSuccess(account -> MessageBus.submit(new CreditCardEvents.Updated(account)));
    }
}
