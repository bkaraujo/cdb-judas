package br.community.infra.persistence.monetary;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
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
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_TRANSACTION", this::toTransactions);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM MON_TRANSACTION WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toTransactions
        ).stream().findFirst();
    }

    @Override
    public Transaction save(Transaction entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val payment = entity.paymentDate();
        val group = entity.groupId();
        val paymentDate = payment == null ? null : Date.valueOf(payment);
        val groupStr = group == null ? null : group.toString();
        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_TRANSACTION (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of
                            (entity.id().toString(),
                            entity.description(),
                            entity.signal(),
                            entity.amount(),
                            Timestamp.valueOf(entity.purchasedAt()),
                            entity.accountId().toString(),
                            entity.status().name(),
                            entity.costCenterId().toString(),
                            paymentDate,
                            groupStr,
                            entity.installmentNumber(),
                            entity.totalInstallments(),
                            entity.notes(),
                            now,
                            now)
            );
        } else {
            dataSource.execute(
                    "UPDATE MON_TRANSACTION SET TXT_DESCRIPTION = ?, NUM_SIGNAL = ?, DEC_AMOUNT = ?, TMS_PURCHASE = ?, "
                            + "COD_ACCOUNT = ?, COD_STATUS = ?, COD_COST_CENTER = ?, DAT_PAYMENT = ?, "
                            + "GROUP_ID = ?, NUM_INSTALLMENT = ?, NUM_INSTALLMENT_TOTAL = ?, TXT_NOTES = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of(
                            entity.description(),
                            entity.signal(),
                            entity.amount(),
                            Timestamp.valueOf(entity.purchasedAt()),
                            entity.accountId().toString(),
                            entity.status().name(),
                            entity.costCenterId().toString(),
                            paymentDate,
                            groupStr,
                            entity.installmentNumber(),
                            entity.totalInstallments(),
                            entity.notes(),
                            now,
                            entity.id().toString())
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute(
                "DELETE FROM MON_TRANSACTION WHERE ID = ?",
                JDBCParameter.of(id.toString())
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Transaction> toTransactions(JDBCResultSet rs) {
        val transactions = new ArrayList<Transaction>();
        while (rs.next().get()) transactions.add(toTransaction(rs));
        return transactions;
    }

    private Transaction toTransaction(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val description = rs.getString("TXT_DESCRIPTION").get();
        val signal = rs.getInt("NUM_SIGNAL").get();
        val amount = rs.getBigDecimal("DEC_AMOUNT").get();
        val purchaseRaw = rs.getTimestamp("TMS_PURCHASE").get();
        val purchasedAt = purchaseRaw.toLocalDateTime();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val status = Transaction.Status.valueOf(rs.getString("COD_STATUS").get());
        val costCenterId = UUID.fromString(rs.getString("COD_COST_CENTER").get());

        final @Nullable Date paymentRaw = rs.getDate("DAT_PAYMENT").get();
        final @Nullable LocalDate paymentDate = paymentRaw == null ? null : paymentRaw.toLocalDate();

        final @Nullable String groupRaw = rs.getString("GROUP_ID").get();
        final @Nullable UUID groupId = (groupRaw == null || groupRaw.isBlank()) ? null : UUID.fromString(groupRaw);

        val installmentNumber = rs.getInt("NUM_INSTALLMENT").get();
        val totalInstallments = rs.getInt("NUM_INSTALLMENT_TOTAL").get();

        val notes = rs.getString("TXT_NOTES").get();

        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new Transaction(id, description, signal, amount, purchasedAt,
                accountId, status, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, createdAt, updatedAt);
    }
}
