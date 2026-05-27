package br.community.feature.dashboard;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardResource {

    private final DashboardService service;

    @GetMapping("/result")
    public DashboardService.MonthlyResult getMonthlyResult(
            @RequestParam int month,
            @RequestParam int year) {
        val result = service.getMonthlyResult(month, year);
        return switch (result) {
            case Result.Success(var data) -> data;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}
