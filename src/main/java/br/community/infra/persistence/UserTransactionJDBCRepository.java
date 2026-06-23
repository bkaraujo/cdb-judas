package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.transactions.UserTransaction;
import br.community.feature.user.accounts.transactions.UserTransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link UserTransactionRepository}; tabela {@code USER_TRANSACTION}. */
@NullMarked
public final class UserTransactionJDBCRepository implements UserTransactionRepository {

    private static final String COLUMNS = "COD_TRANSACTION, COD_USER, COD_CATEGORY, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public Optional<UserTransaction> findByTransactionAndUser(UUID transactionId, UUID userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TRANSACTION WHERE COD_TRANSACTION = ? AND COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, transactionId.toString()),
                        new JDBCPreparedParameter(2, userId.toString())),
                this::toUserTransactions
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public List<UserTransaction> findAllByUser(UUID userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TRANSACTION WHERE COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, userId.toString())),
                this::toUserTransactions
        ).getOrThrow();
    }

    @Override
    public UserTransaction save(UserTransaction entity) {
        val existing = findByTransactionAndUser(entity.transactionId(), entity.userId());
        val now = Timestamp.valueOf(LocalDateTime.now());

        @Nullable String categoryStr = entity.categoryId() == null ? null : entity.categoryId().toString();

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_TRANSACTION (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.transactionId().toString()),
                    new JDBCPreparedParameter(2, entity.userId().toString()),
                    new JDBCPreparedParameter(3, categoryStr),
                    new JDBCPreparedParameter(4, now),
                    new JDBCPreparedParameter(5, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE USER_TRANSACTION SET COD_CATEGORY = ?, TMS_UPDATED_AT = ? WHERE COD_TRANSACTION = ? AND COD_USER = ?",
                    new JDBCPreparedParameter(1, categoryStr),
                    new JDBCPreparedParameter(2, now),
                    new JDBCPreparedParameter(3, entity.transactionId().toString()),
                    new JDBCPreparedParameter(4, entity.userId().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        dataSource.execute("DELETE FROM USER_TRANSACTION WHERE COD_TRANSACTION = ?",
                new JDBCPreparedParameter(1, transactionId.toString())).getOrThrow();
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId) {
        dataSource.execute(
                "UPDATE USER_TRANSACTION SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_USER = ?",
                new JDBCPreparedParameter(1, newCategoryId.toString()),
                new JDBCPreparedParameter(2, oldCategoryId.toString()),
                new JDBCPreparedParameter(3, userId.toString())
        ).getOrThrow();
    }

    private List<UserTransaction> toUserTransactions(JDBCResultSet rs) {
        val list = new ArrayList<UserTransaction>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) list.add(toUserTransaction(rs));
        return list;
    }

    private UserTransaction toUserTransaction(JDBCResultSet rs) {
        val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").getOrThrow());
        val userId = UUID.fromString(rs.getString("COD_USER").getOrThrow());
        @Nullable String categoryRaw = rs.getString("COD_CATEGORY").getOrThrow();
        @Nullable UUID categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);
        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        @Nullable Timestamp updateRaw = rs.getTimestamp("TMS_UPDATED_AT").getOrThrow();
        @Nullable LocalDateTime updatedAt = updateRaw == null ? null : updateRaw.toLocalDateTime();
        return new UserTransaction(transactionId, userId, categoryId, createdAt, updatedAt);
    }
}
