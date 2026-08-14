package br.cdb.feature.f001._2_infrastructure.persistence;

import br.cdb.feature.f001._0_domain.model.Preferences;
import br.cdb.feature.f001._0_domain.repository.PreferencesRepository;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCTransaction;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Adaptador JDBC (H2) da porta {@link PreferencesRepository}: tabela {@code F000_PREFERENCES}
 * (preferências k/v por usuário). Registros ausentes/parciais caem para
 * {@link Preferences#defaults()} na leitura.
 */
@NullMarked
public final class PreferencesJDBCRepository implements PreferencesRepository {

    private final DataSource dataSource = Context.get(DataSource.class);

    @Override
    public Preferences findByPersonId(String personId) {
        val map = dataSource.query(
                "SELECT TXT_KEY, TXT_VALUE FROM F000_PREFERENCES WHERE COD_PERSON = ?",
                JDBCParameter.of(personId),
                rs -> {
                    val m = new HashMap<String, String>();
                    while (rs.next().get()) {
                        val k = rs.getString("TXT_KEY").get();
                        val v = rs.getString("TXT_VALUE").get();
                        m.put(k, v);
                    }
                    return m;
                }
        );

        val theme = map.get("theme");
        val language = map.get("language");
        val locale = map.get("locale");
        val sc = map.get("sidebarCollapsed");

        return new Preferences(theme, language, locale, "true".equals(sc));
    }

    @Override
    public Preferences save(String personId, Preferences prefs) {
        return dataSource.transaction(tx -> {
            upsertPref(tx, personId, "theme", prefs.theme());
            upsertPref(tx, personId, "language", prefs.language());
            upsertPref(tx, personId, "locale", prefs.locale());
            upsertPref(tx, personId, "sidebarCollapsed", String.valueOf(prefs.sidebarCollapsed()));
            return Result.success(prefs);
        });
    }

    private void upsertPref(JDBCTransaction tx, String personId, String key, @Nullable String value) {
        val exists = tx.query(
                "SELECT TXT_VALUE FROM F000_PREFERENCES WHERE COD_PERSON = ? AND TXT_KEY = ?",
                JDBCParameter.of(personId, key),
                rs -> rs.next().get()
        ).get();

        val now = Timestamp.valueOf(Time.now());
        if (exists) {
            tx.execute(
                    "UPDATE F000_PREFERENCES SET TXT_VALUE = ?, TMS_UPDATED_AT = ? WHERE COD_PERSON = ? AND TXT_KEY = ?",
                    JDBCParameter.of(value, now, personId, key)
            ).get();
        } else {
            tx.execute(
                    "INSERT INTO F000_PREFERENCES (COD_PERSON, TXT_KEY, TXT_VALUE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?)",
                    JDBCParameter.of(personId, key, value, now, now)
            ).get();
        }
    }
}
