package br.community.infra.persistence.monetary;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.AccountLimit;
import br.community.context.monetary._0_domain.repository.AccountLimitRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link AccountLimitRepository}; tabela {@code MON_ACCOUNT_LIMIT}.
 * Linha única por conta (PK {@code COD_ACCOUNT}): limite de crédito, cheque especial e ciclo de
 * fatura (dias de fechamento/vencimento), compartilhados por todos os cartões da conta.
 */
@NullMarked
public final class AccountLimitJDBCRepository extends JDBCRepository<AccountLimit> implements AccountLimitRepository {

    public AccountLimitJDBCRepository() {
        super("MON_ACCOUNT_LIMIT");
    }

    @Override
    public Optional<AccountLimit> findById(UUID accountId) {
        return findById(accountId.toString());
    }

    @Override
    public void deleteById(UUID accountId) {
        deleteById(accountId.toString());
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
    protected Map<String, @Nullable Object> values(AccountLimit entity) {
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("DEC_CREDIT_LIMIT", entity.creditLimit());
        values.put("DEC_OVERDRAFT_LIMIT", entity.overdraftLimit());
        values.put("NUM_CLOSING_DAY", entity.closingDay());
        values.put("NUM_DUE_DAY", entity.dueDay());
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected AccountLimit map(JDBCResultSet rs) {
        val accountId = UUID.fromString(rs.getString("COD_ACCOUNT").get());

        final @Nullable BigDecimal creditLimit = rs.getBigDecimal("DEC_CREDIT_LIMIT").get();
        final @Nullable BigDecimal overdraftLimit = rs.getBigDecimal("DEC_OVERDRAFT_LIMIT").get();
        final @Nullable Integer closingDay = rs.getObject("NUM_CLOSING_DAY", Integer.class).get();
        final @Nullable Integer dueDay = rs.getObject("NUM_DUE_DAY", Integer.class).get();

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        return new AccountLimit(accountId, creditLimit, overdraftLimit, closingDay, dueDay, createdAt, updatedAt);
    }
}
