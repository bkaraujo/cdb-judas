package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link TransactionRepository}; tabela {@code MON_TRANSACTION}.
 * Enums como string, datas como {@code DATE}, timestamps como {@code TIMESTAMP};
 * {@code paymentDate}/{@code groupId}/{@code notes} são opcionais.
 * Categoria e tipo (income/expense) saíram para a camada feature (USER_TRANSACTION).
 */
@NullMarked
public final class TransactionJDBCRepository implements TransactionRepository {

    private static final String COLUMNS =
            "ID, TXT_DESCRIPTION, NUM_SIGNAL, DEC_AMOUNT, TMS_PURCHASE, COD_ACCOUNT, COD_STATUS, "
            + "COD_COST_CENTER, DAT_PAYMENT, GROUP_ID, NUM_INSTALLMENT, NUM_INSTALLMENT_TOTAL, TXT_NOTES, "
            + "TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<Transaction> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_TRANSACTION", this::toTransactions).getOrThrow();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return dataSource.query(
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
        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_TRANSACTION (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.description()),
                    new JDBCPreparedParameter(3, entity.signal()),
                    new JDBCPreparedParameter(4, entity.amount()),
                    new JDBCPreparedParameter(5, Timestamp.valueOf(entity.purchasedAt())),
                    new JDBCPreparedParameter(6, entity.accountId().toString()),
                    new JDBCPreparedParameter(7, entity.status().name()),
                    new JDBCPreparedParameter(8, entity.costCenterId().toString()),
                    new JDBCPreparedParameter(9, paymentDate),
                    new JDBCPreparedParameter(10, groupStr),
                    new JDBCPreparedParameter(11, entity.installmentNumber()),
                    new JDBCPreparedParameter(12, entity.totalInstallments()),
                    new JDBCPreparedParameter(13, entity.notes()),
                    new JDBCPreparedParameter(14, now),
                    new JDBCPreparedParameter(15, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_TRANSACTION SET TXT_DESCRIPTION = ?, NUM_SIGNAL = ?, DEC_AMOUNT = ?, TMS_PURCHASE = ?, "
                            + "COD_ACCOUNT = ?, COD_STATUS = ?, COD_COST_CENTER = ?, DAT_PAYMENT = ?, "
                            + "GROUP_ID = ?, NUM_INSTALLMENT = ?, NUM_INSTALLMENT_TOTAL = ?, TXT_NOTES = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.description()),
                    new JDBCPreparedParameter(2, entity.signal()),
                    new JDBCPreparedParameter(3, entity.amount()),
                    new JDBCPreparedParameter(4, Timestamp.valueOf(entity.purchasedAt())),
                    new JDBCPreparedParameter(5, entity.accountId().toString()),
                    new JDBCPreparedParameter(6, entity.status().name()),
                    new JDBCPreparedParameter(7, entity.costCenterId().toString()),
                    new JDBCPreparedParameter(8, paymentDate),
                    new JDBCPreparedParameter(9, groupStr),
                    new JDBCPreparedParameter(10, entity.installmentNumber()),
                    new JDBCPreparedParameter(11, entity.totalInstallments()),
                    new JDBCPreparedParameter(12, entity.notes()),
                    new JDBCPreparedParameter(13, now),
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
        final int signal = rs.getInt("NUM_SIGNAL").getOrThrow();
        val amount = rs.getBigDecimal("DEC_AMOUNT").getOrThrow();
        @Nullable Timestamp purchaseRaw = rs.getTimestamp("TMS_PURCHASE").getOrThrow();
        val purchasedAt = purchaseRaw == null ? LocalDateTime.now() : purchaseRaw.toLocalDateTime();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").getOrThrow());
        val status = Transaction.Status.valueOf(rs.getString("COD_STATUS").getOrThrow());
        val costCenterId = UUID.fromString(rs.getString("COD_COST_CENTER").getOrThrow());

        @Nullable Date paymentRaw = rs.getDate("DAT_PAYMENT").getOrThrow();
        @Nullable LocalDate paymentDate = paymentRaw == null ? null : paymentRaw.toLocalDate();

        @Nullable String groupRaw = rs.getString("GROUP_ID").getOrThrow();
        @Nullable UUID groupId = (groupRaw == null || groupRaw.isBlank()) ? null : UUID.fromString(groupRaw);

        final int installmentNumber = rs.getInt("NUM_INSTALLMENT").getOrThrow();
        final int totalInstallments = rs.getInt("NUM_INSTALLMENT_TOTAL").getOrThrow();

        @Nullable String notes = rs.getString("TXT_NOTES").getOrThrow();

        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        @Nullable Timestamp updateRaw = rs.getTimestamp("TMS_UPDATED_AT").getOrThrow();
        @Nullable LocalDateTime updatedAt = updateRaw == null ? null : updateRaw.toLocalDateTime();

        return new Transaction(id, description, signal, amount, purchasedAt,
                accountId, status, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, createdAt, updatedAt);
    }
}
