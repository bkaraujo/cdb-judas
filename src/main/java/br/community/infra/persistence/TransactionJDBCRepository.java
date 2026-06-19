package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link TransactionRepository}; tabela {@code MON_TRANSACTION}.
 * Enums como string, datas como {@code DATE}; {@code paymentDate}/{@code groupId}/{@code notes}
 * são opcionais. Os filtros (por conta, grupo, status) ficam no serviço sobre {@link #findAll()}.
 */
@NullMarked
public final class TransactionJDBCRepository implements TransactionRepository {

    private static final String COLUMNS =
            "ID, TXT_DESCRIPTION, DEC_AMOUNT, DAT_DATE, COD_CATEGORY, COD_ACCOUNT, TXT_STATUS, TXT_TYPE, "
            + "COD_COST_CENTER, DAT_PAYMENT, COD_GROUP, NUM_INSTALLMENT, NUM_TOTAL_INSTALLMENTS, TXT_NOTES";

    private final DataSource dataSource;

    public TransactionJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM MON_TRANSACTION", this::toTransactions).getOrThrow();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_TRANSACTION WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toTransactions
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Transaction save(Transaction entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val payment = entity.paymentDate();
        val group = entity.groupId();
        @Nullable Date paymentDate = payment == null ? null : Date.valueOf(payment);
        @Nullable String groupStr = group == null ? null : group.toString();
        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_TRANSACTION (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.description()),
                    new JDBCPreparedParameter(3, entity.amount()),
                    new JDBCPreparedParameter(4, Date.valueOf(entity.date())),
                    new JDBCPreparedParameter(5, entity.categoryId().toString()),
                    new JDBCPreparedParameter(6, entity.accountId().toString()),
                    new JDBCPreparedParameter(7, entity.status().name()),
                    new JDBCPreparedParameter(8, entity.type().name()),
                    new JDBCPreparedParameter(9, entity.costCenterId().toString()),
                    new JDBCPreparedParameter(10, paymentDate),
                    new JDBCPreparedParameter(11, groupStr),
                    new JDBCPreparedParameter(12, entity.installmentNumber()),
                    new JDBCPreparedParameter(13, entity.totalInstallments()),
                    new JDBCPreparedParameter(14, entity.notes())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_TRANSACTION SET TXT_DESCRIPTION = ?, DEC_AMOUNT = ?, DAT_DATE = ?, COD_CATEGORY = ?, "
                            + "COD_ACCOUNT = ?, TXT_STATUS = ?, TXT_TYPE = ?, COD_COST_CENTER = ?, DAT_PAYMENT = ?, "
                            + "COD_GROUP = ?, NUM_INSTALLMENT = ?, NUM_TOTAL_INSTALLMENTS = ?, TXT_NOTES = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.description()),
                    new JDBCPreparedParameter(2, entity.amount()),
                    new JDBCPreparedParameter(3, Date.valueOf(entity.date())),
                    new JDBCPreparedParameter(4, entity.categoryId().toString()),
                    new JDBCPreparedParameter(5, entity.accountId().toString()),
                    new JDBCPreparedParameter(6, entity.status().name()),
                    new JDBCPreparedParameter(7, entity.type().name()),
                    new JDBCPreparedParameter(8, entity.costCenterId().toString()),
                    new JDBCPreparedParameter(9, paymentDate),
                    new JDBCPreparedParameter(10, groupStr),
                    new JDBCPreparedParameter(11, entity.installmentNumber()),
                    new JDBCPreparedParameter(12, entity.totalInstallments()),
                    new JDBCPreparedParameter(13, entity.notes()),
                    new JDBCPreparedParameter(14, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_TRANSACTION WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Transaction> toTransactions(JDBCResultSet rs) {
        val transactions = new ArrayList<Transaction>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) transactions.add(toTransaction(rs));
        return transactions;
    }

    private Transaction toTransaction(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").getOrThrow());
        val description = rs.getString("TXT_DESCRIPTION").getOrThrow();
        val amount = rs.getBigDecimal("DEC_AMOUNT").getOrThrow();
        val date = rs.getDate("DAT_DATE").getOrThrow().toLocalDate();
        val categoryId = UUID.fromString(rs.getString("COD_CATEGORY").getOrThrow());
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").getOrThrow());
        val status = Transaction.Status.valueOf(rs.getString("TXT_STATUS").getOrThrow());
        val type = Transaction.Type.valueOf(rs.getString("TXT_TYPE").getOrThrow());
        val costCenterId = UUID.fromString(rs.getString("COD_COST_CENTER").getOrThrow());

        @Nullable Date paymentRaw = rs.getDate("DAT_PAYMENT").getOrThrow();
        @Nullable LocalDate paymentDate = paymentRaw == null ? null : paymentRaw.toLocalDate();

        @Nullable String groupRaw = rs.getString("COD_GROUP").getOrThrow();
        @Nullable UUID groupId = (groupRaw == null || groupRaw.isBlank()) ? null : UUID.fromString(groupRaw);

        final int installmentNumber = rs.getInt("NUM_INSTALLMENT").getOrThrow();
        final int totalInstallments = rs.getInt("NUM_TOTAL_INSTALLMENTS").getOrThrow();

        @Nullable String notes = rs.getString("TXT_NOTES").getOrThrow();

        return new Transaction(id, description, amount, date, categoryId, accountId, status, type,
                costCenterId, paymentDate, groupId, installmentNumber, totalInstallments, notes);
    }
}
