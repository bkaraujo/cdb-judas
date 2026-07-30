package br.cdb.feature.f003._1_application;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.command.CreditCardCommand;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Use case da fatia {@code f003} (cards) — extraído de {@code f002.AccountUseCase}: f003 é dono
 * das mutações de cartão, f002 mantém só uma projeção somente-leitura (ver
 * {@code AccountResponse.Card}). Zero fatias irmãs: dependências são só f000 (kernel) e o contexto
 * monetário via Facade.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class CardUseCase {

    private final CreditCardUseCase ucCreditCard = Registry.tryGet(CreditCardUseCase.class);
    private final ReadUseCases reads = Registry.tryGet(ReadUseCases.class);

    private final UserGuards guards;

    public Result<List<CreditCard>, BusinessError> cards(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucCreditCard.list(accountId, HTTPRequest.personId()));
    }

    public Result<CreditCard, BusinessError> createCard(CreditCardCommand.Create cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> ucCreditCard.upsert(cmd))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(cmd.accountId(), HTTPRequest.personId())));
    }

    public Result<DeletionOutcome, BusinessError> deleteCard(
            UUID accountId, UUID cardId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return guards.ownsCard(accountId, cardId).flatMap(ignored -> {
            if (strategy == null) {
                val count = (int) allTransactions().stream().filter(t -> cardId.equals(t.cardId())).count();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            }

            return ucCreditCard.delete(new CreditCardCommand.Delete(cardId, Deletions.toPolicy(strategy, targetId))).map(ids -> {
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
        return guards.ownsCard(accountId, cardId)
                .flatMap(ignored -> ucCreditCard.upsert(new CreditCardCommand.Update(cardId, active)))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(accountId, HTTPRequest.personId())));
    }

    private List<Transaction> allTransactions() {
        return reads.transactions(HTTPRequest.personId()).getOrElse(List.of());
    }
}
