package br.community.feature.creditcards;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.command.CreditCardCommand;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/credit-cards", produces = MediaType.APPLICATION_JSON_VALUE)
public class CreditCardResource {

    private final MonetaryContext monetaryContext;

    @GetMapping
    public List<CreditCard> list(@RequestParam(required = false) UUID accountId) {
        val result = accountId == null
                ? monetaryContext.listCreditCards()
                : monetaryContext.listCreditCardsByAccount(accountId);
        return switch (result) {
            case Result.Success(var cards) -> cards.stream().map(this::toDto).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCard create(@RequestBody @Valid CreditCardRequest req) {
        return switch (monetaryContext.createCreditCard(toCommand(req))) {
            case Result.Success(var c) -> toDto(c);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public CreditCard update(@PathVariable UUID id, @RequestBody @Valid CreditCardRequest req) {
        return switch (monetaryContext.updateCreditCard(id, toCommand(req))) {
            case Result.Success(var c) -> toDto(c);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        switch (monetaryContext.deleteCreditCard(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }

    private CreditCard toDto(MonetaryAccount c) {
        return new CreditCard(
                c.id(),
                c.linkedAccountId(),
                (String)c.additionalInfo().get("last4"),
                c.balance(),
                (Integer)c.additionalInfo().get("closingDay"),
                (Integer)c.additionalInfo().get("dueDay"),
                c.color(),
                c.active()
        );
    }

    private CreditCardCommand toCommand(CreditCardRequest req) {
        return new CreditCardCommand(
                req.accountId(),
                req.name(),
                req.last4(),
                req.limit(),
                req.closingDay(),
                req.dueDay(),
                req.color(),
                req.active()
        );
    }
}
