import type { CbdChangeDetail } from '../../core/kernel/_1_application/event-bus.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import * as CostCenterDomain from './domain.ts';
import type { CostCenter } from './domain.ts';
import type { CostCenterRepository } from './repository.ts';

export interface CostCenterServiceDeps {
  repo: CostCenterRepository;
  cache: CacheStore;
}

export interface CostCenterService {
  // Centro de custo é fixo e somente leitura — sem create/update/remove.
  list(): Promise<CostCenter[]>;
  listCached(): CostCenter[];
  findById(id: string | null | undefined): CostCenter | null;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createCostCenterService(deps: CostCenterServiceDeps): CostCenterService {
  return {
    list: () => deps.repo.list().then((raw) => (raw || []).map(CostCenterDomain.normalize).filter((c): c is CostCenter => c != null)),
    listCached: () => deps.cache.costCenters().map(CostCenterDomain.normalize).filter((c): c is CostCenter => c != null),
    findById: (id) => CostCenterDomain.normalize(deps.cache.findById('costCenters', id)),
    onChange: (cb) => deps.cache.subscribe('COST_CENTER', cb),
  };
}
