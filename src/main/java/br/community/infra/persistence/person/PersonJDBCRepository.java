package br.community.infra.persistence.person;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.people._0_domain.model.Person;
import br.community.context.people._0_domain.repository.PersonRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link PersonRepository}; tabela {@code PEP_PERSON}. */
@NullMarked
public final class PersonJDBCRepository extends JDBCRepository<Person> implements PersonRepository {

    public PersonJDBCRepository() {
        super("PEP_PERSON");
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(Person entity) {
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("TXT_NAME", entity.name());
        values.put("TXT_LOCALE", entity.locale());
        values.put("TXT_LANGUAGE", entity.language());
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected Person map(JDBCResultSet rs) {
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

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
