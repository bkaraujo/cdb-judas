package br.cdb.feature.f002._2_infrastructure.web.response;

import br.cdb.context.monetary._0_domain.model.Balance;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/** Contrato da API de saldo ({@code accountId}/{@code balance}) — o frontend lê {@code b.balance}; o domínio agora carrega o {@code Account} inteiro e não deve vazar na resposta. */
@NullMarked
public record BalanceResponse(
        UUID accountId,
        YearMonth period,
        BigDecimal balance
) {
    public static BalanceResponse of(Balance balance) {
        return new BalanceResponse(balance.account().id(), balance.period(), balance.value());
    }
}
