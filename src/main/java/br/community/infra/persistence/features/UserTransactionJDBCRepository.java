package br.community.infra.persistence.features;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.accounts.transactions.UserTransaction;
import br.community.feature.user.accounts.transactions.UserTransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link UserTransactionRepository}; tabela {@code USER_TRANSACTION}. */
@NullMarked
public final class UserTransactionJDBCRepository extends JDBCRepository<UserTransaction> implements UserTransactionRepository {

    public UserTransactionJDBCRepository() {
        super("USER_TRANSACTION");
    }

    @Override
    public Optional<UserTransaction> findByTransactionAccountAndUser(UUID transactionId, UUID accountId, UUID userId) {
        return findById(userId.toString(), accountId.toString(), transactionId.toString());
    }

    @Override
    public List<UserTransaction> findAllByUser(UUID userId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_USER = ?",
                JDBCParameter.of(userId.toString()),
                this::mapList
        );
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM USER_TRANSACTION WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    @Override
    public void deleteByTransactionAccountAndUser(UUID transactionId, UUID accountId, UUID userId) {
        deleteById(userId.toString(), accountId.toString(), transactionId.toString());
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId) {
        datasource.execute(
                "UPDATE USER_TRANSACTION SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_USER = ?",
                JDBCParameter.of(newCategoryId.toString(), oldCategoryId.toString(), userId.toString())
        );
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(UserTransaction entity) {
        final @Nullable String categoryStr = entity.categoryId() == null ? null : entity.categoryId().toString();
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_TRANSACTION", entity.transactionId().toString());
        values.put("COD_USER", entity.userId().toString());
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("COD_CATEGORY", categoryStr);
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected UserTransaction map(JDBCResultSet rs) {
        val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").get());
        val userId = UUID.fromString(rs.getString("COD_USER").get());
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());

        final @Nullable String categoryRaw = rs.getString("COD_CATEGORY").get();
        final @Nullable UUID categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();
        return new UserTransaction(transactionId, userId, accountId, categoryId, createdAt, updatedAt);
    }
}
