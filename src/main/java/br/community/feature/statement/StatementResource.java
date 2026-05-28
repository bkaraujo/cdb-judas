package br.community.feature.statement;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{accId}/statements/{yyyyMM}")
    public List<StatementItem> detail(
            @PathVariable UUID accId,
            @PathVariable("yyyyMM") String yyyyMM,
            @RequestParam(required = false) @Nullable String status) {
        val ym = YearMonth.parse(yyyyMM, YYYYMM);
        return switch (service.list(accId.toString(), ym.getMonthValue(), ym.getYear(), status)) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @GetMapping("/statements/{yyyyMM}")
    public List<StatementSummary> summary(
            @PathVariable("yyyyMM") String yyyyMM,
            @RequestParam(required = false) @Nullable String status) {
        val ym = YearMonth.parse(yyyyMM, YYYYMM);
        return switch (service.summary(ym.getMonthValue(), ym.getYear(), status)) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}
