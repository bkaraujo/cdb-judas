/** Contrato público da fatia categories (equivalente ao FNNNApi do backend). Único arquivo que
 * outra fatia pode referenciar. Consumidor: dashboard (refresh do painel de despesas por
 * categoria ao mudar cadastro). */
import type { CbdChangeDetail } from '@/core/kernel/_1_application/event-bus.ts';
import type { CategoryService } from '@/feature/categories/service.ts';

export interface CategoriesApi {
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createCategoriesApi(service: CategoryService): CategoriesApi {
  return {
    onChange: (cb) => service.onChange(cb),
  };
}
