package br.community.infra.persistence.monetary;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link AccountRepository}. Mapeia {@link Account} para
 * {@code MON_ACCOUNT} (metadados globais). Saldo e cor são geridos por {@code USER_ACCOUNT}.
 */
@NullMarked
public final class AccountJDBCRepository implements AccountRepository {

    private static final String COLUMNS =
            "ID, TXT_NAME, TXT_TYPE, FLG_ACTIVE, COD_LINKED_ACCOUNT, TXT_ADDITIONAL_INFO, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);
    private final ObjectMapper mapper;

    public AccountJDBCRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

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

        val linked = entity.linkedAccountId();
        val linkedStr = linked == null ? null : linked.toString();
        val activeFlag = entity.active() ? "Y" : "N";
        val infoJson = mapper.writeValueAsString(entity.additionalInfo());
        val typeId = AccountTypeMapper.toId(entity.type());
        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_ACCOUNT (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.name(),
                            typeId,
                            activeFlag,
                            linkedStr,
                            infoJson,
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE MON_ACCOUNT SET TXT_NAME = ?, TXT_TYPE = ?, FLG_ACTIVE = ?, COD_LINKED_ACCOUNT = ?, TXT_ADDITIONAL_INFO = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.name(),
                            typeId,
                            activeFlag,
                            linkedStr,
                            infoJson,
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

        final @Nullable String linkedRaw = rs.getString("COD_LINKED_ACCOUNT").get();
        final @Nullable UUID linkedAccountId = (linkedRaw == null || linkedRaw.isBlank()) ? null : UUID.fromString(linkedRaw);

        final @Nullable String infoJson = rs.getString("TXT_ADDITIONAL_INFO").get();
        val additionalInfo = (infoJson == null || infoJson.isBlank()) ? new HashMap<String, Object>() : readMap(infoJson);

        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new Account(id, name, type, active, linkedAccountId, additionalInfo, createdAt, updatedAt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return (Map<String, Object>) mapper.readValue(json, Map.class);
    }
}
