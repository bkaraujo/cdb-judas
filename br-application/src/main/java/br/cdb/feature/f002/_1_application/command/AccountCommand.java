package br.cdb.feature.f002._1_application.command;

import br.cdb.feature.f000._0_domain.TransactionPolicy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public sealed interface AccountCommand {

    @NullMarked
    sealed interface Upsert extends AccountCommand {}

    @NullMarked
    record Create(
            String name,
            String type,
            boolean active,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay
    ) implements Upsert {}

    @NullMarked
    record Update(
            UUID id,
            String name,
            String type,
            boolean active,
            @Nullable BigDecimal creditLimit,
            @Nullable BigDecimal overdraftLimit,
            @Nullable Integer closingDay,
            @Nullable Integer dueDay
    ) implements Upsert {}

    @NullMarked
    record Delete(
            UUID id,
            TransactionPolicy policy
    ) implements AccountCommand {}

}
