package br.cdb.feature.f004._2_infrastructure.persistence;

import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link TagRepository}; tabela {@code F004_TAG}. */
@NullMarked
public final class TagJDBCRepository extends JDBCRepository<Tag> implements TagRepository {

    public TagJDBCRepository() {
        super("F004_TAG");
    }

    @Override
    public List<Tag> findAllByPerson(UUID personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                this::mapList
        );
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    /** {@code F004_TAG} não tem TMS_UPDATED_AT; update só altera descrição/cor. */
    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("COD_PERSON", "TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(Tag entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("TXT_DESCRIPTION", entity.name());
        values.put("TXT_COLOR", entity.color());
        values.put("TMS_CREATE_AT", Timestamp.valueOf(Time.now()));
        return values;
    }

    @Override
    protected Tag map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val personId = UUID.fromString(rs.getString("COD_PERSON").get());
        val name = rs.getString("TXT_DESCRIPTION").get();
        val color = rs.getString("TXT_COLOR").get();
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        return new Tag(id, personId, name, color, createdAt);
    }
}
