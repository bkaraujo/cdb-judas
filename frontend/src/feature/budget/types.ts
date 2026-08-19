import type { UUID } from '@/api/types.ts';

/**
 * `/budget` não existe no backend hoje (404) — tipos escritos à mão, sem OpenAPI. Ver
 * `.claude/plan.md`, Fase 1 e Fase 6. Campos canônicos `budgeted`/`spent`: os aliases de wire
 * `budget`/`target`/`actual` de `web/feature/budget.js:9` são remoção pré-aprovada, não portados.
 */
export interface BudgetResponse {
  id: UUID;
  categoryId: UUID;
  name: string;
  budgeted: number;
  spent: number;
  month: number;
  year: number;
  color: string | null;
  icon: string | null;
}

export interface BudgetCreateRequest {
  categoryId: UUID;
  month: number;
  year: number;
  budgeted: number;
  color?: string;
  icon?: string;
}

export interface BudgetUpdateRequest {
  budgeted: number;
  color?: string;
  icon?: string;
}
