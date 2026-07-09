package br.community.infra.persistence.features;

import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
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
public final class UserAccountBalanceJDBCRepository extends JDBCRepository<MonthlyBalance> implements BalanceRepository {

    public UserAccountBalanceJDBCRepository() {
        super("USER_ACCOUNT_BALANCE");
    }

    @Override
    public Optional<MonthlyBalance> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_ACCOUNT = ?",
                JDBCParameter.of(accountId.toString()),
                this::mapList
        );
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
    protected Map<String, @Nullable Object> values(MonthlyBalance entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_USER", findUserIdForAccount(entity.accountId()));
        values.put("COD_ACCOUNT", entity.accountId().toString());
        values.put("NUM_PERIOD", toNumPeriod(entity.period()));
        values.put("DEC_BALANCE", entity.balance());
        return values;
    }

    @Override
    protected MonthlyBalance map(JDBCResultSet rs) {
        return new MonthlyBalance(
                UUID.fromString(rs.getString("ID").get()),
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
