export * from '@/feature/dashboard/domain.ts';
export { createDashboardRepository } from '@/feature/dashboard/repository.ts';
export type { DashboardRepository, MonthlyResult } from '@/feature/dashboard/repository.ts';
export { createDashboardService } from '@/feature/dashboard/service.ts';
export type { DashboardService, DashboardServiceDeps } from '@/feature/dashboard/service.ts';
export { createDashboardPage } from '@/feature/dashboard/page.ts';
export type { DashboardPageDeps } from '@/feature/dashboard/page.ts';
export { PANELS } from '@/feature/dashboard/panels/index.ts';
export type { PanelCtx, PanelDef, PanelId, PanelRenderer } from '@/feature/dashboard/panels/index.ts';
