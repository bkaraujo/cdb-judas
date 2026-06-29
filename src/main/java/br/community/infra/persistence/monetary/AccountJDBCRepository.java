package br.community.infra.persistence.monetary;

import br.commons.Registry;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link AccountRepository}. Mapeia {@link Account} para
 * {@code MON_ACCOUNT} (metadados globais: nome, tipo, ativo). Saldo, cor e os dados de cartão
 * (conta vinculada, limites) são geridos pela feature em {@code USER_ACCOUNT}.
 */
@NullMarked
public final class AccountJDBCRepository implements AccountRepository {

    private static final String COLUMNS = "ID, TXT_NAME, TXT_TYPE, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<Account> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_ACCOUNT", this::toAccounts);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM MON_ACCOUNT WHERE ID = ?",
                JDBCParameter.of(
                        id.toString()
                ),
                this::toAccounts
        ).stream().findFirst();
    }

    @Override
    public Account save(Account entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val activeFlag = entity.active() ? "Y" : "N";
        val typeId = AccountTypeMapper.toId(entity.type());
        val now = Timestamp.valueOf(Time.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_ACCOUNT (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.name(),
                            typeId,
                            activeFlag,
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE MON_ACCOUNT SET TXT_NAME = ?, TXT_TYPE = ?, FLG_ACTIVE = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.name(),
                            typeId,
                            activeFlag,
                            now,
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute(
                "DELETE FROM MON_ACCOUNT WHERE ID = ?",
                JDBCParameter.of(
                        id.toString()
                )
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Account> toAccounts(JDBCResultSet rs) {
        val accounts = new ArrayList<Account>();
        while (rs.next().get()) accounts.add(toAccount(rs));
        return accounts;
    }

    private Account toAccount(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val name = rs.getString("TXT_NAME").get();
        val type = AccountTypeMapper.fromId(rs.getString("TXT_TYPE").get());
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());

        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new Account(id, name, type, active, createdAt, updatedAt);
    }
}
