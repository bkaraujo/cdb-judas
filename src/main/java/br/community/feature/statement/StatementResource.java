package br.community.feature.statement;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/statement", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatementResource {

    private final StatementService service;

    @GetMapping
    public List<StatementItem> list(
            @RequestParam("accountId") String accountId,
            @RequestParam("month") int month,
            @RequestParam("year") int year,
            @RequestParam(required = false) @Nullable String status
    ) {
        val result = service.list(accountId, month, year, status);
        return switch (result) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}
