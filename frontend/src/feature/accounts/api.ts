/** Contrato público da fatia accounts (equivalente ao FNNNApi do backend). Único arquivo que
 * outra fatia pode referenciar. Consumidores: statement (painel de saldo por conta no período),
 * credit-cards (re-render ao mudar cartões de uma conta). */
import type {BalanceResponse} from '@/api/types.ts';
import type {Period} from '@/core/kernel/_0_domain/period.ts';
import type {CbdChangeDetail} from '@/core/kernel/_1_application/event-bus.ts';
import type {AccountService, BalanceService} from '@/feature/accounts/service.ts';

export interface AccountsApi {
  allAccounts(period: Period): Promise<BalanceResponse[] | null>;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createAccountsApi(accountService: AccountService, balanceService: BalanceService): AccountsApi {
  return {
    // Mesmo nome de método que BalanceService.allAccounts — statement injeta AccountsApi no
    // lugar do serviço cru, sem precisar adaptar a chamada interna.
    allAccounts: (period) => balanceService.allAccounts(period),
    onChange: (cb) => accountService.onChange(cb),
  };
}
