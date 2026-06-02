package br.community.feature.user.accounts.core;

import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NullMarked
public record Account(
        UUID id,
        String name,
        BigDecimal balance,
        String type,
        String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        Map<String, Object> additionalInfo,
        BigDecimal currentBalance
) {
    /** Saldo atual = saldo de abertura + todas as transações da conta. Espelha a derivação do
     *  extrato para que todas as telas de leitura (REST e SSE) concordem. */
    public static Account from(MonetaryAccount monetary, List<MonetaryTransaction> transactions) {
        var sum = BigDecimal.ZERO;
        for (var t : transactions) {
            if (monetary.id().equals(t.accountId())) sum = sum.add(t.amount());
        }
        return new Account(
                monetary.id(),
                monetary.name(),
                monetary.balance(),
                Strings.upper(monetary.type().name()),
                monetary.color(),
                monetary.active(),
                monetary.linkedAccountId(),
                monetary.additionalInfo(),
                monetary.balance().add(sum)
        );
    }
}
