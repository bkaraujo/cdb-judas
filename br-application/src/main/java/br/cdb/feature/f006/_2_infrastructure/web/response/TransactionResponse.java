package br.cdb.feature.f006._2_infrastructure.web.response;

import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Status;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NullMarked
public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        @Nullable UUID categoryId,
        UUID accountId,
        Status status,
        Nature type,
        boolean planned,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable String notes,
        @Nullable UUID cardId,
        @Nullable LocalDate purchaseDate,
        List<UUID> tagIds
) {}
