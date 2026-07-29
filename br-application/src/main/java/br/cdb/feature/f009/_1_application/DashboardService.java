package br.cdb.feature.f009._1_application;

import br.cdb.feature.f000._1_application.InternalApi;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class DashboardService {

    private final InternalApi internalApi;

    /** Corpo mínimo do endpoint público {@code GET /accounts/transactions} (f006) — zero tipo
     *  cross-slice, mesma regra dos antigos ports. {@code type} vem lowercase ("income"/"expense",
     *  ver {@code JsonStorageConfig.transactionEnumModule}). */
    @NullMarked
    private record TransactionDto(LocalDate date, BigDecimal amount, String type) {}

    public Result<MonthlyResult, BusinessError> getMonthlyResult(int month, int year) {
        val start = LocalDate.of(year, month, 1);
        val end = start.plusMonths(1).minusDays(1);

        // Filtro já aplicado no servidor (status/dateFrom/dateTo são suportados pelo endpoint público
        // de f006) — evita trazer o histórico inteiro só pra descartar a maior parte aqui.
        val confirmedThisMonth = List.of(internalApi.get(
                "/accounts/transactions?status=CONFIRMED&dateFrom=" + start + "&dateTo=" + end,
                TransactionDto[].class));

        val incomes = sumWhere(confirmedThisMonth, "income");
        val expenses = sumWhere(confirmedThisMonth, "expense");

        List<HistoricalResult> history = new ArrayList<>();
        for (int w = 1; w <= 5; w++) {
            val wStart = start.plusDays((w - 1) * 7L);
            if (wStart.isAfter(end)) break;
            val wEndBeforeCheck = wStart.plusDays(6);
            val wEnd = wEndBeforeCheck.isAfter(end) ? end : wEndBeforeCheck;

            val wConfirmed = confirmedThisMonth.stream()
                    .filter(t -> !t.date().isBefore(wStart) && !t.date().isAfter(wEnd))
                    .toList();

            history.add(new HistoricalResult("S" + w, sumWhere(wConfirmed, "income"), sumWhere(wConfirmed, "expense")));
            if (wEnd.equals(end)) break;
        }

        return Result.success(MonthlyResult.builder()
                .incomes(incomes)
                .expenses(expenses)
                .result(incomes - expenses)
                .history(history)
                .build());
    }

    /** {@code amount} no DTO vem assinado (negativo pra despesa, espelhando o que o cliente
     *  submeteu) — soma em magnitude, igual ao {@code incomes}/{@code expenses} sempre positivos
     *  de antes da fase 6. */
    private static double sumWhere(List<TransactionDto> transactions, String type) {
        return transactions.stream()
                .filter(t -> type.equals(t.type()))
                .mapToDouble(t -> Math.abs(t.amount().doubleValue()))
                .sum();
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
