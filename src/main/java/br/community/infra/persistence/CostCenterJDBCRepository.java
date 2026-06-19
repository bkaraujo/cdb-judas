package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            // FLG_ACTIVE não está no domínio ainda — semeado como 'Y' na persistência
            dataSource.execute(
                    "INSERT INTO MON_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.description()),
                    new JDBCPreparedParameter(3, "Y"),
                    new JDBCPreparedParameter(4, now),
                    new JDBCPreparedParameter(5, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_COST_CENTER SET TXT_DESCRIPTION = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.description()),
                    new JDBCPreparedParameter(2, now),
                    new JDBCPreparedParameter(3, entity.id().toString())
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
        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        @Nullable Timestamp updateRaw = rs.getTimestamp("TMS_UPDATED_AT").getOrThrow();
        @Nullable LocalDateTime updatedAt = updateRaw == null ? null : updateRaw.toLocalDateTime();

        return new CostCenter(
                UUID.fromString(rs.getString("ID").getOrThrow()),
                rs.getString("TXT_DESCRIPTION").getOrThrow(),
                createdAt,
                updatedAt
        );
    }
}
