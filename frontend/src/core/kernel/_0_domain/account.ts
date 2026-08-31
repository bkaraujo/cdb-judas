import type {AccountResponse, Card as CardWire} from '@/api/types.ts';

export const ACCOUNT_TYPES = {
  CHECKING: 'CHECKING',
  INVESTMENT: 'INVESTMENT',
} as const;

export type AccountType = (typeof ACCOUNT_TYPES)[keyof typeof ACCOUNT_TYPES];

export interface Card {
  id: string;
  last4: string;
  accountId: string;
  active: boolean;
}

export interface Account {
  id: string;
  name: string;
  type: string;
  balance: number;
  currentBalance: number;
  color: string | null;
  active: boolean;
  creditLimit: number;
  overdraftLimit: number;
  closingDay: number | null;
  dueDay: number | null;
  cards: Card[];
}

/** O gerado a partir do OpenAPI (`AccountResponse`) não declara `balance` — só `currentBalance`.
 * `balance` nunca chega assim no payload real, mas não está na lista de remoções pré-aprovadas do
 * plano (só `CategoryResponse.description`/`.parent.id`, `budget`/`target` e `StatementItem`
 * `balance`→`runningBal` estão), e o teste portado de `web/test/kernel/account.test.js` cobre o
 * fallback explicitamente — mantido como campo defensivo além do tipo gerado. */
type AccountResponseWire = AccountResponse & { balance?: number | string };

function normalizeCard(raw: CardWire): Card | null {
  if (!raw || raw.id == null) return null;
  return {
    id: raw.id,
    last4: raw.last4 || '',
    accountId: raw.accountId ?? '',
    active: raw.active !== false,
  };
}

export function normalize(raw: AccountResponseWire | null | undefined): Account | null {
  if (!raw || raw.id == null) return null;
  const type = String(raw.type || 'CHECKING').toUpperCase();
  return {
    id: raw.id,
    name: raw.name || '',
    type,
    balance: +(raw.balance ?? 0) || 0,
    currentBalance: raw.currentBalance != null ? +raw.currentBalance || 0 : +(raw.balance ?? 0) || 0,
    color: raw.color || null,
    active: raw.active !== false,
    creditLimit: raw.creditLimit != null ? +raw.creditLimit || 0 : 0,
    overdraftLimit: raw.overdraftLimit != null ? +raw.overdraftLimit || 0 : 0,
    closingDay: raw.closingDay != null ? +raw.closingDay : null,
    dueDay: raw.dueDay != null ? +raw.dueDay : null,
    cards: Array.isArray(raw.cards) ? raw.cards.map(normalizeCard).filter((c): c is Card => c != null) : [],
  };
}

/** Current balance = opening balance + every transaction (derived backend-side). Falls back to
 * the opening balance for payloads without `currentBalance`. */
export function currentBalance(a: Account | null | undefined): number {
  if (!a) return 0;
  return a.currentBalance != null ? +a.currentBalance || 0 : +a.balance || 0;
}

export function hasCards(a: Account | null | undefined): boolean {
  return !!a && Array.isArray(a.cards) && a.cards.length > 0;
}
