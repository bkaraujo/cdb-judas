package br.cdb.feature.f002._2_infrastructure.persistence;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f002._0_domain.repository.ClosingRepository;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Optional;

@NullMarked
public class ClosingJDBCRepository implements ClosingRepository {

    private static final String KEY = "closing";

    private final DataSource dataSource = Context.get(DataSource.class);

    @Override
    public Optional<YearMonth> find() {
        val personId = HTTPRequest.personId();
        val results = dataSource.query(
                "SELECT TXT_VALUE FROM F000_PREFERENCES WHERE COD_PERSON = ? AND TXT_PREFERENCE = ?",
                JDBCParameter.of (
                        personId,
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
        val personId = HTTPRequest.personId();
        // Check + write na mesma transação: evita janela de corrida entre SELECT e INSERT/UPDATE
        // quando vários writers concorrem na mesma chave (COD_PERSON, TXT_PREFERENCE) em ambiente multi-tenant.
        dataSource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT TXT_VALUE FROM F000_PREFERENCES WHERE COD_PERSON = ? AND TXT_PREFERENCE = ?",
                    JDBCParameter.of(personId, KEY),
                    rs -> rs.next().get()
            ).get();

            val now = Timestamp.valueOf(Time.now());
            if (exists) {
                tx.execute(
                        "UPDATE F000_PREFERENCES SET TXT_VALUE = ?, TMS_UPDATED_AT = ? WHERE COD_PERSON = ? AND TXT_PREFERENCE = ?",
                        JDBCParameter.of(ym.toString(), now, personId, KEY)
                ).get();
            } else {
                tx.execute(
                        "INSERT INTO F000_PREFERENCES (COD_PERSON, TXT_PREFERENCE, TXT_VALUE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?)",
                        JDBCParameter.of(personId, KEY, ym.toString(), now, now)
                ).get();
            }
            return Result.success(true);
        });
    }

    @Override
    public void clear() {
        val personId = HTTPRequest.personId();
        dataSource.execute(
                "DELETE FROM F000_PREFERENCES WHERE COD_PERSON = ? AND TXT_PREFERENCE = ?",
                JDBCParameter.of (
                        personId,
                        KEY
                )
        );
    }
}
