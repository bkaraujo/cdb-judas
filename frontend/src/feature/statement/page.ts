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
import type {Account} from '@/core/kernel/_0_domain/account.ts';
import * as AccountDomain from '@/core/kernel/_0_domain/account.ts';
import {fmt, sortByName, valueColor} from '@/core/kernel/_0_domain/format.ts';
import type {InvoiceTx} from '@/core/kernel/_0_domain/invoice.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as StatementItem from '@/core/kernel/_0_domain/statement-item.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import {bindRecordActions, byId, periodNavFor} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import type {Page, PageState} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {createPage} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {
    rowCountFooterHtml,
    statementColumns,
    statementRowHtml
} from '@/core/kernel/_2_infrastructure/primary/statement-row.ts';
import {rowActionsHtml} from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import {emptyState} from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import {pageHeader} from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import {selectorButtonHtml} from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import {toast} from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type {AccountStatementSummary, StatementService} from '@/feature/statement/service.ts';

/** Porta mínima do contrato público de `transactions` (fatia irmã) — só editor/exclusão de uma
 * linha, usados aqui. Cada fatia que precisa disto declara sua própria cópia local (nunca importa
 * de dentro de outra fatia — só via `<slice>/api.ts`, e `transactions` vem depois na ordem do
 * plano). Sem `list`: o índice de lançamentos vem de graça do próprio `service.load` (a mesma
 * janela alargada da conta), não precisa mais de um GET de todas as contas à parte. */
export interface StatementTransactionsPort {
  openEditor(opts: { existing: InvoiceTx; defaultDate?: string | null; onSaved?: () => void }): void;
  openDeleteFlow(tx: InvoiceTx, opts: { onDone?: () => void }): void;
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
  txIndex: InvoiceTx[];
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

  function findFullTx(id: string): InvoiceTx | null {
    return state ? byId(state.txIndex, id) : null;
  }

  function loadStatement(): Promise<void> {
    if (!state) return Promise.resolve();
    if (!state.accountId) {
      state.items = [];
      state.txIndex = [];
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
      .then((result) => {
        if (!state) return;
        state.items = result.rows;
        state.txIndex = result.raw;
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.items = [];
        state.txIndex = [];
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

  function reloadPeriod(): Promise<void> {
    return Promise.all([loadSummary(), loadStatement()]).then(() => undefined);
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');

    const $header = pageHeader({
      title: 'Extrato de Contas',
      nav: periodNavFor(state, { oneBased: true, periodService: deps.periodService, onChange: reloadPeriod }),
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
            showPurchaseDate: !!tx.totalInstallments && tx.totalInstallments > 1,
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
      $page.append(rowCountFooterHtml(txCount));
    }

    $root.empty().append($page);
  }

  function bindRoot($root: JQuery): void {
    // Trocar de conta reescreve a rota: o hash é o estado da tela (deep-link compartilhável,
    // e sobrevive ao botão Voltar do navegador — ver card-statement-page.ts).
    $root.on('click.stm', '[data-act=select-account]', function () {
      const id = $(this).attr('data-id');
      if (id && state && String(id) !== String(state.accountId)) {
        window.location.hash = '#/statement/' + id;
      }
    });

    bindRecordActions($root, '.stm', {
      find: findFullTx,
      onEdit: (tx) => deps.transactions.openEditor({ existing: tx, defaultDate: Period.bounds(currentPeriod()).from, onSaved: reloadPeriod }),
      onDelete: (tx) => deps.transactions.openDeleteFlow(tx, { onDone: reloadPeriod }),
      onMissing: () => toast('Lançamento indisponível — recarregue o período', 'error'),
    });
  }

  // Conta vem do path (#/statement/{accountId}); sem param, ou id inexistente, cai na primeira.
  function resetState(accountId?: string | null): StatementPageState {
    const p = deps.periodService.get();
    state = {
      accountId: accountId ? String(accountId) : null, month: p.month, year: p.year, items: [], summary: {}, txIndex: [], loading: false,
    };
    const accs = checkingAccounts();
    if (!accs.some((a) => String(a.id) === String(state?.accountId))) {
      state.accountId = accs.length > 0 ? String(accs[0]?.id) : null;
    }
    return state;
  }

  return createPage<StatementPageState>({
    ns: '.stm',
    state: resetState,
    render,
    bind: bindRoot,
    onMount: () => {
      reloadPeriod();
    },
  });
}
