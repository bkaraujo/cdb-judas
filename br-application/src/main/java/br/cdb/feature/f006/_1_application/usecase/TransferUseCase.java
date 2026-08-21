package br.cdb.feature.f006._1_application.usecase;

import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entrada da fatia {@code f006} para transferência entre contas — extraída de {@link WriteUseCases},
 * que ficou com o resto da mutação (e continua dona da engine {@code createTransfer}/
 * {@code saveTransferCategories} que esta classe orquestra). Context-wired como as demais classes
 * ex-contexto ({@code Context.tryGet(TransferUseCase.class)}, nunca {@code @Inject}).
 *
 * <p>Camada de <b>entrada</b>, portanto: aplica a política de usuário — guarda de propriedade
 * ({@link UserGuards}) e período fechado — e publica o evento de aplicação
 * ({@link AccountStreamEvents.Refresh} para o SSE, cujo dispatch é de {@code f999}), uma vez por
 * conta envolvida.
 */
@NullMarked
public class TransferUseCase {

    private final WriteUseCases writes = Context.tryGet(WriteUseCases.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    public Result<Transaction, BusinessError> transfer(UUID personId, UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (guards().ownsAccounts(fromAccountId, toAccountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (writes.validateClosing(date) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        // F006_TRANSACTION_CATEGORY.COD_CATEGORY é NOT NULL. Cada perna recebe a categoria de sistema
        // "9. Outros / Transferência" da sua natureza: a saída (EXPENSE) a de despesa, a entrada
        // (INCOME) a de receita. Cobre as duas pernas do grupo, mantendo o 1:1 com F006_TRANSACTION.
        val f005 = Context.get(F005Api.class);
        val expenseCategoryId = f005.transferCategoryId(Nature.EXPENSE);
        val incomeCategoryId = f005.transferCategoryId(Nature.INCOME);
        return writes.createTransfer(fromAccountId, toAccountId, date, amount).map(t -> {
            val categoryId = writes.saveTransferCategories(t, personId, expenseCategoryId, incomeCategoryId);
            MessageBus.submit(new AccountStreamEvents.Refresh(fromAccountId, personId.toString()));
            MessageBus.submit(new AccountStreamEvents.Refresh(toAccountId, personId.toString()));
            return t.withCategory(categoryId);
        });
    }
}
