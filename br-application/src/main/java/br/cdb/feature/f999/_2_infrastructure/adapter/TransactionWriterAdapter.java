package br.cdb.feature.f999._2_infrastructure.adapter;

import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f007._0_domain.model.ImportedTransaction;
import br.cdb.feature.f007._1_application.TransactionWriter;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Único ponto do código que conhece f006 e f007 ao mesmo tempo. Sem CDI: {@code F999Module} publica
 * esta implementação na porta {@link TransactionWriter} do {@link Context}, e quem a consome é a
 * importação ({@code InvoiceImportProcessor}/{@code StatementImportProcessor} em f007), resolvida
 * por chamada. {@code create}/{@code confirmStatus} chamam {@code f006.WriteUseCases#create(Transaction)}/
 * {@code #updateTransactionStatus(UUID, Status, LocalDate)} — os dois métodos que só têm
 * a importação como chamador externo, o contrato f006↔f007 (D3 de {@code .claude/plan.md}).
 */
@NullMarked
public class TransactionWriterAdapter implements TransactionWriter {

    private final WriteUseCases writes = Context.tryGet(WriteUseCases.class);

    @Override
    public Result<UUID, BusinessError> create(ImportedTransaction row) {
        val tx = new Transaction(
                UUID.randomUUID(), row.description(), row.amount(), row.date(),
                row.accountId(), row.status(), row.type(), row.planned(), null,
                row.groupId(), installmentOrDefault(row.installmentNumber()),
                installmentOrDefault(row.totalInstallments()), null, row.cardId());
        return writes.create(tx).map(Transaction::id);
    }

    @Override
    public Result<Void, BusinessError> confirmStatus(UUID transactionId, LocalDate paymentDate) {
        return writes.updateTransactionStatus(transactionId, Status.CONFIRMED, paymentDate)
                .flatMap(ignored -> Result.success());
    }

    private static int installmentOrDefault(@Nullable Integer value) {
        return value == null ? 1 : value;
    }
}
