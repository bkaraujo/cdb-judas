package br.cdb.feature.f010._2_infrastructure.persistence;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link ImportRuleRepository}; tabela {@code F010_IMPORT_RULE}. */
@NullMarked
public final class ImportRuleJDBCRepository extends JDBCRepository<ImportRule> implements ImportRuleRepository {

    public ImportRuleJDBCRepository() {
        super("F010_IMPORT_RULE");
    }

    @Override
    public List<ImportRule> findAllByPerson(UUID personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                this::mapList
        );
    }

    @Override
    public Optional<ImportRule> findByPersonAndId(UUID personId, UUID id) {
        return findById(id.toString())
                .filter(rule -> rule.personId().equals(personId));
    }

    @Override
    public Optional<ImportRule> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    /** {@code F010_IMPORT_RULE} não tem TMS_UPDATED_AT; update só altera os campos editáveis. */
    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("COD_PERSON", "TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(ImportRule entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("TXT_LABEL", entity.name());
        values.put("COD_ACCOUNT", entity.accountId() != null ? entity.accountId().toString() : null);
        values.put("COD_CATEGORY", entity.categoryId() != null ? entity.categoryId().toString() : null);
        values.put("FLG_PLANNED", entity.planned() != null ? (entity.planned() ? "Y" : "N") : null);
        values.put("TMS_CREATE_AT", Timestamp.valueOf(Time.now()));
        return values;
    }

    @Override
    protected ImportRule map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val personId = UUID.fromString(rs.getString("COD_PERSON").get());
        val name = rs.getString("TXT_LABEL").get();
        final @Nullable String accountRaw = rs.getString("COD_ACCOUNT").get();
        final @Nullable UUID accountId = (accountRaw == null || accountRaw.isBlank()) ? null : UUID.fromString(accountRaw);
        final @Nullable String categoryRaw = rs.getString("COD_CATEGORY").get();
        final @Nullable UUID categoryId = (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);
        final @Nullable String plannedRaw = rs.getString("FLG_PLANNED").get();
        final @Nullable Boolean planned = plannedRaw == null ? null : "Y".equals(plannedRaw);
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        return new ImportRule(id, personId, name, List.of(), accountId, categoryId, planned, createdAt);
    }
}
