package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.accounts.closing.ClosingRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Optional;

@NullMarked
public class ClosingJDBCRepository implements ClosingRepository {

    private static final String KEY = "closing";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public Optional<YearMonth> find() {
        val userId = CurrentUser.getId();
        val results = dataSource.query(
                "SELECT TXT_VALUE FROM USER_PREFERENCES WHERE COD_USER = ? AND TXT_KEY = ?",
                JDBCParameter.of (
                        userId,
                        KEY
                ),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) {
                        val v = rs.getString("TXT_VALUE").get();
                        list.add(v);
                    }
                    return list;
                }
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(YearMonth.parse(results.get(0)));
    }

    @Override
    public void save(YearMonth ym) {
        val userId = CurrentUser.getId();
        // Check + write na mesma transação: evita janela de corrida entre SELECT e INSERT/UPDATE
        // quando vários writers concorrem na mesma chave (COD_USER, TXT_KEY) em ambiente multi-tenant.
        dataSource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT TXT_VALUE FROM USER_PREFERENCES WHERE COD_USER = ? AND TXT_KEY = ?",
                    JDBCParameter.of(userId, KEY),
                    rs -> rs.next().get()
            ).get();

            if (exists) {
                tx.execute(
                        "UPDATE USER_PREFERENCES SET TXT_VALUE = ? WHERE COD_USER = ? AND TXT_KEY = ?",
                        JDBCParameter.of(ym.toString(), userId, KEY)
                ).get();
            } else {
                tx.execute(
                        "INSERT INTO USER_PREFERENCES (COD_USER, TXT_KEY, TXT_VALUE) VALUES (?, ?, ?)",
                        JDBCParameter.of(userId, KEY, ym.toString())
                ).get();
            }
            return Result.success(true);
        });
    }

    @Override
    public void clear() {
        val userId = CurrentUser.getId();
        dataSource.execute(
                "DELETE FROM USER_PREFERENCES WHERE COD_USER = ? AND TXT_KEY = ?",
                JDBCParameter.of (
                        userId,
                        KEY
                )
        );
    }
}
