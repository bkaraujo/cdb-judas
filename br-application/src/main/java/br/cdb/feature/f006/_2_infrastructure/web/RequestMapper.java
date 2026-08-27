package br.cdb.feature.f006._2_infrastructure.web;

import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f006.F006Api;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.TransactionCommand;
import br.commons.framework.cdi.Context;
import br.cdb.feature.f006._1_application.usecase.TransactionScope;
import br.cdb.feature.f006._2_infrastructure.web.request.TransactionRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public abstract class RequestMapper {
    private RequestMapper(){}
    public static F006Api.TransactionDto toDto(Transaction t) {
        Nature categoryNature = Nature.EXPENSE;
        if (t.categoryId() != null) {
            categoryNature = Context.get(F005Api.class).natureOf(t.categoryId());
        }
        int signal = t.calculateSignal(categoryNature);
        // Efetiva (pós-estorno) — pode divergir da natureza da categoria quando reversal=true.
        Nature effectiveNature = signal > 0 ? Nature.INCOME : Nature.EXPENSE;

        return new F006Api.TransactionDto(
                t.id(),
                t.description(),
                BigDecimal.valueOf(signal).multiply(t.amount()),
                t.date(),
                t.categoryId(),
                t.accountId(),
                t.status(),
                effectiveNature,
                t.planned(),
                t.paymentDate(),
                t.groupId(),
                t.installmentNumber(),
                t.totalInstallments(),
                t.notes(),
                t.cardId(),
                t.purchaseDate(),
                t.tagIds()
        );
    }

    public static TransactionCommand.Create toCreateCommand(UUID accId, TransactionRequest req) {
        F005Api f005 = Context.get(F005Api.class);
        Nature catNature = req.categoryId() != null ? f005.natureOf(req.categoryId()) : Nature.EXPENSE;
        return new TransactionCommand.Create(
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.planned(),
                req.status(),
                req.type() != catNature,
                req.installments(),
                req.notes(),
                req.cardId()
        );
    }

    public static TransactionCommand.Update toUpdateCommand(UUID txId, UUID accId, TransactionRequest req) {
        F005Api f005 = Context.get(F005Api.class);
        Nature catNature = req.categoryId() != null ? f005.natureOf(req.categoryId()) : Nature.EXPENSE;
        return new TransactionCommand.Update(
                txId,
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.planned(),
                req.status(),
                req.type() != catNature,
                toScope(req.editMode()),
                req.notes(),
                req.cardId()
        );
    }

    /** Qualquer valor além de {@code "FUTURE"} ou {@code "ALL"} (inclusive {@code null}) vira {@link TransactionScope.Single}. */
    public static TransactionScope toScope(@Nullable String mode) {
        if ("ALL".equalsIgnoreCase(mode)) return new TransactionScope.All();
        return "FUTURE".equalsIgnoreCase(mode) ? new TransactionScope.Future() : new TransactionScope.Single();
    }
}
