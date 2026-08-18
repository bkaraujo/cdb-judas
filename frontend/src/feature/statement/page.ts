/** pages/statement — Extrato de Contas.
 * Layout 280px / 1fr: lista de contas (esquerda) + Card de lançamentos com saldo corrente
 * (direita). Conta selecionável; período navegável (mês/ano). StatementService monta os itens no
 * cliente (transações do período + snapshot de saldo mensal). Coluna fixa categoria/subcategoria
 * entre data e descrição. Status 'balance' => linha "Saldo anterior". Compra de cartão não
 * aparece linha a linha nem cartão a cartão: StatementService a colapsa numa linha por (conta,
 * vencimento) — 'Cartões de crédito', clicável para #/credit-cards. Linha de cartões é derivada:
 * sem ações de editar/excluir, como a linha de saldo.
 */
import $ from 'jquery';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import * as AccountDomain from '../../core/kernel/_0_domain/account.ts';
import { esc, fmt, sortByName, valueColor } from '../../core/kernel/_0_domain/format.ts';
import * as Period from '../../core/kernel/_0_domain/period.ts';
import * as StatementItem from '../../core/kernel/_0_domain/statement-item.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { byId, shiftMonth } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { createPage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { statementColumns, statementRowHtml } from '../../core/kernel/_2_infrastructure/primary/statement-row.ts';
import { rowActionsHtml } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { periodNav } from '../../core/kernel/_2_infrastructure/primary/ui/period-nav.ts';
import { selectorButtonHtml } from '../../core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import { toast } from '../../core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type { AccountStatementSummary, StatementService } from './service.ts';

/** Porta mínima do contrato público de `transactions` (fatia irmã) — só editor/exclusão de uma
 * linha + a listagem para o índice do mês, usados aqui. Cada fatia que precisa disto declara sua
 * própria cópia local (nunca importa de dentro de outra fatia — só via `<slice>/api.ts`, e
 * `transactions` vem depois na ordem do plano). */
export interface StatementTransactionsPort {
  list(query: string): Promise<unknown[] | null>;
  openEditor(opts: { existing: StatementItem.StatementSourceTx; list: StatementItem.StatementSourceTx[]; defaultDate?: string | null; onSaved?: () => void }): void;
  openDeleteFlow(tx: StatementItem.StatementSourceTx, opts: { list: StatementItem.StatementSourceTx[]; onDone?: () => void }): void;
}

export interface StatementPageDeps {
  service: StatementService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  cache: CacheStore;
  transactions: StatementTransactionsPort;
}

interface StatementPageState extends PageState {
  accountId: string | null;
  month: number;
  year: number;
  items: StatementItem.StatementRow[];
  summary: Record<string, AccountStatementSummary>;
  txIndex: StatementItem.StatementSourceTx[];
  loading: boolean;
}

export function createStatementPage(deps: StatementPageDeps): Page {
  let state: StatementPageState | null = null;

  function checkingAccounts(): Account[] {
    return deps.cache.accounts().slice().sort(sortByName);
  }

  function selectedAccount(): Account | null {
    return state ? byId(checkingAccounts(), state.accountId) : null;
  }

  function currentPeriod(): Period.Period {
    return Period.create(state?.month || 1, state?.year || new Date().getFullYear());
  }

  function findFullTx(id: string): StatementItem.StatementSourceTx | null {
    return state ? byId(state.txIndex as (StatementItem.StatementSourceTx & { id?: unknown })[], id) : null;
  }

  function loadStatement(): Promise<void> {
    if (!state) return Promise.resolve();
    if (!state.accountId) {
      state.items = [];
      state.loading = false;
      render();
      return Promise.resolve();
    }
    state.loading = true;
    render();
    const account = selectedAccount();
    const sum = state.summary[String(state.accountId)];
    const openingBalance = sum ? sum.openingBalance : account ? +account.balance || 0 : 0;
    return deps.service
      .load(state.accountId, currentPeriod(), openingBalance)
      .then((list) => {
        if (!state) return;
        state.items = Array.isArray(list) ? list : [];
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.items = [];
        state.loading = false;
        render();
        toast((err && err.message) || 'Falha ao carregar extrato', 'error');
      });
  }

  function loadSummary(): Promise<void> {
    return deps.service
      .summary(currentPeriod())
      .then((list) => {
        if (!state) return;
        const map: Record<string, AccountStatementSummary> = {};
        (Array.isArray(list) ? list : []).forEach((s) => {
          map[String(s.accountId)] = s;
        });
        state.summary = map;
        render();
      })
      .catch(() => {
        /* panorama é complementar; silencioso */
      });
  }

  // Índice do mês (todas as contas): usado para resolver o lançamento completo por trás de uma
  // linha do extrato (editar/excluir) e para detectar transferências.
  function loadMonthIndex(): Promise<void> {
    if (!state) return Promise.resolve();
    const b = Period.bounds(currentPeriod());
    return deps.transactions
      .list('dateFrom=' + b.from + '&dateTo=' + b.to)
      .then((list) => {
        if (state) state.txIndex = (Array.isArray(list) ? list : []) as StatementItem.StatementSourceTx[];
      })
      .catch(() => {
        if (state) state.txIndex = [];
      });
  }

  function reloadPeriod(): Promise<void> {
    return Promise.all([loadSummary(), loadMonthIndex()]).then(() => {
      loadStatement();
    });
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');

    const $header = pageHeader({
      title: 'Extrato de Contas',
      nav: periodNav({
        month: state.month,
        year: state.year,
        onPrev: () => {
          if (state) shiftMonth(state, -1, true, deps.periodService);
          reloadPeriod();
        },
        onNext: () => {
          if (state) shiftMonth(state, 1, true, deps.periodService);
          reloadPeriod();
        },
        onChange: (m, y) => {
          deps.periodService.set(m, y);
          if (state) {
            state.month = m;
            state.year = y;
          }
          reloadPeriod();
        },
      }),
    });

    const $sticky = $('<div class="stm-sticky-header"></div>');
    $sticky.append($header);
    $page.append($sticky);

    const $grid = $('<div class="split-view"></div>');

    const $left = $('<div class="split-left"></div>');
    const accs = checkingAccounts();
    if (accs.length === 0) {
      $left.append('<div style="font-size:13px;color:var(--text-muted);padding:14px 16px;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);">Nenhuma conta disponível.</div>');
    } else {
      accs.forEach((a) => {
        const active = String(a.id) === String(state?.accountId);
        const sum = state?.summary[String(a.id)];
        const bal = sum ? Number(sum.closingBalance) || 0 : AccountDomain.currentBalance(a);
        $left.append(selectorButtonHtml({ id: a.id, active, title: a.name, value: fmt(bal), valueColor: valueColor(bal), cls: 'stm', act: 'select-account' }));
      });
    }
    $grid.append($left);

    const $card = $('<div class="card split-right"></div>');

    if (!state.accountId) {
      $card.append(emptyState({ icon: 'bookOpen', title: 'Selecione uma conta', desc: 'Escolha uma conta à esquerda para visualizar o extrato.' }));
    } else if (state.loading) {
      $card.append(emptyState({ icon: 'bookOpen', title: 'Carregando…' }));
    } else if (state.items.length === 0) {
      $card.append(emptyState({ icon: 'bookOpen', title: 'Nenhum lançamento neste período', desc: 'Tente outro mês ou selecione outra conta.' }));
    } else {
      const items = state.items;
      const cols = statementColumns(deps.cache.categories(), deps.cache.accounts());
      items.forEach((tx, i) => {
        $card.append(
          statementRowHtml(tx, deps.cache.categories(), deps.cache.tags(), cols, {
            isLast: i === items.length - 1,
            showBalance: true,
            status: 'dot',
            invoiceLink: true,
            // Row actions (edit/delete) resolve the full transaction from the month index by id;
            // as linhas "Saldo anterior" e de fatura não têm nenhuma — a fatura é derivada,
            // edita-se no extrato do cartão.
            actions: (row) => (row.invoice ? '' : rowActionsHtml(row.id || '')),
          }),
        );
      });
    }

    $grid.append($card);
    $page.append($grid);

    if (state.accountId && !state.loading) {
      const txCount = state.items.filter((it) => !StatementItem.isBalanceHeader(it)).length;
      $page.append('<div style="text-align:right;padding:12px 4px 0;font-size:12px;color:var(--text-muted);">' + esc(txCount + (txCount === 1 ? ' transação exibida' : ' transações exibidas')) + '</div>');
    }

    $root.empty().append($page);
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.stm', '[data-act=select-account]', function () {
      const id = $(this).attr('data-id');
      if (id && state && String(id) !== String(state.accountId)) {
        state.accountId = id;
        loadStatement();
      }
    });

    $root.on('click.stm', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id') as string);
      if (!tx || !state) {
        toast('Lançamento indisponível — recarregue o período', 'error');
        return;
      }
      deps.transactions.openEditor({ existing: tx, list: state.txIndex, defaultDate: Period.bounds(currentPeriod()).from, onSaved: reloadPeriod });
    });
    $root.on('click.stm', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id') as string);
      if (!tx || !state) {
        toast('Lançamento indisponível — recarregue o período', 'error');
        return;
      }
      deps.transactions.openDeleteFlow(tx, { list: state.txIndex, onDone: reloadPeriod });
    });
  }

  // Default-select first checking account if available.
  function resetStateWithDefaultAccount(): StatementPageState {
    const p = deps.periodService.get();
    state = {
      accountId: null, month: p.month, year: p.year, items: [], summary: {}, txIndex: [], loading: false,
    };
    const accs = checkingAccounts();
    if (accs.length > 0) state.accountId = String(accs[0]?.id);
    return state;
  }

  return createPage<StatementPageState>({
    ns: '.stm',
    state: resetStateWithDefaultAccount,
    render,
    bind: bindRoot,
    onMount: () => {
      reloadPeriod();
    },
  });
}
