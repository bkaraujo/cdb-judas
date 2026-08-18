export * from './domain.ts';
export { createDashboardRepository } from './repository.ts';
export type { DashboardRepository, MonthlyResult } from './repository.ts';
export { createDashboardService } from './service.ts';
export type { DashboardService, DashboardServiceDeps } from './service.ts';
export { createDashboardPage } from './page.ts';
export type { DashboardPageDeps } from './page.ts';
export { PANELS } from './panels/index.ts';
export type { PanelCtx, PanelDef, PanelId, PanelRenderer } from './panels/index.ts';
