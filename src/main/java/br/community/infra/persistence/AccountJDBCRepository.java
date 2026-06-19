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

import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link AccountRepository}. Mapeia o agregado {@link Account}
 * para a tabela {@code MON_ACCOUNT} (ver {@link Database}): enum {@code Type} como string,
 * {@code additionalInfo} como JSON na coluna {@code TXT_ADDITIONAL_INFO}. Usa a API
 * {@code DataSource.executeQuery/execute} (a conexão é devolvida ao pool internamente).
 */
@NullMarked
public final class AccountJDBCRepository implements AccountRepository {

    private static final String COLUMNS =
            "ID, TXT_NAME, DEC_BALANCE, TXT_TYPE, TXT_COLOR, BOL_ACTIVE, COD_LINKED_ACCOUNT, TXT_ADDITIONAL_INFO";

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
        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_ACCOUNT (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, entity.balance()),
                    new JDBCPreparedParameter(4, entity.type().name()),
                    new JDBCPreparedParameter(5, entity.color()),
                    new JDBCPreparedParameter(6, activeFlag),
                    new JDBCPreparedParameter(7, linkedStr),
                    new JDBCPreparedParameter(8, infoJson)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_ACCOUNT SET TXT_NAME = ?, DEC_BALANCE = ?, TXT_TYPE = ?, TXT_COLOR = ?, "
                            + "BOL_ACTIVE = ?, COD_LINKED_ACCOUNT = ?, TXT_ADDITIONAL_INFO = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.name()),
                    new JDBCPreparedParameter(2, entity.balance()),
                    new JDBCPreparedParameter(3, entity.type().name()),
                    new JDBCPreparedParameter(4, entity.color()),
                    new JDBCPreparedParameter(5, activeFlag),
                    new JDBCPreparedParameter(6, linkedStr),
                    new JDBCPreparedParameter(7, infoJson),
                    new JDBCPreparedParameter(8, entity.id().toString())
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
        val balance = rs.getBigDecimal("DEC_BALANCE").getOrThrow();
        val type = Account.Type.valueOf(rs.getString("TXT_TYPE").getOrThrow());
        val color = rs.getString("TXT_COLOR").getOrThrow();
        final boolean active = "Y".equals(rs.getString("BOL_ACTIVE").getOrThrow());

        @Nullable String linkedRaw = rs.getString("COD_LINKED_ACCOUNT").getOrThrow();
        @Nullable UUID linkedAccountId = (linkedRaw == null || linkedRaw.isBlank()) ? null : UUID.fromString(linkedRaw);

        @Nullable String infoJson = rs.getString("TXT_ADDITIONAL_INFO").getOrThrow();
        Map<String, Object> additionalInfo = (infoJson == null || infoJson.isBlank()) ? new HashMap<>() : readMap(infoJson);

        return new Account(id, name, balance, type, color, active, linkedAccountId, additionalInfo);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return (Map<String, Object>) mapper.readValue(json, Map.class);
    }
}
