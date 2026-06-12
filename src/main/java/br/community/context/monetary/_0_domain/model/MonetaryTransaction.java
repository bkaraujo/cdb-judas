package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record MonetaryTransaction(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        UUID accountId,
        Status status,
        Type type,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable String notes
) {
    /**
     * Natureza de um {@link MonetaryTransaction}: {@link #EXPENSE} (saída, sinal negativo) ou
     * {@link #INCOME} (entrada, sinal positivo).
     *
     * <p>A forma serializada (JSON persistido e contrato da API) é o nome em minúsculas
     * ({@code "expense"}, {@code "income"}). O mapeamento de/para essa forma vive em
     * {@code JsonStorageConfig} (persistência, Jackson 2) e {@code WebConfig}
     * (camada web, Jackson 3), mantendo o domínio livre de dependências de framework.
     */
    public enum Type {
        EXPENSE, INCOME
    }

    public enum Status {
        SCHEDULED, CONFIRMED, PENDING
    }
}
