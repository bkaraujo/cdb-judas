// DEPRECATED: CostCenter feature removed in favor of boolean `planned` field (f006 refactor).
// File kept to avoid deletion issues. All references have been removed.
// Safe to delete this entire directory when possible.

export interface CostCenterServiceDeps {}
export interface CostCenterService {
  list<T = any>(): Promise<T[]>;
  listCached<T = any>(): T[];
  findById<T = any>(id: string | null | undefined): T | null;
  onChange(cb: (detail: any) => void): () => void;
}
export function createCostCenterService(): CostCenterService {
  return {
    list: async () => [],
    listCached: () => [],
    findById: () => null,
    onChange: () => () => {},
  };
}
