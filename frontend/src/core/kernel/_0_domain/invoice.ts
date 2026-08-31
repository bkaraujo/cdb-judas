/**
 * Ciclo de fatura de cartão + colapso das compras numa linha só. Pure.
 *
 * Dois níveis de colapso, aplicados em sequência pelas telas de Lançamentos e Extrato de Contas:
 * `collapse` = uma linha por (cartão, vencimento); `mergeCards` = funde os cartões da mesma conta
 * numa linha 'Cartões de crédito' por (conta, vencimento).
 *
 * O ciclo é da CONTA, não do cartão (closingDay/dueDay são colunas de F002_ACCOUNT, compartilhadas
 * por todos os cartões dela). Regra — contrato em docs/backend/invoice-cycle.md, espelhada no
 * backend por f002._0_domain.model.InvoiceCycle; mudou aqui, muda lá:
 *   close(m)  = dia closingDay do mês m, clampado ao tamanho do mês
 *   fechamento: a compra d pertence ao ciclo m tal que close(m-1) < d <= close(m)
 *   vencimento: primeira data com dia dueDay ESTRITAMENTE depois de close(m) — resolve sozinho o
 *               caso dueDay <= closingDay, em que a fatura vence no mês seguinte ao fechamento
 *
 * Datas trafegam como 'yyyy-mm-dd', comparáveis com < e > (ordem lexicográfica = cronológica).
 */
import type {Account} from '@/core/kernel/_0_domain/account.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';

const DEFAULT_CLOSING_DAY = 1;
const DEFAULT_DUE_DAY = 10;
const CARDS_LABEL = 'Cartões de crédito';

export interface InvoiceTx {
  id?: string;
  date: string;
  description?: string;
  amount: number;
  cardId?: string | null;
  categoryId?: string | null;
}

export interface InvoiceRow {
  id: string;
  invoice: true;
  cards?: true;
  cardId: string | null;
  accountId: string;
  date: string;
  description: string;
  amount: number;
  type: string;
  status: string;
  categoryId: null;
}

export type CollapsedRow = InvoiceTx | InvoiceRow;

function pad2(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}
function day(dateLike: string | null | undefined): string {
  return String(dateLike || '').slice(0, 10);
}

function periodOf(dateStr: string): Period.Period {
  const d = day(dateStr);
  return Period.create(parseInt(d.slice(5, 7), 10), parseInt(d.slice(0, 4), 10));
}

function atDay(period: Period.Period, dayOfMonth: number): string {
  const last = new Date(period.year, period.month, 0).getDate();
  return period.year + '-' + pad2(period.month) + '-' + pad2(Math.min(dayOfMonth, last));
}

function configuredDay(value: number | null | undefined, fallback: number): number {
  const n = +(value ?? NaN);
  return n > 0 ? n : fallback;
}

function closingOn(account: Account | null | undefined, period: Period.Period): string {
  return atDay(period, configuredDay(account && account.closingDay, DEFAULT_CLOSING_DAY));
}

function dueOn(account: Account | null | undefined, period: Period.Period): string {
  return atDay(period, configuredDay(account && account.dueDay, DEFAULT_DUE_DAY));
}

function shift(period: Period.Period, n: number): Period.Period {
  return Period.shift(period, n);
}

/** Vencimento da fatura que fecha no mês `closingPeriod`. */
export function dueForClosing(account: Account | null | undefined, closingPeriod: Period.Period): string {
  const closing = closingOn(account, closingPeriod);
  const candidate = dueOn(account, closingPeriod);
  return candidate > closing ? candidate : dueOn(account, shift(closingPeriod, 1));
}

/** Vencimento da fatura em que a compra `dateStr` cai. */
export function dueDate(account: Account | null | undefined, dateStr: string): string {
  const d = day(dateStr);
  const period = periodOf(d);
  return dueForClosing(account, d > closingOn(account, period) ? shift(period, 1) : period);
}

/** Compras cobertas pela fatura que vence em `dueDateStr` — intervalo fechado {from, to}. */
export function cycleFor(account: Account | null | undefined, dueDateStr: string): { from: string; to: string } {
  const due = day(dueDateStr);
  const period = periodOf(due);
  const closingPeriod = closingOn(account, period) < due ? period : shift(period, -1);
  const previousClose = new Date(closingOn(account, shift(closingPeriod, -1)) + 'T00:00:00');
  previousClose.setDate(previousClose.getDate() + 1);
  return {
    from: previousClose.getFullYear() + '-' + pad2(previousClose.getMonth() + 1) + '-' + pad2(previousClose.getDate()),
    to: closingOn(account, closingPeriod),
  };
}

/** Vencimentos que caem dentro de `period` — normalmente um só; zero ou dois em contas cujo
 * closingDay/dueDay empurra a fatura para fora/duas vezes dentro do mês. */
export function dueDatesIn(account: Account | null | undefined, period: Period.Period): string[] {
  const out: string[] = [];
  ([shift(period, -1), period, shift(period, 1)] as const).forEach((closingPeriod) => {
    const due = dueForClosing(account, closingPeriod);
    const p = periodOf(due);
    if (p.year === period.year && p.month === period.month && out.indexOf(due) < 0) out.push(due);
  });
  return out.sort();
}

/** Janela de datas que precisa ser buscada para montar `period`: o próprio mês (transações sem
 * cartão) unido aos ciclos das faturas que vencem nele (compras de meses anteriores). */
export function fetchWindow(accounts: readonly Account[] | null | undefined, period: Period.Period): { from: string; to: string } {
  const b = Period.bounds(period);
  let from = b.from;
  let to = b.to;
  (accounts || []).forEach((a) => {
    if (!a.cards || a.cards.length === 0) return;
    dueDatesIn(a, period).forEach((due) => {
      const c = cycleFor(a, due);
      if (c.from < from) from = c.from;
      if (c.to > to) to = c.to;
    });
  });
  return { from, to };
}

function label(account: Account | null | undefined, card: { last4?: string | null } | null | undefined): string {
  return 'FATURA · ' + ((account && account.name) || '—') + ' ' + ((card && card.last4) || '????');
}

/** Rótulo da linha agregada de cartões. Com o nome da conta ('Cartões Nubank') nas telas que
 * misturam contas — Lançamentos; genérico onde a conta já é o recorte da tela (Extrato de
 * Contas), que chama mergeCards sem a lista de contas. */
export function cardsLabel(accountName: string | null | undefined): string {
  return accountName ? 'Cartões ' + accountName : CARDS_LABEL;
}

function today(): string {
  const d = new Date();
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
}

/** Substitui as compras de cartão por UMA linha sintética por (cartão, vencimento) e mantém as
 * demais transações do período intactas. Entrada: transações da janela alargada (ver
 * fetchWindow) + as contas do cache (que carregam `cards`). Saída: linhas do período. Compra cujo
 * vencimento cai fora de `period` é descartada — ela aparece no mês em que vence. */
export function collapse(txs: readonly InvoiceTx[] | null | undefined, accounts: readonly Account[] | null | undefined, period: Period.Period): CollapsedRow[] {
  const bounds = Period.bounds(period);
  const cardIndex: Record<string, { card: Account['cards'][number]; account: Account }> = {};
  (accounts || []).forEach((a) => {
    (a.cards || []).forEach((c) => {
      cardIndex[String(c.id)] = { card: c, account: a };
    });
  });

  const rows: CollapsedRow[] = [];
  const invoices: Record<string, InvoiceRow> = {};
  (txs || []).forEach((t) => {
    const ref = t.cardId != null ? cardIndex[String(t.cardId)] : null;
    if (!ref) {
      // Sem cartão (ou cartão desconhecido no cache): linha normal, escopada ao período.
      const d = day(t.date);
      if (d >= bounds.from && d <= bounds.to) rows.push(t);
      return;
    }
    const due = dueDate(ref.account, t.date);
    if (due < bounds.from || due > bounds.to) return;

    const key = String(t.cardId) + '|' + due;
    let inv = invoices[key];
    if (!inv) {
      inv = {
        id: 'invoice:' + t.cardId + ':' + due,
        invoice: true,
        cardId: t.cardId ?? null,
        accountId: ref.account.id,
        date: due,
        description: label(ref.account, ref.card),
        amount: 0,
        type: 'expense',
        status: due <= today() ? 'confirmed' : 'scheduled',
        categoryId: null,
      };
      invoices[key] = inv;
      rows.push(inv);
    }
    inv.amount += +t.amount || 0;
  });

  // Fatura que soma crédito líquido (estorno maior que a compra) deixa de ser despesa.
  Object.keys(invoices).forEach((k) => {
    if ((invoices[k] as InvoiceRow).amount > 0) (invoices[k] as InvoiceRow).type = 'income';
  });
  return rows;
}

/** Funde as linhas de fatura de `collapse` numa única linha por (CONTA, vencimento), sem cartão
 * próprio, que aponta pra tela #/credit-cards. `accounts` (opcional) só nomeia a linha — ver
 * cardsLabel. A tela de Lançamentos e o Extrato de Contas são visões consolidadas: nelas a fatura
 * de cada cartão é ruído (o detalhe por cartão está no Extrato do Cartão). A conta continua no
 * row — é ela que paga a fatura, e o filtro de conta dos Lançamentos casa por `accountId`. Dois
 * vencimentos distintos no mês (ou duas contas) continuam sendo duas linhas: uma linha só teria
 * de escolher uma das datas. Rows não-fatura passam intactas, na ordem de entrada. */
export function mergeCards(rows: readonly CollapsedRow[] | null | undefined, accounts?: readonly Account[] | null): CollapsedRow[] {
  const nameById: Record<string, string> = {};
  (accounts || []).forEach((a) => {
    nameById[String(a.id)] = a.name;
  });

  const out: CollapsedRow[] = [];
  const merged: Record<string, InvoiceRow> = {};
  (rows || []).forEach((r) => {
    if (!r || !('invoice' in r) || !r.invoice) {
      out.push(r);
      return;
    }
    const invRow = r as InvoiceRow;
    const due = day(invRow.date);
    const key = String(invRow.accountId) + '|' + due;
    let m = merged[key];
    if (!m) {
      m = {
        id: 'cards:' + invRow.accountId + ':' + due,
        invoice: true,
        cards: true, // agrega os cartões da conta: sem cardId próprio
        cardId: null,
        accountId: invRow.accountId,
        date: due,
        description: cardsLabel(nameById[String(invRow.accountId)]),
        amount: 0,
        type: 'expense',
        // Status vem da data (due <= hoje), igual pra toda linha de fatura do mesmo vencimento.
        status: invRow.status,
        categoryId: null,
      };
      merged[key] = m;
      out.push(m);
    }
    m.amount += +invRow.amount || 0;
  });

  Object.keys(merged).forEach((k) => {
    if (merged[k]!.amount > 0) merged[k]!.type = 'income';
  });
  return out;
}

export { DEFAULT_CLOSING_DAY, DEFAULT_DUE_DAY, CARDS_LABEL };
