/** Lançamentos (list + filtros + CRUD modal + status). Card da lista no leiaute de linha do
 * Extrato de Contas: data | conta (coluna fixa) | categoria (coluna fixa) | descrição | badge de
 * status | valor | ações.
 *
 * Compra de cartão não aparece linha a linha nem cartão a cartão: `TransactionService
 * .listForPeriod` colapsa as compras em fatura (Domain.Invoice.collapse) e funde as faturas da
 * mesma conta numa linha só (Domain.Invoice.mergeCards) — 'Cartões <conta>', uma por (conta,
 * vencimento), lançada na data de vencimento e clicável para #/credit-cards. Consequências:
 *   - a linha de cartões é derivada: sem editar/excluir/confirmar;
 *   - os filtros de CATEGORIA e STATUS a escondem — ela agrega lançamentos de várias categorias e
 *     não tem status próprio, então nenhum valor único a representa;
 *   - conta, tipo e busca funcionam normalmente (a busca casa o rótulo);
 *   - os cards de resumo passam a somar a fatura no mês do vencimento.
 * `state.raw` guarda as transações cruas da janela buscada — é delas que os modais de
 * editar/excluir resolvem o lançamento por trás de uma linha.
 *
 * Fábrica de página: 'transactions' (Lançamentos, page.ts) e 'installments' (Parcelamentos,
 * installments-page.ts) compartilham leiaute/estado/filtros — só muda o título e o filtro extra
 * de totalInstallments > 1.
 */
import $ from 'jquery';
import {categoryById, categoryLabel, esc, flatCategories, fmt, valueColor} from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as Transaction from '@/core/kernel/_0_domain/transaction.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import {bindRecordActions, byId, periodNavFor} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import type {Page, PageState} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {createPage} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {accountOptionsHtml, categoryPickerHtml} from '@/core/kernel/_2_infrastructure/primary/pickers.ts';
import {
    rowCountFooterHtml,
    statementColumns,
    statementRowHtml
} from '@/core/kernel/_2_infrastructure/primary/statement-row.ts';
import {btn, rowActionsHtml} from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import {emptyState} from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import {pageHeader} from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import {statCardHtml} from '@/core/kernel/_2_infrastructure/primary/ui/card.ts';
import {toast} from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type {TransactionActions, TxLike} from '@/feature/transactions/actions.ts';
import type {TransactionService} from '@/feature/transactions/service.ts';

export interface TxRow extends TxLike {
  date?: string | null;
  categoryId?: string | null;
  invoice?: boolean;
  tagIds?: string[];
  amount: number;
  purchaseDate?: string | null;
}

export interface TransactionsPageDeps {
  transactionService: TransactionService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  actions: TransactionActions;
  cache: CacheStore;
  /** Porta do contrato público de `import-statement` (fatia irmã que vem depois na ordem do
   * plano) — abre o modal de importação. */
  openImport: (opts: { onImported: () => void }) => void;
}

export interface TransactionsPageConfig {
  title: string;
  installmentsOnly: boolean;
}

interface TransactionsPageState extends PageState {
  loading: boolean;
  title: string;
  installmentsOnly: boolean;
  transactions: TxRow[];
  raw: TxRow[];
  month: number; // 0-11
  year: number;
  search: string;
  filterType: 'all' | 'income' | 'expense' | 'transfer';
  filterAccount: string;
  filterCategory: string;
  filterStatus: string;
  filterPlanned: '' | 'yes' | 'no';
  showFilters: boolean;
  pendingScrollTop: number | null;
  pendingRootScrollTop: number | null;
}

export function createTransactionsListPage(deps: TransactionsPageDeps, cfg: TransactionsPageConfig): Page {
  let state: TransactionsPageState | null = null;

  function findTx(id: string): TxRow | null {
    return state ? byId(state.raw, id) : null;
  }

  // Default date for a new transaction: today when the displayed month is the current month,
  // otherwise the first day of the month being displayed.
  function defaultNewDate(): string {
    const now = new Date();
    if (!state) return now.toISOString().slice(0, 10);
    if (state.month === now.getMonth() && state.year === now.getFullYear()) {
      return now.toISOString().slice(0, 10);
    }
    return Period.bounds(Period.create(state.month + 1, state.year)).from;
  }

  function openFormModal(existing?: TxRow | null): void {
    if (!state) return;
    deps.actions.openFormModal({
      existing: existing || null,
      defaultDate: defaultNewDate(),
      onSaved: () => loadTransactions(),
    });
  }

  function loadTransactions(): Promise<void> {
    if (!state) return Promise.resolve();
    // render() rebuilds the whole page from scratch (`$root.empty()`), which momentarily leaves
    // both scroll containers empty — capture both here; render() re-applies them once the real
    // list is back.
    const $listCard = state.$root && state.$root.find('[data-region=list-card]');
    state.pendingScrollTop = $listCard && $listCard.length ? ($listCard.scrollTop() ?? null) : null;
    state.pendingRootScrollTop = state.$root ? (state.$root.scrollTop() ?? null) : null;

    state.loading = true;
    render();
    const period = Period.create(state.month + 1, state.year);
    const fetch = state.installmentsOnly ? deps.transactionService.listInstallmentsForPeriod(period) : deps.transactionService.listForPeriod(period);
    return fetch
      .then((res) => {
        if (!state) return;
        state.transactions = res.rows as TxRow[];
        state.raw = res.raw as TxRow[];
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.loading = false;
        state.transactions = [];
        state.raw = [];
        render();
        toastError((err && err.message) || 'Falha ao carregar lançamentos');
      });
  }

  function filteredTxs(): TxRow[] {
    if (!state) return [];
    const catMap = categoryById(deps.cache.categories());
    const s = (state.search || '').toLowerCase();
    return state.transactions.filter((tx) => {
      // A linha de cartões agrega várias categorias e não tem status próprio: filtrar por um
      // desses valores a exclui em vez de fingir que ela tem um.
      if (tx.invoice && (state?.filterCategory || state?.filterStatus)) return false;
      const cat = tx.categoryId != null ? catMap[tx.categoryId] : undefined;
      // As duas pernas de uma transferência reaproveitam os campos de parcela só pra se
      // referenciarem uma à outra; não são parcela de compra nenhuma. Sempre caem na categoria de
      // sistema "Transferência" (cat.isSystem).
      if (state?.installmentsOnly && (!(+(tx.totalInstallments ?? 0) > 1) || (cat && cat.isSystem))) return false;
      const catLbl = cat ? categoryLabel(deps?.cache.categories() || [], cat).toLowerCase() : '';
      const matchSearch = !s || (tx.description || '').toLowerCase().indexOf(s) >= 0 || catLbl.indexOf(s) >= 0;
      // Backend nunca manda type:"transfer" (só EXPENSE/INCOME); perna de transferência é
      // identificada pela categoria de sistema "Transferência" (cat.isSystem), não por tx.type.
      // Por isso Receitas/Despesas precisam excluir essas pernas explicitamente, senão elas
      // aparecem duplicadas nos dois filtros (cada perna é EXPENSE numa conta e INCOME na outra).
      const isTransferTx = !!(cat && cat.isSystem);
      const matchType =
        state?.filterType === 'all'
          ? true
          : state?.filterType === 'transfer'
            ? isTransferTx
            : tx.type === state?.filterType && !isTransferTx;
      const matchAcc = !state?.filterAccount || String(tx.accountId) === String(state?.filterAccount);
      const matchCat = !state?.filterCategory || String(tx.categoryId) === String(state?.filterCategory);
      const matchSt = !state?.filterStatus || (tx as { status?: string }).status === state?.filterStatus;
      const matchPlanned = !state?.filterPlanned || (state?.filterPlanned === 'yes' && (tx as { planned?: boolean }).planned) || (state?.filterPlanned === 'no' && !(tx as { planned?: boolean }).planned);
      return matchSearch && matchType && matchAcc && matchCat && matchSt && matchPlanned;
    });
  }

  function summary(list: readonly TxRow[]): { income: number; expense: number; result: number } {
    let income = 0;
    let expense = 0;
    list.forEach((t) => {
      if (t.type === 'income') income += Number((t as { amount?: number }).amount) || 0;
      else if (t.type === 'expense') expense += Math.abs(Number((t as { amount?: number }).amount) || 0);
    });
    return { income, expense, result: income - expense };
  }

  function toastError(msg: string): void {
    toast(msg, 'error');
  }

  function chipBtn(val: string, label: string): string {
    const active = state?.filterType === val;
    const style =
      'padding:6px 14px;border-radius:var(--radius-sm);font-size:13px;font-weight:500;' +
      'border:1px solid var(--border);cursor:pointer;transition:all var(--transition);' +
      (active ? 'background:var(--accent);color:#fff;' : 'background:transparent;color:var(--text-secondary);');
    return '<button type="button" data-act="set-type" data-type="' + esc(val) + '" style="' + style + '">' + esc(label) + '</button>';
  }

  function renderAdvancedFilters(): JQuery {
    if (!state) return $();
    // excludeRoots: true — raiz nunca casa contra tx.categoryId (só subcategoria é atribuível a
    // um lançamento), então oferecê-la no filtro só rende opção morta que nunca filtra nada.
    const catFieldHtml = categoryPickerHtml({
      items: flatCategories(deps.cache.categories(), undefined, true, state.filterCategory),
      selectedId: state.filterCategory,
      selectId: 'tx-filter-category',
      selectAttrs: ' data-act="filter-category"',
      placeholder: 'Todas',
      alwaysPlaceholder: true,
    });

    const accOpts = accountOptionsHtml(deps.cache.accounts(), state.filterAccount, { includeEmpty: true, emptyLabel: 'Todas', activeOnly: false });

    const stOpts = ['', 'confirmed', 'pending', 'scheduled']
      .map((st) => '<option value="' + esc(st) + '"' + (st === state?.filterStatus ? ' selected' : '') + '>' + esc(st === '' ? 'Todos' : Transaction.statusLabel(st)) + '</option>')
      .join('');

    const plannedOpts = ['', 'yes', 'no']
      .map((p) => '<option value="' + esc(p) + '"' + (p === state?.filterPlanned ? ' selected' : '') + '>' + esc(p === '' ? 'Todos' : p === 'yes' ? 'Planejada' : 'Não planejada') + '</option>')
      .join('');

    const hasAny = !!(state.filterAccount || state.filterCategory || state.filterStatus || state.filterPlanned);

    return $(
      '<div class="card" style="padding:16px;margin-bottom:14px;">' +
        '<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;">' +
          '<div class="form-group"><label class="form-label">Conta</label><select data-act="filter-account">' + accOpts + '</select></div>' +
          '<div class="form-group"><label class="form-label">Categoria</label>' + catFieldHtml + '</div>' +
          '<div class="form-group"><label class="form-label">Status</label><select data-act="filter-status">' + stOpts + '</select></div>' +
          '<div class="form-group"><label class="form-label">Planejada</label><select data-act="filter-planned">' + plannedOpts + '</select></div>' +
        '</div>' +
        (hasAny ? '<div style="display:flex;justify-content:flex-end;margin-top:10px;"><button type="button" data-act="clear-filters" class="btn btn-ghost btn-sm">' + icon('x', 13) + '<span>Limpar filtros</span></button></div>' : '') +
      '</div>',
    );
  }

  function renderList(): JQuery {
    if (!state) return $();
    const list = filteredTxs();

    const $card = $('<div class="card" data-region="list-card" style="padding:0;overflow-y:auto;max-height:calc(100vh - 300px);min-height:200px;"></div>');

    if (state.loading) {
      $card.append(emptyState({ icon: 'list', title: 'Carregando…' }));
      return $card;
    }
    if (list.length === 0) {
      $card.append(
        emptyState({
          icon: state.installmentsOnly ? 'repeat' : 'list',
          title: state.installmentsOnly ? 'Nenhuma compra parcelada encontrada' : 'Nenhum lançamento encontrado',
          desc: state.installmentsOnly ? 'Ajuste os filtros ou importe uma fatura com compras parceladas.' : 'Ajuste os filtros ou crie um novo lançamento.',
        }),
      );
      return $card;
    }

    const accs = deps.cache.accounts();
    const accMap: Record<string, (typeof accs)[number]> = {};
    accs.forEach((a) => {
      accMap[String(a.id)] = a;
    });
    const categories = deps.cache.categories();
    const tags = deps.cache.tags();
    const cols = statementColumns(categories, accs);

    const sorted = list.slice().sort((a, b) => String(b.date).localeCompare(String(a.date)));

    sorted.forEach((tx, i) => {
      const acc = accMap[String(tx.accountId)];
      const stKey = (tx as { status?: string }).status || 'confirmed';

      $card.append(
        statementRowHtml(tx, categories, tags, cols, {
          index: i + 1,
          isLast: i === sorted.length - 1,
          showAccount: true,
          accountName: acc ? acc.name : '—',
          status: 'badge',
          invoiceLink: true,
          showPurchaseDate: !!tx.totalInstallments && tx.totalInstallments > 1,
          // Row actions; mark-paid só pra não-confirmadas. Linha de cartão é derivada — edita-se
          // cada compra no extrato do cartão.
          actions: (row) => {
            if (row.invoice) return '';
            const markPaidHtml =
              stKey !== 'confirmed'
                ? '<button type="button" class="icon-btn" title="Confirmar" data-act="mark-paid" data-id="' + esc(row.id || '') + '" style="width:28px;height:28px;color:var(--income);">' + icon('check', 14) + '</button>'
                : '';
            return rowActionsHtml(row.id || '', { extra: markPaidHtml });
          },
        }),
      );
    });

    return $card;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');

    const $header = pageHeader({
      title: state.title,
      actions: [
        btn({ variant: state.showFilters ? 'primary' : 'secondary', size: 'md', icon: 'filter', label: 'Filtros', attrs: 'data-act="toggle-filters"' }),
        btn({ variant: 'secondary', size: 'md', icon: 'download', label: 'Importar', attrs: 'data-act="import"' }),
        btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Lançamento', attrs: 'data-act="new"' }),
      ],
      nav: periodNavFor(state, { oneBased: false, periodService: deps.periodService, onChange: loadTransactions }),
    });
    $page.append($header);

    const sum = summary(filteredTxs());
    const $summary = $(
      '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:18px;">' +
        statCardHtml({ icon: 'arrowUp', label: 'RECEITAS', value: fmt(sum.income), color: 'var(--income)' }) +
        statCardHtml({ icon: 'arrowDown', label: 'DESPESAS', value: fmt(sum.expense), color: 'var(--expense)' }) +
        statCardHtml({ icon: 'trendingUp', label: 'RESULTADO', value: fmt(sum.result), color: valueColor(sum.result) }) +
      '</div>',
    );
    $page.append($summary);

    const $quick = $(
      '<div style="display:flex;gap:10px;align-items:center;margin-bottom:14px;flex-wrap:wrap;">' +
        '<div style="position:relative;flex:1;max-width:320px;min-width:200px;">' +
          '<div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted);display:flex;">' + icon('search', 15) + '</div>' +
          '<input data-act="search" type="text" placeholder="Pesquisar lançamentos..." value="' + esc(state.search) + '" style="padding-left:32px;" />' +
        '</div>' +
        chipBtn('all', 'Todos') + chipBtn('income', 'Receitas') + chipBtn('expense', 'Despesas') + chipBtn('transfer', 'Transferências') +
      '</div>',
    );
    $page.append($quick);

    if (state.showFilters) $page.append(renderAdvancedFilters());

    $page.append(renderList());

    if (!state.loading) {
      const txCount = filteredTxs().length;
      $page.append(rowCountFooterHtml(txCount));
    }

    $root.empty().append($page);

    if (!state.loading) {
      if (state.pendingScrollTop != null) {
        $root.find('[data-region=list-card]').scrollTop(state.pendingScrollTop);
        state.pendingScrollTop = null;
      }
      if (state.pendingRootScrollTop != null) {
        $root.scrollTop(state.pendingRootScrollTop);
        state.pendingRootScrollTop = null;
      }
    }
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.tx', '[data-act=new]', () => openFormModal(null));
    $root.on('click.tx', '[data-act=import]', () => {
      deps.openImport({ onImported: () => loadTransactions() });
    });

    $root.on('click.tx', '[data-act=toggle-filters]', () => {
      if (state) state.showFilters = !state.showFilters;
      render();
    });

    $root.on('click.tx', '[data-act=set-type]', function () {
      const t = $(this).attr('data-type') as TransactionsPageState['filterType'];
      if (t && state && t !== state.filterType) {
        state.filterType = t;
        render();
      }
    });

    $root.on('input.tx', '[data-act=search]', function (this: HTMLInputElement) {
      if (!state) return;
      state.search = $(this).val() as string || '';
      const caretPos = this.selectionStart;
      render();
      const $input = state.$root?.find('[data-act=search]');
      $input?.trigger('focus');
      try {
        ($input?.[0] as HTMLInputElement)?.setSelectionRange(caretPos, caretPos);
      } catch {
        /* noop */
      }
    });

    $root.on('change.tx', '[data-act=filter-account]', function () {
      if (state) state.filterAccount = ($(this).val() as string) || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-category]', function () {
      if (state) state.filterCategory = ($(this).val() as string) || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-status]', function () {
      if (state) state.filterStatus = ($(this).val() as string) || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-planned]', function () {
      if (state) state.filterPlanned = ($(this).val() as TransactionsPageState['filterPlanned']) || '';
      render();
    });
    $root.on('click.tx', '[data-act=clear-filters]', () => {
      if (state) {
        state.filterAccount = '';
        state.filterCategory = '';
        state.filterStatus = '';
        state.filterPlanned = '';
      }
      render();
    });

    bindRecordActions($root, '.tx', {
      find: findTx,
      onNew: () => openFormModal(null),
      onEdit: openFormModal,
      onDelete: (tx) => deps.actions.openDeleteModal(tx, { onDone: loadTransactions }),
    });

    $root.on('click.tx', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id') as string);
      if (tx) deps.actions.markPaid(tx, { onDone: loadTransactions });
    });
  }

  return createPage<TransactionsPageState>({
    ns: '.tx',
    state: () => {
      const p = deps.periodService.get();
      state = {
        loading: true, title: cfg.title, installmentsOnly: cfg.installmentsOnly,
        transactions: [], raw: [], month: p.month - 1, year: p.year,
        search: '', filterType: 'all', filterAccount: '', filterCategory: '', filterStatus: '', filterPlanned: '',
        showFilters: false, pendingScrollTop: null, pendingRootScrollTop: null,
      };
      return state;
    },
    render,
    bind: bindRoot,
    onMount: () => {
      loadTransactions();
    },
  });
}

export function createTransactionsPage(deps: TransactionsPageDeps): Page {
  return createTransactionsListPage(deps, { title: 'Lançamentos', installmentsOnly: false });
}
