/** Contrato público da fatia accounts-payable (equivalente ao FNNNApi do backend). Único arquivo
 * que outra fatia pode referenciar. Consumidor: dashboard (painéis de resultado/próximos
 * vencimentos). */
import * as PayableDomain from '@/feature/accounts-payable/domain.ts';
import type { PayableListItem, PayableService } from '@/feature/accounts-payable/service.ts';

export interface AccountsPayableApi {
  listPayable(): Promise<PayableListItem[]>;
  listReceivable(): Promise<PayableListItem[]>;
  isActive(item: { status?: string } | null | undefined): boolean;
}

export function createAccountsPayableApi(service: PayableService): AccountsPayableApi {
  return {
    listPayable: () => service.listPayable(),
    listReceivable: () => service.listReceivable(),
    isActive: (item) => PayableDomain.isActive(item),
  };
}
