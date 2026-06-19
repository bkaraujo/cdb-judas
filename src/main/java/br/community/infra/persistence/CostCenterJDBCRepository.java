package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link CostCenterRepository}; tabela {@code MON_COST_CENTER}. */
@NullMarked
public final class CostCenterJDBCRepository implements CostCenterRepository {

    private static final String COLUMNS = "ID, TXT_DESCRIPTION";

    private final DataSource dataSource;

    public CostCenterJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CostCenter> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM MON_COST_CENTER", this::toCostCenters).getOrThrow();
    }

    @Override
    public Optional<CostCenter> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_COST_CENTER WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toCostCenters
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public CostCenter save(CostCenter entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_COST_CENTER (" + COLUMNS + ") VALUES (?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.description())
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_COST_CENTER SET TXT_DESCRIPTION = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.description()),
                    new JDBCPreparedParameter(2, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_COST_CENTER WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<CostCenter> toCostCenters(JDBCResultSet rs) {
        val costCenters = new ArrayList<CostCenter>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) costCenters.add(toCostCenter(rs));
        return costCenters;
    }

    private CostCenter toCostCenter(JDBCResultSet rs) {
        return new CostCenter(
                UUID.fromString(rs.getString("ID").getOrThrow()),
                rs.getString("TXT_DESCRIPTION").getOrThrow()
        );
    }
}
