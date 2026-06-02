package br.community.feature.user.accounts;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.statement.StatementItem;
import br.community.feature.user.accounts.statement.StatementService;
import br.community.feature.user.accounts.statement.StatementSummary;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatementResource {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private final StatementService service;

    @GetMapping("/{accountID}/statements/{yyyyMM}")
    public List<StatementItem> detail(
            @PathVariable UUID accountID,
            @PathVariable("yyyyMM") String yyyyMM,
            @RequestParam(required = false) @Nullable String status
    ) {
        val ym = YearMonth.parse(yyyyMM, YYYYMM);
        return switch (service.list(accountID.toString(), ym.getMonthValue(), ym.getYear(), status)) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @GetMapping("/statements/{yyyyMM}")
    public List<StatementSummary> summary(
            @PathVariable("yyyyMM") String yyyyMM,
            @RequestParam(required = false) @Nullable String status
    ) {
        val ym = YearMonth.parse(yyyyMM, YYYYMM);
        return switch (service.summary(ym.getMonthValue(), ym.getYear(), status)) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}
