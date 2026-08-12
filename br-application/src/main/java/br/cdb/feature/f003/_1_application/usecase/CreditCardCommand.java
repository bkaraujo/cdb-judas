package br.cdb.feature.f003._1_application.usecase;

import br.cdb.feature.f000._0_domain.TransactionPolicy;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public sealed interface CreditCardCommand {

    @NullMarked
    sealed interface Upsert extends CreditCardCommand {}

    @NullMarked
    record Create(
            UUID accountId,
            String last4
    ) implements Upsert {}

    @NullMarked
    record Update(
            UUID id,
            boolean active
    ) implements Upsert {}

    @NullMarked
    record Delete(
            UUID id,
            TransactionPolicy policy
    ) implements CreditCardCommand {}

}
