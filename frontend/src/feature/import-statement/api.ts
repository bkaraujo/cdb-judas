/** Contrato público da fatia import-statement (equivalente ao FNNNApi do backend). Único arquivo
 * que outra fatia pode referenciar — hoje sem consumidores (só o composition-root abre o modal a
 * partir da sidebar/dashboard). Adapta os wire types reais de `api/overrides.ts` (ImportPreview,
 * a união discriminada por `documentType`) e as portas já publicadas por transactions/import-rules
 * para o modelo de UI mais solto que `modal.ts` trabalha (ImportDataUI/ImportRowUI). */
import type { ImportPreview } from '../../api/overrides.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import type { ImportRulesApi } from '../import-rules/api.ts';
import type { TransactionsApi } from '../transactions/api.ts';
import { createImportStatementModal } from './modal.ts';
import type { ImportDataUI, ImportStatementModalOptions } from './modal.ts';

export interface ImportStatementApi {
  open(opts?: ImportStatementModalOptions): void;
}

export function createImportStatementApi(transactions: TransactionsApi, importRules: ImportRulesApi, cache: CacheStore): ImportStatementApi {
  const openModal = createImportStatementModal({
    cache,
    importRules,
    transactions: {
      importPreview: (file, password, accountId) =>
        transactions.importPreview(file, password, accountId == null ? undefined : accountId).then((preview: ImportPreview | null) => preview as unknown as ImportDataUI | null),
      importConfirm: (payload) => transactions.importConfirm(payload) as Promise<{ created?: number; skipped?: number; reconciled?: number } | null>,
    },
  });

  return {
    open: (opts) => {
      openModal(opts || {});
    },
  };
}
