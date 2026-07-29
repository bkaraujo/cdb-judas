package br.cdb.feature.f000._0_domain.repository;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CostCenterRepository extends Repository<CostCenter, UUID> {
}
