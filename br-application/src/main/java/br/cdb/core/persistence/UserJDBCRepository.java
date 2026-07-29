package br.cdb.core.persistence;

import br.cdb.core.security.User;
import br.commons.Registry;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCTransaction;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link UserRepository}: tabela {@code F000_USER} (identidade) e
 * {@code F000_USER_CREDENTIAL} (histórico de senhas), ligadas a {@code F000_PERSON} via
 * {@code COD_PERSON}. Preferências são uma feature à parte ({@code PreferencesJDBCRepository}).
 *
 * <p>A {@code F000_PERSON} é criada a montante pelo contexto people ({@code UserService} →
 * {@code PersonUseCase.register}); o login apenas a referencia — este adaptador nunca escreve
 * em {@code F000_PERSON}. Ao criar um login novo, {@link #save(User)} exige {@code user.personId()}
 * não-nulo; numa atualização o vínculo já persistido é reaproveitado.</p>
 */
@NullMarked
public final class UserJDBCRepository implements UserRepository {

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public Optional<User> findByUsername(String username) {
        return dataSource.query(
                "SELECT U.ID, U.TXT_USERNAME, U.FLG_ACTIVE, U.TMS_CREATE_AT, U.TMS_UPDATED_AT, U.COD_PERSON, P.TXT_NAME"
                        + " FROM F000_USER U JOIN F000_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.TXT_USERNAME = ?",
                JDBCParameter.of(username),
                this::toUsers
        ).stream().findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return dataSource.query(
                "SELECT U.ID, U.TXT_USERNAME, U.FLG_ACTIVE, U.TMS_CREATE_AT, U.TMS_UPDATED_AT, U.COD_PERSON, P.TXT_NAME"
                        + " FROM F000_USER U JOIN F000_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.ID = ?",
                JDBCParameter.of(id),
                this::toUsers
        ).stream().findFirst();
    }

    @Override
    public Optional<User> findByPersonId(String personId) {
        return dataSource.query(
                "SELECT U.ID, U.TXT_USERNAME, U.FLG_ACTIVE, U.TMS_CREATE_AT, U.TMS_UPDATED_AT, U.COD_PERSON, P.TXT_NAME"
                        + " FROM F000_USER U JOIN F000_PERSON P ON P.ID = U.COD_PERSON"
                        + " WHERE U.COD_PERSON = ?",
                JDBCParameter.of(personId),
                this::toUsers
        ).stream().findFirst();
    }

    @Override
    public User save(User user) {
        return dataSource.transaction(tx -> {
            val now = Timestamp.valueOf(Time.now());
            val existingPersonId = findPersonId(tx, user.id());

            // A Person é criada a montante (contexto people); numa atualização o vínculo já existe
            // e é reaproveitado. Só a criação de um login novo exige o personId explícito.
            val personId = user.personId() != null ? user.personId() : existingPersonId;
            if (personId == null) {
                throw new IllegalStateException(
                        "User.personId é obrigatório para criar o login (a Person deve ser criada antes)");
            }

            if (existingPersonId == null) {
                tx.execute(
                        "INSERT INTO F000_USER (ID, TXT_USERNAME, COD_PERSON, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        JDBCParameter.of(
                                user.id(),
                                user.username(),
                                personId,
                                user.active() ? "Y" : "N",
                                now,
                                now
                        )
                ).get();
            } else {
                tx.execute(
                        "UPDATE F000_USER SET TXT_USERNAME = ?, COD_PERSON = ?, FLG_ACTIVE = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                        JDBCParameter.of(
                                user.username(),
                                personId,
                                user.active() ? "Y" : "N",
                                now,
                                user.id()
                        )
                ).get();
            }

            tx.execute(
                    "INSERT INTO F000_USER_CREDENTIAL (ID, COD_USER, TXT_PASSWORD, TMS_CREATE_AT) VALUES (?, ?, ?, ?)",
                    JDBCParameter.of(
                            UUID.randomUUID().toString(),
                            user.id(),
                            user.password(),
                            now
                    )
            ).get();

            // Devolve o agregado com o personId (criado a montante pelo contexto people) — o
            // cache do decorador indexa este resultado.
            return Result.success(new User(
                    user.id(), user.username(), user.name(), user.password(),
                    user.active(), user.createdAt(), user.updatedAt(), personId));
        });
    }

    @Nullable
    private String findPersonId(JDBCTransaction tx, String userId) {
        val results = tx.query(
                "SELECT COD_PERSON FROM F000_USER WHERE ID = ?",
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

    private List<User> toUsers(JDBCResultSet rs) {
        val users = new ArrayList<User>();
        while (rs.next().get()) users.add(toUser(rs));
        return users;
    }

    private User toUser(JDBCResultSet rs) {
        val id = rs.getString("ID").get();
        val username = rs.getString("TXT_USERNAME").get();
        val name = rs.getString("TXT_NAME").get();
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();
        val personId = rs.getString("COD_PERSON").get();
        val password = findLatestPassword(id);
        return new User(id, username, name, password, active, createdAt, updatedAt, personId);
    }

    private String findLatestPassword(String userId) {
        val results = dataSource.query(
                "SELECT TXT_PASSWORD FROM F000_USER_CREDENTIAL WHERE COD_USER = ? ORDER BY TMS_CREATE_AT DESC LIMIT 1",
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
}
