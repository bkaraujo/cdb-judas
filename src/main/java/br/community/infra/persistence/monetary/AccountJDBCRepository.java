package br.community.infra.persistence.monetary;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.repository.AccountRepository;
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
 * Adaptador JDBC (H2) da porta {@link AccountRepository}. Mapeia {@link Account} para
 * {@code MON_ACCOUNT} (metadados globais: nome, tipo, ativo). Saldo, cor e os dados de cartão
 * (conta vinculada, limites) são geridos pela feature em {@code USER_ACCOUNT}.
 */
@NullMarked
public final class AccountJDBCRepository extends JDBCRepository<Account> implements AccountRepository {

    public AccountJDBCRepository() {
        super("MON_ACCOUNT");
    }

    @Override
    public Optional<Account> findById(UUID id) {
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
    protected Map<String, @Nullable Object> values(Account entity) {
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("TXT_NAME", entity.name());
        values.put("TXT_TYPE", AccountTypeMapper.toId(entity.type()));
        values.put("FLG_ACTIVE", entity.active() ? "Y" : "N");
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected Account map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val name = rs.getString("TXT_NAME").get();
        val type = AccountTypeMapper.fromId(rs.getString("TXT_TYPE").get());
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        return new Account(id, name, type, active, createdAt, updatedAt);
    }
}
