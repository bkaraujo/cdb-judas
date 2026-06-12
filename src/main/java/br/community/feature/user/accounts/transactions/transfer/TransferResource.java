package br.community.feature.user.accounts.transactions.transfer;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.transactions.TransactionResponse;
import br.community.feature.user.accounts.transactions.core.AbstractResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransferResource extends AbstractResource {

    private final MonetaryContext monetaryContext;

    @PostMapping("/transactions/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@RequestBody @Valid TransferRequest req) {
        return switch (monetaryContext.createTransfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

}
