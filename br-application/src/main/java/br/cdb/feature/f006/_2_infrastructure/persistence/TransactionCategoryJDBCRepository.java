package br.cdb.feature.f006._2_infrastructure.persistence;

import br.cdb.feature.f006._0_domain.repository.TransactionCategoryRepository;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador JDBC (H2) da porta {@link TransactionCategoryRepository}; tabela
 * {@code F006_TRANSACTION_CATEGORY}. Join table pura (PK composta {@code COD_TRANSACTION, COD_PERSON}),
 * sem entidade de domínio própria — por isso opera direto sobre {@link DataSource} em vez de estender
 * {@code JDBCRepository} (mesmo molde de {@link TransactionTagJDBCRepository}).
 */
@NullMarked
public final class TransactionCategoryJDBCRepository implements TransactionCategoryRepository {

    private final DataSource datasource = Context.get(DataSource.class);

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        datasource.execute(
                "UPDATE F006_TRANSACTION_CATEGORY SET COD_CATEGORY = ? WHERE COD_CATEGORY = ? AND COD_PERSON = ?",
                JDBCParameter.of(newCategoryId.toString(), oldCategoryId.toString(), personId.toString())
        );
    }

    /** DELETE + INSERT (a tabela só tem as três colunas da chave/valor, nada a preservar num UPDATE). */
    @Override
    public void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId) {
        datasource.transaction(tx -> {
            tx.execute(
                    "DELETE FROM F006_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ? AND COD_PERSON = ?",
                    JDBCParameter.of(transactionId.toString(), personId.toString())
            ).get();

            if (categoryId != null) {
                tx.execute(
                        "INSERT INTO F006_TRANSACTION_CATEGORY (COD_TRANSACTION, COD_PERSON, COD_CATEGORY) VALUES (?, ?, ?)",
                        JDBCParameter.of(transactionId.toString(), personId.toString(), categoryId.toString())
                ).get();
            }

            return Result.success(true);
        });
    }

    @Override
    public Optional<UUID> findCategory(UUID transactionId, UUID personId) {
        return datasource.query(
                "SELECT COD_CATEGORY FROM F006_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ? AND COD_PERSON = ?",
                JDBCParameter.of(transactionId.toString(), personId.toString()),
                TransactionCategoryJDBCRepository::readCategories
        ).stream().findFirst();
    }

    @Override
    public Map<UUID, UUID> findCategoriesByPerson(UUID personId) {
        return datasource.query(
                "SELECT COD_TRANSACTION, COD_CATEGORY FROM F006_TRANSACTION_CATEGORY WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                TransactionCategoryJDBCRepository::readCategoryByTransaction
        );
    }

    @Override
    public void deleteCategoryByTransaction(UUID transactionId) {
        datasource.execute(
                "DELETE FROM F006_TRANSACTION_CATEGORY WHERE COD_TRANSACTION = ?",
                JDBCParameter.of(transactionId.toString())
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
                "SELECT COD_TRANSACTION FROM F006_TRANSACTION_CATEGORY WHERE COD_PERSON = ? AND COD_CATEGORY IN (" + placeholders + ")",
                JDBCParameter.of(params.toArray()),
                TransactionCategoryJDBCRepository::readTransactionIds
        );
    }

    private static List<UUID> readTransactionIds(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) ids.add(UUID.fromString(rs.getString("COD_TRANSACTION").get()));
        return ids;
    }

    private static List<UUID> readCategories(JDBCResultSet rs) {
        val ids = new ArrayList<UUID>();
        while (rs.next().get()) {
            val raw = rs.getString("COD_CATEGORY").get();
            if (raw != null && !raw.isBlank()) ids.add(UUID.fromString(raw));
        }
        return ids;
    }

    private static Map<UUID, UUID> readCategoryByTransaction(JDBCResultSet rs) {
        val byTransaction = new LinkedHashMap<UUID, UUID>();
        while (rs.next().get()) {
            val raw = rs.getString("COD_CATEGORY").get();
            if (raw == null || raw.isBlank()) continue;
            byTransaction.put(UUID.fromString(rs.getString("COD_TRANSACTION").get()), UUID.fromString(raw));
        }
        return byTransaction;
    }
}
