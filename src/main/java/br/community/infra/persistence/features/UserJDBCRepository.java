package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCTransaction;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.core.web.security.Preferences;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link UserRepository}: tabela {@code SEC_USER} (identidade),
 * {@code USER_CREDENTIAL} (histórico de senhas) e {@code USER_PREFERENCES} (preferências k/v),
 * ligadas a {@code PEP_PERSON} via {@code COD_PERSON}.
 */
@NullMarked
public final class UserJDBCRepository implements UserRepository {

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public Optional<User> findByUsername(String username) {
        return dataSource.query(
                "SELECT U.ID, U.TXT_USERNAME, P.TXT_NAME"
                        + " FROM SEC_USER U JOIN PEP_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.TXT_USERNAME = ?",
                JDBCParameter.of(username),
                this::toUsers
        ).stream().findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return dataSource.query(
                "SELECT U.ID, U.TXT_USERNAME, P.TXT_NAME"
                        + " FROM SEC_USER U JOIN PEP_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.ID = ?",
                JDBCParameter.of(id),
                this::toUsers
        ).stream().findFirst();
    }

    @Override
    public User save(User user) {
        return dataSource.transaction(tx -> {
            val now = Timestamp.valueOf(LocalDateTime.now());
            val existingPersonId = findPersonId(tx, user.id());
            val rawName = user.name();
            val personName = rawName != null ? rawName : user.username();

            String personId;
            if (existingPersonId == null) {
                personId = UUID.randomUUID().toString();
                tx.execute(
                        "INSERT INTO PEP_PERSON (ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE, TMS_CREATE_AT, TMS_UPDATED_AT)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        JDBCParameter.of(
                                personId,
                                personName,
                                "pt-BR",
                                "pt-BR",
                                now,
                                now
                        )
                ).get();
            } else {
                personId = existingPersonId;
                tx.execute(
                        "UPDATE PEP_PERSON SET TXT_NAME = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                        JDBCParameter.of(
                                personName,
                                now,
                                personId
                        )
                ).get();
            }

            tx.execute(
                    "MERGE INTO SEC_USER (ID, TXT_USERNAME, COD_PERSON) KEY(ID) VALUES (?, ?, ?)",
                    JDBCParameter.of(
                            user.id(),
                            user.username(),
                            personId
                    )
            ).get();

            tx.execute(
                    "INSERT INTO USER_CREDENTIAL (ID, COD_USER, TXT_PASSWORD, TMS_CREATE_AT) VALUES (?, ?, ?, ?)",
                    JDBCParameter.of(
                            UUID.randomUUID().toString(),
                            user.id(),
                            user.password(),
                            now
                    )
            ).get();

            val prefs = user.preferences();
            upsertPref(tx, user.id(), "theme", prefs.theme());
            upsertPref(tx, user.id(), "language", prefs.language());
            upsertPref(tx, user.id(), "locale", prefs.locale());
            upsertPref(tx, user.id(), "sidebarCollapsed", String.valueOf(prefs.sidebarCollapsed()));

            return Result.success(user);
        });
    }

    @Nullable
    private String findPersonId(JDBCTransaction tx, String userId) {
        val results = tx.query(
                "SELECT COD_PERSON FROM SEC_USER WHERE ID = ?",
                JDBCParameter.of(userId),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) {
                        val v = rs.getString("COD_PERSON").get();
                        list.add(v);
                    }
                    return list;
                }
        ).get();
        return results.isEmpty() ? null : results.get(0);
    }

    private void upsertPref(JDBCTransaction tx, String userId, String key, @Nullable String value) {
        tx.execute(
                "MERGE INTO USER_PREFERENCES (COD_USER, TXT_KEY, TXT_VALUE) KEY(COD_USER, TXT_KEY) VALUES (?, ?, ?)",
                JDBCParameter.of(
                        userId,
                        key,
                        value
                )
        ).get();
    }

    private List<User> toUsers(JDBCResultSet rs) {
        val users = new ArrayList<User>();
        while (rs.next().get()) users.add(toUser(rs));
        return users;
    }

    private User toUser(JDBCResultSet rs) {
        val id = rs.getString("ID").get();
        val username = rs.getString("TXT_USERNAME").get();
        val name = rs.getString("TXT_NAME").get();
        val password = findLatestPassword(id);
        val preferences = loadPreferences(id);
        return new User(id, username, name, password, preferences);
    }

    private String findLatestPassword(String userId) {
        val results = dataSource.query(
                "SELECT TXT_PASSWORD FROM USER_CREDENTIAL WHERE COD_USER = ? ORDER BY TMS_CREATE_AT DESC LIMIT 1",
                JDBCParameter.of(userId),
                rs -> {
                    val list = new ArrayList<String>();
                    while (rs.next().get()) {
                        val pw = rs.getString("TXT_PASSWORD").get();
                        list.add(pw);
                    }
                    return list;
                }
        );
        return results.isEmpty() ? "" : results.get(0);
    }

    private Preferences loadPreferences(String userId) {
        val map = dataSource.query(
                "SELECT TXT_KEY, TXT_VALUE FROM USER_PREFERENCES WHERE COD_USER = ?",
                JDBCParameter.of(userId),
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
}
