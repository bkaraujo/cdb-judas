package br.cdb.feature.f006._2_infrastructure.persistence;

import br.cdb.feature.f006._0_domain.UserTransaction;
import br.cdb.feature.f006._0_domain.UserTransactionRepository;
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

/**
 * Adaptador JDBC (H2) da porta {@link UserTransactionRepository}; tabela
 * {@code F005_TRANSACTION_CATEGORY} — vínculo transação↔categoria, PK {@code (COD_TRANSACTION, COD_PERSON)}.
 * Conta não é mais coluna própria (nativa de {@code F006_TRANSACTION} desde a fusão dos contextos).
 */
@NullMarked
public final class UserTransactionJDBCRepository extends JDBCRepository<UserTransaction> implements UserTransactionRepository {

    public UserTransactionJDBCRepository() {
        super("F005_TRANSACTION_CATEGORY");
    }

    @Override
    public Optional<UserTransaction> findByTransactionAndPerson(UUID transactionId, UUID personId) {
        return findById(transactionId.toString(), personId.toString());
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
                "DELETE FROM F005_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
        );
    }

    @Override
    public void deleteByTransactionAndPerson(UUID transactionId, UUID personId) {
        deleteById(transactionId.toString(), personId.toString());
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        datasource.execute(
                "UPDATE F005_TRANSACTION_CATEGORY SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_PERSON = ?",
                JDBCParameter.of(newCategoryId.toString(), oldCategoryId.toString(), personId.toString())
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
                "SELECT COD_TRANSACTION FROM F005_TRANSACTION_CATEGORY WHERE COD_PERSON = ? AND COD_CATEGORY IN (" + placeholders + ")",
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
    protected Map<String, @Nullable Object> values(UserTransaction entity) {
        val categoryStr = entity.categoryId() == null ? null : entity.categoryId().toString();
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_TRANSACTION", entity.transactionId().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("COD_CATEGORY", categoryStr);
        return values;
    }

    @Override
    protected UserTransaction map(JDBCResultSet rs) {
        val transactionId = UUID.fromString(rs.getString("COD_TRANSACTION").get());
        val personId = UUID.fromString(rs.getString("COD_PERSON").get());

        val categoryRaw = rs.getString("COD_CATEGORY").get();
        val categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);

        return new UserTransaction(transactionId, personId, categoryId, null, null);
    }
}
