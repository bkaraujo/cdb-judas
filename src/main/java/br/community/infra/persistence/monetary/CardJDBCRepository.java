package br.community.infra.persistence.monetary;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.CreditCard;
import br.community.context.monetary._0_domain.repository.CardRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link CardRepository}; tabela {@code MON_CARD}.
 * Cartão é identificado só pelo last4 e vinculado a uma conta real ({@code COD_ACCOUNT}).
 */
@NullMarked
public final class CardJDBCRepository extends JDBCRepository<CreditCard> implements CardRepository {

    public CardJDBCRepository() {
        super("MON_CARD");
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
