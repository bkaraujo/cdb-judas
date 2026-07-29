package br.cdb.feature.f003._2_infrastructure.persistence;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.infra.persistence.monetary.AccountOwnerLookup;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link CreditCardRepository}; tabela {@code F003_CARD}.
 * Cartão é identificado só pelo last4 e vinculado a uma conta real ({@code COD_ACCOUNT}).
 * {@code COD_PERSON} é derivado da conta ({@link AccountOwnerLookup}) — o comando de criação não
 * carrega personId (o contexto não o conhece).
 */
@NullMarked
public final class CreditCardJDBCRepository extends JDBCRepository<CreditCard> implements CreditCardRepository {

    public CreditCardJDBCRepository() {
        super("F003_CARD");
    }

    @Override
    public Optional<CreditCard> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    public List<CreditCard> findAllByPerson(String personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId),
                this::mapList
        );
    }

    @Override
    public List<CreditCard> findByAccountAndPerson(UUID accountId, String personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_ACCOUNT = ? AND COD_PERSON = ?",
                JDBCParameter.of(accountId.toString(), personId),
                this::mapList
        );
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
    protected Map<String, @Nullable Object> values(CreditCard entity) {
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_PERSON", AccountOwnerLookup.find(datasource, entity.accountId()));
        values.put("TXT_LAST4", entity.last4());
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("FLG_ACTIVE", entity.active() ? "Y" : "N");
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected CreditCard map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val last4 = rs.getString("TXT_LAST4").get();
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        return new CreditCard(id, last4, accountId, active, createdAt, updatedAt);
    }
}
