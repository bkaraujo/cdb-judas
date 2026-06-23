package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._0_domain.repository.TagRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link TagRepository}; tabela {@code MON_TAG}. */
@NullMarked
public final class TagJDBCRepository implements TagRepository {

    private static final String COLUMNS = "ID, TXT_NAME, TXT_COLOR";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<Tag> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_TAG", this::toTags).getOrThrow();
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM MON_TAG WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toTags
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Tag save(Tag entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_TAG (" + COLUMNS + ") VALUES (?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, entity.color())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_TAG SET TXT_NAME = ?, TXT_COLOR = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.name()),
                    new JDBCPreparedParameter(2, entity.color()),
                    new JDBCPreparedParameter(3, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_TAG WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Tag> toTags(JDBCResultSet rs) {
        val tags = new ArrayList<Tag>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) tags.add(toTag(rs));
        return tags;
    }

    private Tag toTag(JDBCResultSet rs) {
        return new Tag(
                UUID.fromString(rs.getString("ID").getOrThrow()),
                rs.getString("TXT_NAME").getOrThrow(),
                rs.getString("TXT_COLOR").getOrThrow()
        );
    }
}
