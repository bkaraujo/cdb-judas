package br.cdb.infra.persistence.features;

import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.cdb.context.monetary._0_domain.repository.BalanceRepository;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link BalanceRepository}: tabela {@code USER_ACCOUNT_BALANCE}
 * (períodos mensais por utilizador, período como YYYYMM inteiro).
 */
@NullMarked
public final class UserAccountBalanceJDBCRepository extends JDBCRepository<AccountBalance> implements BalanceRepository {

    public UserAccountBalanceJDBCRepository() {
        super("USER_ACCOUNT_BALANCE");
    }

    @Override
    public Optional<AccountBalance> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public List<AccountBalance> findByAccount(UUID accountId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId.toString()),
                this::mapList
        );
    }

    /**
     * Upsert por (COD_ACCOUNT, NUM_PERIOD): {@link AccountBalance} não carrega {@code id} — a PK
     * física ({@code ID}) é gerada aqui só no INSERT; a existência é checada pela chave de negócio.
     */
    @Override
    public AccountBalance save(AccountBalance entity) {
        val accountId = entity.accountId().toString();
        val numPeriod = toNumPeriod(entity.period());
        val codUser = findUserIdForAccount(entity.accountId());

        datasource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT 1 FROM " + table() + " WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                    JDBCParameter.of(accountId, numPeriod),
                    rs -> rs.next().get()
            ).get();

            if (exists) {
                tx.execute(
                        "UPDATE " + table() + " SET DEC_BALANCE = ? WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                        JDBCParameter.of(entity.balance(), accountId, numPeriod)
                ).get();
            } else {
                tx.execute(
                        "INSERT INTO " + table() + " (ID, COD_USER, COD_ACCOUNT, NUM_PERIOD, DEC_BALANCE) VALUES (?, ?, ?, ?, ?)",
                        JDBCParameter.of(UUID.randomUUID().toString(), codUser, accountId, numPeriod, entity.balance())
                ).get();
            }

            return Result.success(true);
        });

        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    /** Exclui pelo par de negócio (conta, período) — é o que {@code BalanceService.deleteById} usa hoje. */
    @Override
    public void delete(UUID uuid, YearMonth period) {
        datasource.execute(
                "DELETE FROM " + table() + " WHERE COD_ACCOUNT = ? AND NUM_PERIOD = ?",
                JDBCParameter.of(uuid.toString(), toNumPeriod(period))
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    @Override
    protected Map<String, @Nullable Object> values(AccountBalance entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("COD_USER", findUserIdForAccount(entity.accountId()));
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("NUM_PERIOD", toNumPeriod(entity.period()));
        values.put("DEC_BALANCE", entity.balance());
        return values;
    }

    @Override
    protected AccountBalance map(JDBCResultSet rs) {
        return new AccountBalance(
                UUID.fromString(rs.getString("COD_ACCOUNT").get()),
                fromNumPeriod(rs.getInt("NUM_PERIOD").get()),
                rs.getBigDecimal("DEC_BALANCE").get()
        );
    }

    private String findUserIdForAccount(UUID accountId) {
        val results = datasource.query(
                "SELECT COD_USER FROM USER_ACCOUNT WHERE COD_ACCOUNT = ? LIMIT 1",
                JDBCParameter.of(accountId.toString()),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) list.add(rs.getString("COD_USER").get());
                    return list;
                }
        );
        return results.isEmpty() ? "" : results.getFirst();
    }

    private static int toNumPeriod(YearMonth period) {
        return period.getYear() * 100 + period.getMonthValue();
    }

    private static YearMonth fromNumPeriod(int numPeriod) {
        return YearMonth.of(numPeriod / 100, numPeriod % 100);
    }
}
