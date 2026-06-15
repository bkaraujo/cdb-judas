package br.community.feature.user.accounts.transactions.transfer;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.transactions.TransactionResponse;
import br.community.feature.user.accounts.transactions.core.TransactionMapper;
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
public class TransferResource {

    private final MonetaryContext monetaryContext;
    private final ClosingService closingService;

    @PostMapping("/transactions/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@RequestBody @Valid TransferRequest req) {
        if (closingService.validateDate(req.date()) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
        return switch (monetaryContext.createTransfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var t) -> TransactionMapper.toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

}
