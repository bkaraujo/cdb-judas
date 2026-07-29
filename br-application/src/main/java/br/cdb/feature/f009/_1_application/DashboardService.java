package br.cdb.feature.f009._1_application;

import br.cdb.feature.f006._1_application.usecase.TransactionUseCase;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionUseCase ucTransaction = Registry.tryGet(TransactionUseCase.class);

    public Result<MonthlyResult, BusinessError> getMonthlyResult(String personId, int month, int year) {
        return ucTransaction.transactions(personId)
                .map(all -> {
                    val start = LocalDate.of(year, month, 1);
                    val end = start.plusMonths(1).minusDays(1);

                    val confirmedThisMonth = all.stream()
                            .filter(t -> t.status().name().equals("CONFIRMED"))
                            .filter(t -> !t.date().isBefore(start) && !t.date().isAfter(end))
                            .toList();

                    val incomes = confirmedThisMonth.stream()
                            .filter(t -> t.signal() > 0)
                            .mapToDouble(t -> t.amount().doubleValue())
                            .sum();

                    val expenses = confirmedThisMonth.stream()
                            .filter(t -> t.signal() < 0)
                            .mapToDouble(t -> t.amount().doubleValue())
                            .sum();

                    List<HistoricalResult> history = new ArrayList<>();
                    for (int w = 1; w <= 5; w++) {
                        val wStart = start.plusDays((w - 1) * 7L);
                        if (wStart.isAfter(end)) break;
                        val wEndBeforeCheck = wStart.plusDays(6);
                        val wEnd = wEndBeforeCheck.isAfter(end) ? end : wEndBeforeCheck;

                        val wConfirmed = confirmedThisMonth.stream()
                                .filter(t -> !t.date().isBefore(wStart) && !t.date().isAfter(wEnd))
                                .toList();

                        val wRec = wConfirmed.stream()
                                .filter(t -> t.signal() > 0)
                                .mapToDouble(t -> t.amount().doubleValue()).sum();

                        val wDes = wConfirmed.stream()
                                .filter(t -> t.signal() < 0)
                                .mapToDouble(t -> t.amount().doubleValue()).sum();

                        history.add(new HistoricalResult("S" + w, wRec, wDes));
                        if (wEnd.equals(end)) break;
                    }

                    return MonthlyResult.builder()
                            .incomes(incomes)
                            .expenses(expenses)
                            .result(incomes - expenses)
                            .history(history)
                            .build();
                });
    }

    @NullMarked
    @Builder
    public record MonthlyResult(double incomes, double expenses, double result, List<HistoricalResult> history) {
        public double historyMax() {
            double max = 1.0;
            if (history == null) return max;
            for (val h : history) {
                if (h.incomes() > max) max = h.incomes();
                if (h.expenses() > max) max = h.expenses();
            }
            return max;
        }
    }

    @NullMarked
    public record HistoricalResult(String month, double incomes, double expenses) {}
}
