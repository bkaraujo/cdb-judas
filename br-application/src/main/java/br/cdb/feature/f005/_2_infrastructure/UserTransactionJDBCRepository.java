package br.cdb.feature.f005._2_infrastructure;

import br.cdb.feature.f005._0_domain.UserTransaction;
import br.cdb.feature.f005._0_domain.UserTransactionRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/** Adaptador JDBC (H2) da porta {@link UserTransactionRepository}; tabela {@code PERSON_TRANSACTION}. */
@NullMarked
public final class UserTransactionJDBCRepository extends JDBCRepository<UserTransaction> implements UserTransactionRepository {

    public UserTransactionJDBCRepository() {
        super("PERSON_TRANSACTION");
    }

    @Override
    public Optional<UserTransaction> findByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId) {
        return findById(personId.toString(), accountId.toString(), transactionId.toString());
    }

    @Override
    public List<UserTransaction> findAllByPerson(UUID personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                this::mapList
        );
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM PERSON_TRANSACTION WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    @Override
    public void deleteByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId) {
        deleteById(personId.toString(), accountId.toString(), transactionId.toString());
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        datasource.execute(
                "UPDATE PERSON_TRANSACTION SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_PERSON = ?",
                JDBCParameter.of(newCategoryId.toString(), oldCategoryId.toString(), personId.toString())
        );
    }

    @Override
    public void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId) {
        datasource.execute(
                "UPDATE PERSON_TRANSACTION SET COD_ACCOUNT = ? WHERE COD_ACCOUNT = ? AND COD_PERSON = ?",
                JDBCParameter.of(newAccountId.toString(), oldAccountId.toString(), personId.toString())
        );
    }

    @Override
    public void deleteByAccountAndPerson(UUID accountId, UUID personId) {
        datasource.execute(
                "DELETE FROM PERSON_TRANSACTION WHERE COD_ACCOUNT = ? AND COD_PERSON = ?",
                JDBCParameter.of(accountId.toString(), personId.toString())
        );
    }

    @Override
    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();

        val placeholders = categoryIds.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        val params = new ArrayList<Object>();
        params.add(personId.toString());
        categoryIds.forEach(id -> params.add(id.toString()));

        return datasource.query(
                "SELECT COD_TRANSACTION FROM PERSON_TRANSACTION WHERE COD_PERSON = ? AND COD_CATEGORY IN (" + placeholders + ")",
                JDBCParameter.of(params.toArray()),
                UserTransactionJDBCRepository::readTransactionIds
        );
    }

    private static List<UUID> readTransactionIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_TRANSACTION").get()));
        return ids;
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(UserTransaction entity) {
        val categoryStr = entity.categoryId() == null ? null : entity.categoryId().toString();
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_TRANSACTION", entity.transactionId().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("COD_CATEGORY", categoryStr);
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected UserTransaction map(JDBCResultSet rs) {
        val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").get());
        val personId = UUID.fromString(rs.getString("COD_PERSON").get());
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());

        val categoryRaw = rs.getString("COD_CATEGORY").get();
        val categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();
        return new UserTransaction(transactionId, personId, accountId, categoryId, createdAt, updatedAt);
    }
}
