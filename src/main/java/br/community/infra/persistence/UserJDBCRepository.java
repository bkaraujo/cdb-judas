package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.core.web.security.Preferences;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC (H2) da porta {@link UserRepository}; tabela {@code SEC_USER}. {@code id} é o
 * identificador de usuário (string), {@code preferences} é gravado como JSON na coluna
 * {@code TXT_PREFERENCES}.
 */
@NullMarked
public final class UserJDBCRepository implements UserRepository {

    private static final String COLUMNS = "ID, TXT_USERNAME, TXT_NAME, TXT_PASSWORD, TXT_PREFERENCES";

    private final DataSource dataSource;
    private final ObjectMapper mapper;

    public UserJDBCRepository(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM SEC_USER WHERE TXT_USERNAME = ?",
                List.of(new JDBCPreparedParameter(1, username)),
                this::toUsers
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM SEC_USER WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id)),
                this::toUsers
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public User save(User user) {
        @Nullable String name = user.name();
        dataSource.execute(
                "MERGE INTO SEC_USER (" + COLUMNS + ") KEY(ID) VALUES (?, ?, ?, ?, ?)",
                new JDBCPreparedParameter(1, user.id()),
                new JDBCPreparedParameter(2, user.username()),
                new JDBCPreparedParameter(3, name),
                new JDBCPreparedParameter(4, user.password()),
                new JDBCPreparedParameter(5, mapper.writeValueAsString(user.preferences()))
        ).getOrThrow();
        return user;
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
        val password = rs.getString("TXT_PASSWORD").getOrThrow();

        @Nullable String prefsJson = rs.getString("TXT_PREFERENCES").getOrThrow();
        Preferences preferences = (prefsJson == null || prefsJson.isBlank())
                ? Preferences.defaults()
                : mapper.readValue(prefsJson, Preferences.class);

        return new User(id, username, name, password, preferences);
    }
}
