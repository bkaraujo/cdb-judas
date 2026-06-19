package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.people._0_domain.model.Person;
import br.community.context.people._0_domain.repository.PersonRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link PersonRepository}; tabela {@code PEP_PERSON}. */
@NullMarked
public final class PersonJDBCRepository implements PersonRepository {

    private static final String COLUMNS = "ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE";

    private final DataSource dataSource;

    public PersonJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Person> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM PEP_PERSON", this::toPersons).getOrThrow();
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM PEP_PERSON WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toPersons
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Person save(Person entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO PEP_PERSON (" + COLUMNS + ") VALUES (?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, entity.locale()),
                    new JDBCPreparedParameter(4, entity.language())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE PEP_PERSON SET TXT_NAME = ?, TXT_LOCALE = ?, TXT_LANGUAGE = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.name()),
                    new JDBCPreparedParameter(2, entity.locale()),
                    new JDBCPreparedParameter(3, entity.language()),
                    new JDBCPreparedParameter(4, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM PEP_PERSON WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Person> toPersons(JDBCResultSet rs) {
        val persons = new ArrayList<Person>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) persons.add(toPerson(rs));
        return persons;
    }

    private Person toPerson(JDBCResultSet rs) {
        return new Person(
                UUID.fromString(rs.getString("ID").getOrThrow()),
                rs.getString("TXT_NAME").getOrThrow(),
                rs.getString("TXT_LOCALE").getOrThrow(),
                rs.getString("TXT_LANGUAGE").getOrThrow()
        );
    }
}
