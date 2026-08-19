/** Orchestração de busca + montagem da matriz de evolução. Sem domain própria: lê transações via
 * porta (injetada), categorias/contas do CacheStore. Agregação dele para domain. */
import { fetchWindow } from '@/core/kernel/_0_domain/invoice.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import * as Domain from './domain.ts';

export interface EvolutionTxPort {
  list(query: string): Promise<Domain.EvolutionTx[] | null>;
}

export interface CategoryEvolutionServiceDeps {
  txRepo: EvolutionTxPort;
  cache: CacheStore;
}

export interface CategoryEvolutionService {
  load(interval: Domain.Interval, start: Period.Period, size: number): Promise<Domain.EvolutionMatrix>;
}

export function createCategoryEvolutionService(deps: CategoryEvolutionServiceDeps): CategoryEvolutionService {
  return {
    load: async (interval, start, size) => {
      const accounts = deps.cache.accounts();
      const categories = deps.cache.categories();

      // Buckets
      const buckets = Domain.buildBuckets(interval, start, size);

      // Período: para anual, ajusta start para janeiro do mesmo ano
      let firstPeriod = start;
      if (interval === 'year') {
        firstPeriod = Period.create(1, start.year);
      }

      // Último período
      const lastPeriod = interval === 'month' ? Period.shift(firstPeriod, size - 1) : Period.shift(firstPeriod, (size - 1) * 12);

      // Janela de datas
      const w = {
        from: fetchWindow(accounts, firstPeriod).from,
        to: fetchWindow(accounts, lastPeriod).to,
      };

      // Busca (sem limit)
      const raw = await deps.txRepo.list('dateFrom=' + w.from + '&dateTo=' + w.to);
      const txs = Array.isArray(raw) ? raw : [];

      // Agregação
      return Domain.buildMatrix(txs, categories, accounts, buckets, interval);
    },
  };
}
