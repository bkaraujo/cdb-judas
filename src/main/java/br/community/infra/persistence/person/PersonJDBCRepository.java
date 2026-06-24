package br.community.infra.persistence.person;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.people._0_domain.model.Person;
import br.community.context.people._0_domain.repository.PersonRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link PersonRepository}; tabela {@code PEP_PERSON}. */
@NullMarked
public final class PersonJDBCRepository implements PersonRepository {

    private static final String COLUMNS = "ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<Person> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM PEP_PERSON", this::toPersons);
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM PEP_PERSON WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toPersons
        ).stream().findFirst();
    }

    @Override
    public Person save(Person entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO PEP_PERSON (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.name(),
                            entity.locale(),
                            entity.language(),
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE PEP_PERSON SET TXT_NAME = ?, TXT_LOCALE = ?, TXT_LANGUAGE = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.name(),
                            entity.locale(),
                            entity.language(),
                            now,
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute(
                "DELETE FROM PEP_PERSON WHERE ID = ?",
                JDBCParameter.of(
                        id.toString()
                )
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Person> toPersons(JDBCResultSet rs) {
        val persons = new ArrayList<Person>();
        while (rs.next().get()) persons.add(toPerson(rs));
        return persons;
    }

    private Person toPerson(JDBCResultSet rs) {
        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new Person(
                UUID.fromString(rs.getString("ID").get()),
                rs.getString("TXT_NAME").get(),
                rs.getString("TXT_LOCALE").get(),
                rs.getString("TXT_LANGUAGE").get(),
                createdAt,
                updatedAt
        );
    }
}
