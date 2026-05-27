package br.community.feature.operations.payables;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/operations/payables", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountPayableResource {

    private final AccountPayableService service;

    @GetMapping
    public List<AccountPayable> list(@RequestParam String type) {
        val result = service.listByType(type);
        return switch (result) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PutMapping("/{id}/confirm")
    public AccountPayable confirm(@PathVariable UUID id, @RequestBody @Valid ConfirmRequest req) {
        val result = service.confirm(id, req.paymentDate());
        return switch (result) {
            case Result.Success(var cp) -> cp;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }


}
