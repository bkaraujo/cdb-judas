package br.cdb.feature.f006._2_infrastructure.persistence;

import br.cdb.core.persistence.repository.AccountOwnerLookup;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link TransactionRepository}; tabela {@code F006_TRANSACTION}.
 * Enums como string, datas como {@code DATE}, timestamps como {@code TIMESTAMP};
 * {@code paymentDate}/{@code groupId}/{@code notes} são opcionais. {@code COD_PERSON} é derivado da
 * conta ({@link AccountOwnerLookup}) — o comando de criação não carrega personId.
 *
 * <p>{@code Transaction.categoryId}/{@code tagIds} moram em tabelas à parte
 * ({@code F006_TRANSACTION_CATEGORY}/{@code F006_TRANSACTION_TAG}), cada uma com porta e adaptador
 * próprios ({@link TransactionCategoryJDBCRepository}/{@link TransactionTagJDBCRepository}):
 * {@link #map} devolve sempre {@code null}/vazio nesses componentes e {@link #values} não os grava.
 */
@NullMarked
public final class TransactionJDBCRepository extends JDBCRepository<Transaction> implements TransactionRepository {

    public TransactionJDBCRepository() {
        super("F006_TRANSACTION");
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    public void reassignAccount(UUID from, UUID to) {
        datasource.execute(
                "UPDATE F006_TRANSACTION SET COD_ACCOUNT = ? WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(to.toString(), from.toString())
        );
    }

    @Override
    public void reassignCard(UUID from, UUID to) {
        datasource.execute(
                "UPDATE F006_TRANSACTION SET COD_CARD = ? WHERE COD_CARD = ?",
                JDBCParameter.of(to.toString(), from.toString())
        );
    }


    @Override
    public List<Transaction> findAllByPerson(String personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId),
                this::mapList
        );
    }

    @Override
    public Optional<Transaction> findByIdAndPerson(UUID id, String personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE ID = ? AND COD_PERSON = ?",
                JDBCParameter.of(id.toString(), personId),
                this::mapList
        ).stream().findFirst();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(Transaction entity) {
        val payment = entity.paymentDate();
        val group = entity.groupId();
        val card = entity.cardId();
        final @Nullable Date paymentDate = payment == null ? null : Date.valueOf(payment);
        final @Nullable String groupStr = group == null ? null : group.toString();
        final @Nullable String cardStr = card == null ? null : card.toString();
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_PERSON", AccountOwnerLookup.find(datasource, entity.accountId()));
        values.put("TXT_DESCRIPTION", entity.description());
        values.put("NUM_SIGNAL", entity.signal());
        values.put("DEC_AMOUNT", entity.amount());
        values.put("TMS_PURCHASE", Timestamp.valueOf(entity.purchasedAt()));
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("COD_STATUS", entity.status().name());
        values.put("COD_COST_CENTER", entity.costCenterId().toString());
        values.put("DAT_PAYMENT", paymentDate);
        values.put("GROUP_ID", groupStr);
        values.put("NUM_INSTALLMENT", entity.installmentNumber());
        values.put("NUM_INSTALLMENT_TOTAL", entity.totalInstallments());
        values.put("TXT_NOTES", entity.notes());
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        values.put("COD_CARD", cardStr);
        return values;
    }

    @Override
    protected Transaction map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val description = rs.getString("TXT_DESCRIPTION").get();
        val signal = rs.getInt("NUM_SIGNAL").get();
        val amount = rs.getBigDecimal("DEC_AMOUNT").get();
        val purchasedAt = rs.getTimestamp("TMS_PURCHASE").get().toLocalDateTime();
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

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        final @Nullable String cardRaw = rs.getString("COD_CARD").get();
        final @Nullable UUID cardId = (cardRaw == null || cardRaw.isBlank()) ? null : UUID.fromString(cardRaw);

        return new Transaction(id, description, signal, amount, purchasedAt,
                accountId, status, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId);
    }
}
