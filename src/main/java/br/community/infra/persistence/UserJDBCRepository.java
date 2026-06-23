package br.community.infra.persistence;

import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.core.web.security.Preferences;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link UserRepository}: tabela {@code SEC_USER} (identidade),
 * {@code USER_CREDENTIAL} (histórico de senhas) e {@code USER_PREFERENCES} (preferências k/v),
 * ligadas a {@code PEP_PERSON} via {@code COD_PERSON}.
 */
@NullMarked
public final class UserJDBCRepository implements UserRepository {

    private final DataSource dataSource;

    public UserJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return dataSource.executeQuery(
                "SELECT U.ID, U.TXT_USERNAME, P.TXT_NAME"
                        + " FROM SEC_USER U JOIN PEP_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.TXT_USERNAME = ?",
                List.of(new JDBCPreparedParameter(1, username)),
                this::toUsers
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return dataSource.executeQuery(
                "SELECT U.ID, U.TXT_USERNAME, P.TXT_NAME"
                        + " FROM SEC_USER U JOIN PEP_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.ID = ?",
                List.of(new JDBCPreparedParameter(1, id)),
                this::toUsers
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public User save(User user) {
        return dataSource.transaction(tx -> {
            val now = Timestamp.valueOf(LocalDateTime.now());
            @Nullable String existingPersonId = findPersonId(tx, user.id());
            @Nullable String rawName = user.name();
            String personName = rawName != null ? rawName : user.username();

            String personId;
            if (existingPersonId == null) {
                personId = UUID.randomUUID().toString();
                tx.execute(
                        "INSERT INTO PEP_PERSON (ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE, TMS_CREATE_AT, TMS_UPDATED_AT)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        new JDBCPreparedParameter(1, personId),
                        new JDBCPreparedParameter(2, personName),
                        new JDBCPreparedParameter(3, "pt-BR"),
                        new JDBCPreparedParameter(4, "pt-BR"),
                        new JDBCPreparedParameter(5, now),
                        new JDBCPreparedParameter(6, now)
                ).getOrThrow();
            } else {
                personId = existingPersonId;
                tx.execute(
                        "UPDATE PEP_PERSON SET TXT_NAME = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                        new JDBCPreparedParameter(1, personName),
                        new JDBCPreparedParameter(2, now),
                        new JDBCPreparedParameter(3, personId)
                ).getOrThrow();
            }

            tx.execute(
                    "MERGE INTO SEC_USER (ID, TXT_USERNAME, COD_PERSON) KEY(ID) VALUES (?, ?, ?)",
                    new JDBCPreparedParameter(1, user.id()),
                    new JDBCPreparedParameter(2, user.username()),
                    new JDBCPreparedParameter(3, personId)
            ).getOrThrow();

            tx.execute(
                    "INSERT INTO USER_CREDENTIAL (ID, COD_USER, TXT_PASSWORD, TMS_CREATE_AT) VALUES (?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, UUID.randomUUID().toString()),
                    new JDBCPreparedParameter(2, user.id()),
                    new JDBCPreparedParameter(3, user.password()),
                    new JDBCPreparedParameter(4, now)
            ).getOrThrow();

            val prefs = user.preferences();
            upsertPref(tx, user.id(), "theme", prefs.theme());
            upsertPref(tx, user.id(), "language", prefs.language());
            upsertPref(tx, user.id(), "locale", prefs.locale());
            upsertPref(tx, user.id(), "sidebarCollapsed", String.valueOf(prefs.sidebarCollapsed()));

            return Result.success(user);
        }).getOrThrow();
    }

    @Nullable
    private String findPersonId(DataSource.Tx tx, String userId) {
        val results = tx.executeQuery(
                "SELECT COD_PERSON FROM SEC_USER WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, userId)),
                rs -> {
                    val list = new ArrayList<String>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable String v = rs.getString("COD_PERSON").getOrThrow();
                        if (v != null) list.add(v);
                    }
                    return list;
                }
        ).getOrThrow();
        return results.isEmpty() ? null : results.get(0);
    }

    private void upsertPref(DataSource.Tx tx, String userId, String key, @Nullable String value) {
        tx.execute(
                "MERGE INTO USER_PREFERENCES (COD_USER, TXT_KEY, TXT_VALUE) KEY(COD_USER, TXT_KEY) VALUES (?, ?, ?)",
                new JDBCPreparedParameter(1, userId),
                new JDBCPreparedParameter(2, key),
                new JDBCPreparedParameter(3, value)
        ).getOrThrow();
    }

    private List<User> toUsers(JDBCResultSet rs) {
        val users = new ArrayList<User>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) users.add(toUser(rs));
        return users;
    }

    private User toUser(JDBCResultSet rs) {
        val id = rs.getString("ID").getOrThrow();
        val username = rs.getString("TXT_USERNAME").getOrThrow();
        @Nullable String name = rs.getString("TXT_NAME").getOrThrow();
        String password = findLatestPassword(id);
        Preferences preferences = loadPreferences(id);
        return new User(id, username, name, password, preferences);
    }

    private String findLatestPassword(String userId) {
        val results = dataSource.executeQuery(
                "SELECT TXT_PASSWORD FROM USER_CREDENTIAL WHERE COD_USER = ? ORDER BY TMS_CREATE_AT DESC LIMIT 1",
                List.of(new JDBCPreparedParameter(1, userId)),
                rs -> {
                    val list = new ArrayList<String>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable String pw = rs.getString("TXT_PASSWORD").getOrThrow();
                        if (pw != null) list.add(pw);
                    }
                    return list;
                }
        ).getOrThrow();
        return results.isEmpty() ? "" : results.get(0);
    }

    private Preferences loadPreferences(String userId) {
        val map = dataSource.executeQuery(
                "SELECT TXT_KEY, TXT_VALUE FROM USER_PREFERENCES WHERE COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, userId)),
                rs -> {
                    val m = new HashMap<String, String>();
                    while (Boolean.TRUE.equals(rs.next().getOrThrow())) {
                        @Nullable String k = rs.getString("TXT_KEY").getOrThrow();
                        @Nullable String v = rs.getString("TXT_VALUE").getOrThrow();
                        if (k != null && v != null) m.put(k, v);
                    }
                    return m;
                }
        ).getOrThrow();

        @Nullable String theme = map.get("theme");
        @Nullable String language = map.get("language");
        @Nullable String locale = map.get("locale");
        @Nullable String sc = map.get("sidebarCollapsed");

        return new Preferences(theme, language, locale, "true".equals(sc));
    }
}
