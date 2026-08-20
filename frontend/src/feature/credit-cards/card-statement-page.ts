/** pages/card-statement — Extrato do Cartão (#/card-statement/{cardId}).
 *
 * Mesmo leiaute do Extrato de Contas: .split-view (280px lista de cartões à esquerda + card de
 * lançamentos com acumulado à direita), linhas .stm-row com as mesmas colunas (data | categoria |
 * descrição | valor | acumulado | dot de status | ações) — statementRowHtml.
 *
 * Duas diferenças de conteúdo, ambas por ser fatura e não conta:
 *   - o período navegado é o do VENCIMENTO: o ciclo de compras vem de Domain.Invoice.cycleFor e
 *     começa no mês anterior (com closingDay=20/dueDay=5, a fatura que vence em 05/04 cobre
 *     21/02–20/03);
 *   - a linha-cabeçalho é "Fatura anterior" — traz o total do ciclo anterior na coluna de saldo,
 *     mas o acumulado do ciclo atual recomeça em 0, então a última linha é o total da fatura.
 *
 * Fora do sidebar: chega-se aqui pelo link da linha de fatura (Lançamentos/Extrato de Contas) ou
 * pelo "Ver fatura" da tela de Cartões.
 */
import $ from 'jquery';
import { fmtDate } from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as StatementItem from '@/core/kernel/_0_domain/statement-item.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { bindRecordActions, byId, periodNavFor } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { createPage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { rowActionsHtml } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { selectorButtonHtml } from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import { fmt } from '@/core/kernel/_0_domain/format.ts';
import { rowCountFooterHtml, statementColumns, statementRowHtml } from '@/core/kernel/_2_infrastructure/primary/statement-row.ts';
import { toast } from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type { InvoiceTx } from '@/feature/credit-cards/domain.ts';
import type { CreditCardService, CreditCardWithAccount } from '@/feature/credit-cards/service.ts';

/** Porta mínima do contrato público de `transactions` (fatia irmã que vem depois na ordem do
 * plano) — só editor/exclusão de uma linha, usados aqui. */
export interface CardStatementTxPort {
  openEditor(opts: { existing: InvoiceTx; defaultDate?: string | null; onSaved?: () => void }): void;
  openDeleteFlow(tx: InvoiceTx, opts: { onDone?: () => void }): void;
}

export interface CardStatementPageDeps {
  service: CreditCardService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  cache: CacheStore;
  transactions: CardStatementTxPort;
}

interface CardStatementPageState extends PageState {
  cardId: string | null;
  month: number;
  year: number;
  items: StatementItem.StatementRow[];
  totals: Record<string, number>;
  cycle: { from: string; to: string } | null;
  dueDate: string | null;
  txIndex: InvoiceTx[];
  loading: boolean;
}

export function createCardStatementPage(deps: CardStatementPageDeps): Page {
  let state: CardStatementPageState | null = null;

  function cards(): CreditCardWithAccount[] {
    return deps.service
      .listFromCache()
      .slice()
      .sort((a, b) => {
        const byAccount = a.account.name.toLowerCase().localeCompare(b.account.name.toLowerCase(), 'pt-BR');
        return byAccount !== 0 ? byAccount : String(a.last4).localeCompare(String(b.last4));
      });
  }

  function selectedCard(): CreditCardWithAccount | null {
    return state ? byId(cards(), state.cardId) : null;
  }

  function currentPeriod(): Period.Period {
    return Period.create(state?.month || 1, state?.year || new Date().getFullYear());
  }

  function findFullTx(id: string): InvoiceTx | null {
    return state ? byId(state.txIndex as (InvoiceTx & { id?: unknown })[], id) : null;
  }

  function reloadPeriod(): Promise<unknown> {
    return Promise.all([loadTotals(), loadInvoice()]);
  }

  /* Fatura do cartão selecionado: ciclo atual (linhas + acumulado) e o total do ciclo anterior,
   * que vira a linha-cabeçalho "Fatura anterior". */
  function loadInvoice(): Promise<void> {
    if (!state) return Promise.resolve();
    const card = selectedCard();
    if (!card) {
      state.items = [];
      state.loading = false;
      render();
      return Promise.resolve();
    }
    state.loading = true;
    render();

    const period = currentPeriod();
    const previous = Period.shift(period, -1);
    return Promise.all([deps.service.invoiceFor(card, card.account, period), deps.service.invoiceFor(card, card.account, previous)])
      .then(([invoice, previousInvoice]) => {
        if (!state) return;
        const openingLabel = invoice.cycle ? invoice.cycle.from : null;
        state.cycle = invoice.cycle;
        state.dueDate = invoice.dueDate;
        state.txIndex = invoice.transactions;
        state.items = StatementItem.buildRows(0, invoice.transactions as StatementItem.StatementSourceTx[], openingLabel, {
          headerLabel: 'Fatura anterior',
          headerBalance: -Math.abs(previousInvoice.total || 0),
          startBalance: 0,
        });
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.items = [];
        state.loading = false;
        render();
        toast((err && err.message) || 'Falha ao carregar a fatura', 'error');
      });
  }

  /* Totais de todos os cartões no período — alimenta a coluna da esquerda. Uma requisição por
   * cartão, como o extrato faz por conta. */
  function loadTotals(): Promise<void> {
    if (!state) return Promise.resolve();
    const list = cards();
    if (!list.length) return Promise.resolve();
    return Promise.all(
      list.map((c) =>
        deps.service
          .invoiceFor(c, c.account, currentPeriod())
          .then((inv) => ({ id: c.id as string, total: inv.total || 0 }))
          .catch(() => ({ id: c.id as string, total: 0 })),
      ),
    ).then((res) => {
      if (!state) return;
      const map: Record<string, number> = {};
      res.forEach((r) => {
        map[String(r.id)] = r.total;
      });
      state.totals = map;
      render();
    });
  }

  function renderRows($card: JQuery): void {
    if (!state) return;
    const items = state.items;
    const cols = statementColumns(deps.cache.categories(), deps.cache.accounts());
    items.forEach((tx, i) => {
      $card.append(
        statementRowHtml(tx, deps.cache.categories(), deps.cache.tags(), cols, {
          isLast: i === items.length - 1,
          showBalance: true,
          status: 'dot',
          actions: (row) => rowActionsHtml(row.id as string),
        }),
      );
    });
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');
    const card = selectedCard();

    const cycleLabel = state.cycle ? 'Compras de ' + fmtDate(state.cycle.from) + ' a ' + fmtDate(state.cycle.to) + (state.dueDate ? ' · vence ' + fmtDate(state.dueDate) : '') : '';
    const $header = pageHeader({
      title: 'Extrato do Cartão',
      subtitle: cycleLabel || undefined,
      nav: periodNavFor(state, { oneBased: true, periodService: deps.periodService, onChange: reloadPeriod }),
    });

    const $sticky = $('<div class="stm-sticky-header"></div>');
    $sticky.append($header);
    $page.append($sticky);

    const $grid = $('<div class="split-view"></div>');

    const $left = $('<div class="split-left"></div>');
    const list = cards();
    if (list.length === 0) {
      $left.append('<div style="font-size:13px;color:var(--text-muted);padding:14px 16px;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);">Nenhum cartão cadastrado.</div>');
    } else {
      list.forEach((c) => {
        const active = String(c.id) === String(state?.cardId);
        const total = state?.totals[String(c.id)] || 0;
        $left.append(
          selectorButtonHtml({
            id: c.id, active,
            title: ((c.account && c.account.name) || '—') + ' •••• ' + c.last4,
            value: fmt(total), valueColor: total > 0 ? 'var(--expense)' : 'var(--text-muted)',
            cls: 'cst', act: 'select-card',
          }),
        );
      });
    }
    $grid.append($left);

    const $card = $('<div class="card split-right"></div>');

    if (!card) {
      $card.append(emptyState({ icon: 'creditCard', title: 'Selecione um cartão', desc: 'Escolha um cartão à esquerda para visualizar a fatura.' }));
    } else if (state.loading) {
      $card.append(emptyState({ icon: 'creditCard', title: 'Carregando…' }));
    } else if (state.items.length <= 1) {
      $card.append(emptyState({ icon: 'creditCard', title: 'Sem lançamentos nesta fatura', desc: 'Tente outro mês ou selecione outro cartão.' }));
    } else {
      renderRows($card);
    }

    $grid.append($card);
    $page.append($grid);

    if (card && !state.loading) {
      const txCount = state.items.filter((it) => !StatementItem.isBalanceHeader(it)).length;
      $page.append(rowCountFooterHtml(txCount));
    }

    $root.empty().append($page);
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.cst', '[data-act=back]', () => {
      window.location.hash = '#/credit-cards';
    });

    // Trocar de cartão reescreve a rota: o hash é o estado da tela (deep-link compartilhável).
    $root.on('click.cst', '[data-act=select-card]', function () {
      const id = $(this).attr('data-id');
      if (id && state && String(id) !== String(state.cardId)) {
        window.location.hash = '#/card-statement/' + id;
      }
    });

    bindRecordActions($root, '.cst', {
      find: findFullTx,
      onEdit: (tx) => deps.transactions.openEditor({ existing: tx, defaultDate: state?.cycle ? state.cycle.to : null, onSaved: reloadPeriod }),
      onDelete: (tx) => deps.transactions.openDeleteFlow(tx, { onDone: reloadPeriod }),
      onMissing: () => toast('Lançamento indisponível — recarregue o período', 'error'),
    });
  }

  // Sem cartão no path (ou cartão inexistente): cai no primeiro disponível.
  function resetStateWithFallback(cardId?: string | null): CardStatementPageState {
    const p = deps.periodService.get();
    state = {
      cardId: cardId ? String(cardId) : null,
      month: p.month,
      year: p.year,
      items: [],
      totals: {},
      cycle: null,
      dueDate: null,
      txIndex: [],
      loading: false,
    };
    if (!selectedCard()) {
      const list = cards();
      state.cardId = list.length ? String(list[0]?.id) : null;
    }
    return state;
  }

  return createPage<CardStatementPageState>({
    ns: '.cst',
    state: resetStateWithFallback,
    render,
    bind: bindRoot,
    onMount: () => {
      reloadPeriod();
    },
  });
}
