package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
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

    private final DataSource dataSource;
    private final ObjectMapper mapper;

    public AccountJDBCRepository(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    @Override
    public List<Account> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM MON_ACCOUNT", this::toAccounts).getOrThrow();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_ACCOUNT WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toAccounts
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Account save(Account entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val linked = entity.linkedAccountId();
        @Nullable String linkedStr = linked == null ? null : linked.toString();
        val activeFlag = entity.active() ? "Y" : "N";
        val infoJson = mapper.writeValueAsString(entity.additionalInfo());
        val typeId = AccountTypeMapper.toId(entity.type());
        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_ACCOUNT (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, typeId),
                    new JDBCPreparedParameter(4, activeFlag),
                    new JDBCPreparedParameter(5, linkedStr),
                    new JDBCPreparedParameter(6, infoJson),
                    new JDBCPreparedParameter(7, now),
                    new JDBCPreparedParameter(8, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_ACCOUNT SET TXT_NAME = ?, TXT_TYPE = ?, FLG_ACTIVE = ?,"
                            + " COD_LINKED_ACCOUNT = ?, TXT_ADDITIONAL_INFO = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.name()),
                    new JDBCPreparedParameter(2, typeId),
                    new JDBCPreparedParameter(3, activeFlag),
                    new JDBCPreparedParameter(4, linkedStr),
                    new JDBCPreparedParameter(5, infoJson),
                    new JDBCPreparedParameter(6, now),
                    new JDBCPreparedParameter(7, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_ACCOUNT WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Account> toAccounts(JDBCResultSet rs) {
        val accounts = new ArrayList<Account>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) accounts.add(toAccount(rs));
        return accounts;
    }

    private Account toAccount(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").getOrThrow());
        val name = rs.getString("TXT_NAME").getOrThrow();
        val type = AccountTypeMapper.fromId(rs.getString("TXT_TYPE").getOrThrow());
        final boolean active = "Y".equals(rs.getString("FLG_ACTIVE").getOrThrow());

        @Nullable String linkedRaw = rs.getString("COD_LINKED_ACCOUNT").getOrThrow();
        @Nullable UUID linkedAccountId = (linkedRaw == null || linkedRaw.isBlank()) ? null : UUID.fromString(linkedRaw);

        @Nullable String infoJson = rs.getString("TXT_ADDITIONAL_INFO").getOrThrow();
        Map<String, Object> additionalInfo = (infoJson == null || infoJson.isBlank()) ? new HashMap<>() : readMap(infoJson);

        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        @Nullable Timestamp updateRaw = rs.getTimestamp("TMS_UPDATED_AT").getOrThrow();
        @Nullable LocalDateTime updatedAt = updateRaw == null ? null : updateRaw.toLocalDateTime();

        return new Account(id, name, type, active, linkedAccountId, additionalInfo, createdAt, updatedAt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return (Map<String, Object>) mapper.readValue(json, Map.class);
    }
}
