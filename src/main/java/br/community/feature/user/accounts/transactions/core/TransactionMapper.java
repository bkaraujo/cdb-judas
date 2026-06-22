package br.community.feature.user.accounts.transactions.core;

import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.feature.user.accounts.transactions.TransactionRequest;
import br.community.feature.user.accounts.transactions.TransactionResponse;
import br.community.feature.user.accounts.transactions.UserTransaction;
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
                t.notes()
        );
    }

    public static TransactionCommand toCommand(UUID accId, TransactionRequest req) {
        return new TransactionCommand(
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.costCenterId(),
                req.status(),
                req.type(),
                req.installments(),
                req.editMode(),
                req.notes()
        );
    }
}
