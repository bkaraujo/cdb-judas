package br.community.infra.persistence.monetary;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link CostCenterRepository}; tabela {@code MON_COST_CENTER}. */
@NullMarked
public final class CostCenterJDBCRepository implements CostCenterRepository {

    private static final String COLUMNS = "ID, TXT_DESCRIPTION, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<CostCenter> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_COST_CENTER", this::toCostCenters);
    }

    @Override
    public Optional<CostCenter> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM MON_COST_CENTER WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toCostCenters
        ).stream().findFirst();
    }

    @Override
    public CostCenter save(CostCenter entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            // FLG_ACTIVE não está no domínio ainda — semeado como 'Y' na persistência
            dataSource.execute(
                    "INSERT INTO MON_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.description(),
                            "Y",
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE MON_COST_CENTER SET TXT_DESCRIPTION = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.description(),
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
                "DELETE FROM MON_COST_CENTER WHERE ID = ?",
                JDBCParameter.of(id.toString())
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<CostCenter> toCostCenters(JDBCResultSet rs) {
        val costCenters = new ArrayList<CostCenter>();
        while (rs.next().get()) costCenters.add(toCostCenter(rs));
        return costCenters;
    }

    private CostCenter toCostCenter(JDBCResultSet rs) {
        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new CostCenter(
                UUID.fromString(rs.getString("ID").get()),
                rs.getString("TXT_DESCRIPTION").get(),
                createdAt,
                updatedAt
        );
    }
}
