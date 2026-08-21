package br.cdb.feature.f010._2_infrastructure.persistence;

import br.cdb.feature.f010._0_domain.repository.ImportRuleTriggerRepository;
import br.commons.Result;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Adaptador JDBC (H2) da porta {@link ImportRuleTriggerRepository}; tabela
 * {@code F010_IMPORT_RULE_TRIGGER}. Join table pura (PK composta {@code COD_RULE, TXT_TRIGGER}),
 * sem entidade de domínio própria — por isso opera direto sobre {@link DataSource} em vez de
 * estender {@code JDBCRepository} (mesmo molde de {@code TransactionTagJDBCRepository}, f006).
 */
@NullMarked
public final class ImportRuleTriggerJDBCRepository implements ImportRuleTriggerRepository {

    private final DataSource datasource = Context.get(DataSource.class);

    @Override
    public void replaceTriggers(UUID ruleId, UUID personId, List<String> triggers) {
        datasource.transaction(tx -> {
            tx.execute(
                    "DELETE FROM F010_IMPORT_RULE_TRIGGER WHERE COD_RULE = ? AND COD_PERSON = ?",
                    JDBCParameter.of(ruleId.toString(), personId.toString())
            ).get();

            for (val trigger : new LinkedHashSet<>(triggers)) {
                tx.execute(
                        "INSERT INTO F010_IMPORT_RULE_TRIGGER (COD_RULE, COD_PERSON, TXT_TRIGGER) VALUES (?, ?, ?)",
                        JDBCParameter.of(ruleId.toString(), personId.toString(), trigger)
                ).get();
            }

            return Result.success(true);
        });
    }

    @Override
    public Map<UUID, List<String>> findTriggersByPerson(UUID personId) {
        return datasource.query(
                "SELECT COD_RULE, TXT_TRIGGER FROM F010_IMPORT_RULE_TRIGGER WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                ImportRuleTriggerJDBCRepository::readTriggersByRule
        );
    }

    @Override
    public void deleteTriggersByRule(UUID ruleId) {
        datasource.execute(
                "DELETE FROM F010_IMPORT_RULE_TRIGGER WHERE COD_RULE = ?",
                JDBCParameter.of(ruleId.toString())
        );
    }

    private static Map<UUID, List<String>> readTriggersByRule(JDBCResultSet rs) {
        val byRule = new LinkedHashMap<UUID, List<String>>();
        while (rs.next().get()) {
            val ruleId = UUID.fromString(rs.getString("COD_RULE").get());
            val trigger = rs.getString("TXT_TRIGGER").get();
            byRule.computeIfAbsent(ruleId, ignored -> new ArrayList<>()).add(trigger);
        }
        return byRule;
    }
}
