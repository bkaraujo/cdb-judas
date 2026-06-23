package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.accounts.closing.ClosingRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NullMarked
@RequiredArgsConstructor
public class ClosingJDBCRepository implements ClosingRepository {

    private static final String KEY = "closing";

    private final DataSource dataSource;

    @Override
    public Optional<YearMonth> find() {
        val userId = CurrentUser.getId();
        val results = dataSource.executeQuery(
                "SELECT TXT_VALUE FROM USER_PREFERENCES WHERE COD_USER = ? AND TXT_KEY = ?",
                List.of(
                        new JDBCPreparedParameter(1, userId),
                        new JDBCPreparedParameter(2, KEY)
                ),
                rs -> {
                    val list = new ArrayList<String>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable String v = rs.getString("TXT_VALUE").getOrThrow();
                        if (v != null) list.add(v);
                    }
                    return list;
                }
        ).getOrThrow();

        return results.isEmpty() ? Optional.empty() : Optional.of(YearMonth.parse(results.get(0)));
    }

    @Override
    public void save(YearMonth ym) {
        val userId = CurrentUser.getId();
        dataSource.execute(
                "MERGE INTO USER_PREFERENCES (COD_USER, TXT_KEY, TXT_VALUE) KEY(COD_USER, TXT_KEY) VALUES (?, ?, ?)",
                new JDBCPreparedParameter(1, userId),
                new JDBCPreparedParameter(2, KEY),
                new JDBCPreparedParameter(3, ym.toString())
        ).getOrThrow();
    }

    @Override
    public void clear() {
        val userId = CurrentUser.getId();
        dataSource.execute(
                "DELETE FROM USER_PREFERENCES WHERE COD_USER = ? AND TXT_KEY = ?",
                new JDBCPreparedParameter(1, userId),
                new JDBCPreparedParameter(2, KEY)
        ).getOrThrow();
    }
}
