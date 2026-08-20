/** pages/credit-cards — Cartões de Crédito.
 *
 * Cartões não são mais contas — cada um é `{ id, last4, accountId, active }` filho de uma conta
 * real. A página agrupa os cartões do cache por conta (uma "conta com cartões" pode ter 1+
 * cartões) e renderiza um tile por grupo:
 *   - banda visual top em gradient com a cor/nome DA CONTA
 *   - uma linha por cartão (last4 + fatura daquele cartão, casada por tx.cardId)
 *   - barra de uso COMPARTILHADA (uma por grupo): total dos cartões da conta vs
 *     account.creditLimit — o limite é da conta, não de cada cartão
 *   - rodapé Fechamento/Vencimento (também da conta)
 *
 * Período: navegação por mês, pelo VENCIMENTO da fatura — o ciclo vem de
 * Domain.CreditCard.invoicePeriod (closingDay/dueDay da conta, ver Domain.Invoice), não do mês
 * calendário. Backend GET /api/transactions ignora filtros accountId/from/to, então buscamos a
 * lista completa uma vez e particionamos por cartão + intervalo de datas no cliente.
 * "Ver fatura" leva para #/card-statement/{cardId} — o modal de fatura virou tela própria.
 *
 * "Novo Cartão" → redireciona para #/accounts (cartões são geridos no modal de edição da conta).
 */
import $ from 'jquery';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import { esc, fmt, sortByName } from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import { periodNavFor } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { cachePage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { progressBarHtml } from '@/core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import * as CreditCardDomain from '@/feature/credit-cards/domain.ts';
import type { InvoiceTx } from '@/feature/credit-cards/domain.ts';
import type { CreditCardService, CreditCardWithAccount } from '@/feature/credit-cards/service.ts';

interface CardGroup {
  account: Account;
  cards: CreditCardWithAccount[];
}

interface CreditCardsPageState extends PageState {
  groups: CardGroup[];
  month: number;
  year: number;
  allTx: InvoiceTx[];
  txLoading: boolean;
}

export interface CreditCardsPageDeps {
  service: CreditCardService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  onAccountsChange: (cb: () => void) => () => void;
  listTransactions: (params?: string) => Promise<InvoiceTx[] | null>;
}

function pctColor(pct: number): string {
  return 'var(--' + CreditCardDomain.barColorByUsage(pct) + ')';
}

export function createCreditCardsPage(deps: CreditCardsPageDeps): Page {
  let state: CreditCardsPageState | null = null;

  function currentPeriod(): Period.Period {
    return Period.create(state?.month || 1, state?.year || new Date().getFullYear());
  }

  // Cards live in CacheStore (hydrated at login, SSE-refreshed), grouped by their owning account
  // for the grid.
  function syncGroupsFromCache(s: CreditCardsPageState): void {
    const flat = deps.service.listFromCache();
    const byAccount: Record<string, CardGroup> = {};
    flat.forEach((c) => {
      const aid = String(c.accountId);
      if (!byAccount[aid]) byAccount[aid] = { account: c.account, cards: [] };
      byAccount[aid].cards.push(c);
    });
    const groups = Object.keys(byAccount).map((k) => byAccount[k] as CardGroup);
    groups.forEach((g) => g.cards.sort((a, b) => String(a.last4).localeCompare(String(b.last4))));
    groups.sort((a, b) => sortByName(a.account, b.account));
    s.groups = groups;
  }

  function loadAllTx(): Promise<void> {
    if (!state) return Promise.resolve();
    state.txLoading = true;
    return deps
      .listTransactions('limit=5000&sort=date,desc')
      .then((list) => {
        if (!state) return;
        state.allTx = Array.isArray(list) ? list : [];
        state.txLoading = false;
        render();
      })
      .catch(() => {
        if (!state) return;
        state.allTx = [];
        state.txLoading = false;
        render();
      });
  }

  // Invoice for a single card (matched by tx.cardId) — used per row.
  function computeInvoice(account: Account, cardId: string): { items: InvoiceTx[]; total: number } {
    const period = currentPeriod();
    const b = CreditCardDomain.invoicePeriod(account, period);
    const cid = String(cardId);
    const items = (state?.allTx || []).filter((t) => {
      if (String(t.cardId) !== cid) return false;
      const d = String(t.date || '').slice(0, 10);
      return d >= b.from && d <= b.to;
    });
    const total = CreditCardDomain.invoiceTotal(items, cardId, account, period);
    return { items, total };
  }

  // Combined invoice for every card on the account — feeds the shared usage bar.
  function computeAccountInvoice(account: Account): number {
    return CreditCardDomain.accountInvoiceTotal(state?.allTx || [], account, currentPeriod());
  }

  function renderGroup(group: CardGroup): JQuery {
    const account = group.account;
    const color = account.color || '#820AD1';
    const closing = CreditCardDomain.closingDay(account);
    const due = CreditCardDomain.dueDay(account);
    const limit = Number(account.creditLimit) || 0;

    const accountUsed = computeAccountInvoice(account);
    const pct = CreditCardDomain.usagePct(accountUsed, limit);
    const barColor = pctColor(pct);
    const available = CreditCardDomain.availableCredit(limit, accountUsed);

    const $card = $('<div class="card cc cc-tile" data-account-id="' + esc(account.id) + '"></div>');

    const $header = $('<div class="cc-band" style="--cc-color:' + esc(color) + ';--cc-color-soft:' + esc(color) + '99;"></div>');
    $header.append('<div class="cc-band-glow"></div>');
    $header.append('<div class="cc-band-label">' + icon('creditCard', 14) + '<span>CRÉDITO' + (account.active === false ? ' · INATIVA' : '') + '</span></div>');
    $header.append('<div class="cc-band-name">' + esc(account.name || '—') + '</div>');
    $header.append('<div class="cc-band-closing">Fecha dia ' + esc(closing) + '</div>');
    $header.append('<div class="cc-band-due">Vence dia ' + esc(due) + '</div>');
    $card.append($header);

    const $usage = $(
      '<div>' +
        '<div class="cc-usage-head"><span class="cc-usage-label">Limite da conta</span><span class="cc-usage-value">' + esc(fmt(limit)) + '</span></div>' +
        progressBarHtml(+pct.toFixed(1), barColor) +
        '<div class="cc-usage-foot"><span>' + esc(pct.toFixed(0)) + '% utilizado</span><span>' + esc(fmt(available)) + ' disponível</span></div>' +
      '</div>',
    );
    $card.append($usage);

    const $rows = $('<div style="display:flex;flex-direction:column;"></div>');
    group.cards.forEach((c) => {
      const inv = computeInvoice(account, c.id as string);
      const used = Number(inv.total) || 0;
      const valueHtml = state?.txLoading
        ? '<span style="font-size:13px;color:var(--text-muted);">…</span>'
        : '<span style="font-size:14px;font-weight:800;color:' + (used > 0 ? 'var(--expense)' : 'var(--text-primary)') + ';">' + esc(fmt(used)) + '</span>';
      $rows.append(
        '<div class="cc-row">' +
          '<span class="cc-row-last4">•••• ' + esc(c.last4) + '</span>' +
          valueHtml +
          '<a href="#/card-statement/' + esc(c.id) + '" class="cc-row-link">Ver fatura ' + icon('chevronRight', 12) + '</a>' +
        '</div>',
      );
    });
    $card.append($rows);

    $card.on('mouseenter', () => $card.css('border-color', color));
    $card.on('mouseleave', () => $card.css('border-color', ''));

    return $card;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');

    const $header = pageHeader({
      title: 'Cartões de Crédito',
      nav: periodNavFor(state, { oneBased: true, periodService: deps.periodService, onChange: render }),
    });
    $page.append($header);

    if (!state.groups.length) {
      $page.append(emptyState({ icon: 'creditCard', title: 'Nenhum cartão cadastrado', desc: 'Adicione cartões na tela de Contas Bancárias.' }));
    } else {
      const $grid = $('<div style="display:grid;gap:16px;grid-template-columns:repeat(auto-fill, minmax(320px, 1fr));"></div>');
      state.groups.forEach((g) => $grid.append(renderGroup(g)));
      $page.append($grid);
    }

    $root.empty().append($page);
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.cc', '[data-act=new]', () => {
      window.location.hash = '#/accounts';
    });
  }

  return cachePage<CreditCardsPageState>({
    ns: '.cc',
    state: () => {
      const p = deps.periodService.get();
      state = { groups: [], month: p.month, year: p.year, allTx: [], txLoading: true };
      return state;
    },
    sync: syncGroupsFromCache,
    subscribe: (cb) => deps.onAccountsChange(cb),
    render,
    bind: bindRoot,
    onMount: () => {
      loadAllTx();
    },
  });
}
