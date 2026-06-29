package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.transactions.UserTransaction;
import br.community.feature.user.accounts.transactions.UserTransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
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
                JDBCParameter.of(
                        transactionId.toString(),
                        userId.toString()
                ),
                this::toUserTransactions
        ).stream().findFirst();
    }

    @Override
    public List<UserTransaction> findAllByUser(UUID userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TRANSACTION WHERE COD_USER = ?",
                JDBCParameter.of(
                        userId.toString()
                ),
                this::toUserTransactions
        );
    }

    @Override
    public UserTransaction save(UserTransaction entity) {
        val existing = findByTransactionAndUser(entity.transactionId(), entity.userId());
        val now = Timestamp.valueOf(Time.now());

        val categoryStr = entity.categoryId() == null ? null : entity.categoryId().toString();

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_TRANSACTION (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    JDBCParameter.of(
                            entity.transactionId().toString(),
                            entity.userId().toString(),
                            categoryStr,
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE USER_TRANSACTION SET COD_CATEGORY = ?, TMS_UPDATED_AT = ? WHERE COD_TRANSACTION = ? AND COD_USER = ?",
                    JDBCParameter.of(categoryStr,
                            now,
                            entity.transactionId().toString(),
                            entity.userId().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        dataSource.execute(
                "DELETE FROM USER_TRANSACTION WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(
                        transactionId.toString()
                )
        );
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId) {
        dataSource.execute(
                "UPDATE USER_TRANSACTION SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_USER = ?",
                JDBCParameter.of(
                        newCategoryId.toString(),
                        oldCategoryId.toString(),
                        userId.toString()
                )
        );
    }

    private List<UserTransaction> toUserTransactions(JDBCResultSet rs) {
        val list = new ArrayList<UserTransaction>();
        while (rs.next().get()) list.add(toUserTransaction(rs));
        return list;
    }

    private UserTransaction toUserTransaction(JDBCResultSet rs) {
        val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").get());
        val userId = UUID.fromString(rs.getString("COD_USER").get());
        final @Nullable String categoryRaw = rs.getString("COD_CATEGORY").get();
        final @Nullable UUID categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);
        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();
        return new UserTransaction(transactionId, userId, categoryId, createdAt, updatedAt);
    }
}
