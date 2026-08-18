package br.cdb.feature.f007;

import br.cdb.core.View;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f007._2_infrastructure.web.ImportResource;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cliente tipado da própria API pública de {@code f007}, para consumo cross-slice, no mesmo papel de
 * {@code f002.F002Api}/{@code f003.F003Api}/{@code f004.F004Api}/{@code f006.F006Api}. Hoje sem
 * consumidor: nenhuma fatia lê a importação de fora dela.
 *
 * <p>Espelha só {@link ImportResource#confirm} — {@code POST /import/preview}
 * ({@link ImportResource#preview}) é {@code multipart/form-data}, e {@link HTTPApi} só monta
 * corpo JSON (Etapa 0 de {@code .claude/plan.md}); montar multipart no {@code HttpClient} do JDK à
 * mão não se justifica para um método sem chamador. Ver "Ponto em aberto" no plano.
 *
 * <p>Context-wired ({@code Context.tryGet(F007Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F007Api {

    public enum StatementType { CREDIT_CARD_INVOICE, BANK_STATEMENT }

    /** Espelha {@code StatementConfirmRequest.Row}. */
    @NullMarked
    public record ConfirmRow(
            String description,
            BigDecimal amount,
            LocalDate date,
            @Nullable LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            Transaction.@Nullable Type transactionType,
            UUID categoryId,
            @Nullable UUID costCenterId,
            @Nullable UUID cardId,
            @Nullable List<UUID> tagIds
    ) implements View {}

    /** Corpo de {@link #confirm} — espelha {@code StatementConfirmRequest}. */
    @NullMarked
    public record ConfirmBody(StatementType type, @Nullable UUID accountId, List<ConfirmRow> rows) {}

    /** Espelha {@code ImportConfirmResponse}. */
    @NullMarked
    public record ConfirmResult(int created, int reconciled, int skipped) implements View {}

    private static HTTPApi internalApi() {
        return Context.get(HTTPApi.class);
    }

    public ConfirmResult confirm(ConfirmBody body) {
        return internalApi().post("/accounts/transactions/import/confirm", body, ConfirmResult.class);
    }
}
