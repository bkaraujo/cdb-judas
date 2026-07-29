package br.cdb.feature.f006._2_infrastructure.web;

import br.cdb.feature.f006._0_domain.UserTransaction;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.command.TransactionCommand;
import br.cdb.feature.f006._1_application.command.TransactionScope;
import br.cdb.feature.f006._2_infrastructure.web.request.TransactionRequest;
import br.cdb.feature.f006._2_infrastructure.web.response.TransactionResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public final class TransactionMapper {

    private TransactionMapper() {}

    public static TransactionResponse toDto(Transaction t, @Nullable UserTransaction ut) {
        return new TransactionResponse(
                t.id(),
                t.description(),
                BigDecimal.valueOf(t.signal()).multiply(t.amount()),
                t.date(),
                ut != null ? ut.categoryId() : null,
                t.accountId(),
                t.status(),
                t.type(),
                t.costCenterId(),
                t.paymentDate(),
                t.groupId(),
                t.installmentNumber(),
                t.totalInstallments(),
                t.notes(),
                t.cardId()
        );
    }

    public static TransactionCommand.Create toCreateCommand(UUID accId, TransactionRequest req) {
        return new TransactionCommand.Create(
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.costCenterId(),
                req.status(),
                req.type(),
                req.installments(),
                req.notes(),
                req.cardId()
        );
    }

    public static TransactionCommand.Update toUpdateCommand(UUID txId, UUID accId, TransactionRequest req) {
        return new TransactionCommand.Update(
                txId,
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.costCenterId(),
                req.status(),
                req.type(),
                toScope(req.editMode()),
                req.notes(),
                req.cardId()
        );
    }

    /** Qualquer valor além de {@code "FUTURE"} (inclusive {@code null}) vira {@link TransactionScope.Single}. */
    public static TransactionScope toScope(@Nullable String mode) {
        return "FUTURE".equalsIgnoreCase(mode) ? new TransactionScope.Future() : new TransactionScope.Single();
    }
}
