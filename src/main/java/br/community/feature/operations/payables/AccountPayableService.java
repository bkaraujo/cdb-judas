package br.community.feature.operations.payables;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@NullMarked
@RequiredArgsConstructor
public class AccountPayableService {

    private final MonetaryContext monetaryContext;

    public Result<List<AccountPayable>, DomainError> listByType(String type) {
        val transactionType = "PAYABLE".equalsIgnoreCase(type) ? "expense" : "income";
        return monetaryContext.listPendingTransactions()
                .map(pending -> pending.stream()
                        .filter(t -> transactionType.equalsIgnoreCase(t.type()))
                        .map(t -> new AccountPayable(
                                t.id(), t.description(), t.date(), t.amount().abs(),
                                t.accountId(), t.categoryId(), t.status(), Strings.upper(type)
                        ))
                        .toList());
    }

    public Result<AccountPayable, DomainError> confirm(UUID id, LocalDate paymentDate) {
        return monetaryContext.updateTransactionStatus(id, "confirmed", paymentDate)
                .map(t -> new AccountPayable(
                        t.id(), t.description(), t.date(), t.amount().abs(), t.accountId(), t.categoryId(), t.status(),
                        "expense".equalsIgnoreCase(t.type()) ? "PAYABLE" : "RECEIVABLE"
                ));
    }


}
