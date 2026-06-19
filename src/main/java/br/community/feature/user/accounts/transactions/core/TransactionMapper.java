package br.community.feature.user.accounts.transactions.core;

import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.feature.user.accounts.transactions.TransactionRequest;
import br.community.feature.user.accounts.transactions.TransactionResponse;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class TransactionMapper {

    private TransactionMapper() {}

    public static TransactionResponse toDto(Transaction t) {
        return new TransactionResponse(
                t.id(),
                t.description(),
                t.amount(),
                t.date(),
                t.categoryId(),
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
                req.categoryId(),
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
