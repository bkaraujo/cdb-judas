/** pages/accounts-payable — A Pagar e Receber.
 * Lists from PayableService (listPayable + listReceivable), filters by period (month/year on
 * `due`). Summary card (3-col), tabs (pagar/receber), card-list rows with status dot + badge +
 * amount + actions. CRUD via TransactionsApi (create/update/remove). Mark-as-paid via
 * TransactionsApi.patchStatus.
 */
import $ from 'jquery';
import { categoryById, categoryLabel, esc, fmt, fmtDate, maskCurrency, parseCurrency, valueColor } from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as Transaction from '@/core/kernel/_0_domain/transaction.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { bindRecordActions, byId, formModal, periodNavFor } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { bindCurrencyMask } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { createPage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { accountOptionsHtml, categoryItemsFor, categoryPickerHtml } from '@/core/kernel/_2_infrastructure/primary/pickers.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { statCardHtml } from '@/core/kernel/_2_infrastructure/primary/ui/card.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { tabs } from '@/core/kernel/_2_infrastructure/primary/ui/tabs.ts';
import { toast } from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import { typeToggleHtml } from '@/core/kernel/_2_infrastructure/primary/ui/type-toggle.ts';
import * as PayableDomain from '@/feature/accounts-payable/domain.ts';
import type { PayableListItem, PayableService } from '@/feature/accounts-payable/service.ts';

/** Porta mínima do contrato público de `transactions` (fatia irmã) — CRUD + delete/mark-paid
 * (ganha de brinde o escopo de parcelas SINGLE/FUTURE/ALL no delete). */
export interface AccountsPayableTxPort {
  create(data: Record<string, unknown>): Promise<unknown>;
  update(id: string, data: Record<string, unknown>): Promise<unknown>;
  openDeleteFlow(item: PayableListItem, opts: { onDone?: () => void }): void;
  markPaid(item: PayableListItem, opts: { onDone?: () => void }): void;
}

export interface AccountsPayablePageDeps {
  service: PayableService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  cache: CacheStore;
  transactions: AccountsPayableTxPort;
}

interface AccountsPayablePageState extends PageState {
  loading: boolean;
  payable: PayableListItem[];
  receivable: PayableListItem[];
  tab: 'payable' | 'receivable';
  month: number;
  year: number;
}

export function createAccountsPayablePage(deps: AccountsPayablePageDeps): Page {
  let state: AccountsPayablePageState | null = null;

  function currentPeriod(): Period.Period {
    return Period.create((state?.month ?? 0) + 1, state?.year || new Date().getFullYear());
  }

  function findItem(id: string): PayableListItem | null {
    return state ? byId(state.payable.concat(state.receivable), id) : null;
  }

  function inPeriod(arr: PayableListItem[]): PayableListItem[] {
    return deps.service.inPeriod(arr, currentPeriod());
  }

  function loadData(): Promise<void> {
    if (!state) return Promise.resolve();
    state.loading = true;
    render();
    return Promise.all([deps.service.listPayable(), deps.service.listReceivable()])
      .then(([payable, receivable]) => {
        if (!state) return;
        state.payable = Array.isArray(payable) ? payable : [];
        state.receivable = Array.isArray(receivable) ? receivable : [];
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.loading = false;
        state.payable = [];
        state.receivable = [];
        render();
        toast((err && err.message) || 'Falha ao carregar contas', 'error');
      });
  }

  function renderList(items: PayableListItem[]): JQuery {
    const $card = $('<div class="card card-list" style="overflow-y:auto;max-height:calc(100vh - 320px);min-height:200px;"></div>');

    if (state?.loading) {
      $card.append(emptyState({ icon: 'calendar', title: 'Carregando…' }));
      return $card;
    }
    if (items.length === 0) {
      $card.append(
        emptyState({
          icon: 'calendar',
          title: 'Nenhuma conta neste período',
          desc: state?.tab === 'payable' ? 'Nenhuma conta a pagar para o período selecionado.' : 'Nenhuma conta a receber para o período selecionado.',
        }),
      );
      return $card;
    }

    const catMap = categoryById(deps.cache.categories());
    const sorted = items.slice().sort((a, b) => String(a.due).localeCompare(String(b.due)));

    sorted.forEach((item) => {
      const st = String(item.status || 'pending').toLowerCase();
      const badgeColor = PayableDomain.statusBadgeVariant(st);
      const dotColor = 'var(--' + badgeColor + ')';
      const badgeLabel = st === 'scheduled' ? 'Agendado' : st === 'confirmed' ? 'Confirmado' : 'Pendente';

      const amtColor = state?.tab === 'payable' ? 'var(--expense)' : 'var(--income)';
      const amtAbs = Math.abs(Number(item.amount) || 0);

      const cat = item.categoryId != null ? catMap[item.categoryId] : undefined;
      const catLbl = cat ? categoryLabel(deps.cache.categories(), cat) : item.categoryId ? String(item.categoryId) : '—';

      // `item.name` nunca existe (adapt() produz `description`) — preserva o bug original em que
      // toda linha mostra "—" como título (ver nota em service.ts).
      const title = (item as unknown as { name?: string }).name;

      const $row = $(
        '<div class="card-row" data-row="ap" data-id="' + esc(item.id) + '">' +
          '<div class="card-row-main">' +
            '<div style="width:10px;height:10px;border-radius:50%;background:' + dotColor + ';flex-shrink:0;"></div>' +
            '<div style="min-width:0;flex:1;display:flex;flex-direction:column;gap:2px;">' +
              '<span class="row-title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;">' + esc(title || '—') + '</span>' +
              '<span class="row-sub">' + esc(catLbl) + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="card-row-actions" style="gap:16px;">' +
            '<span style="font-size:12px;color:var(--text-muted);">' + esc(fmtDate(item.due)) + '</span>' +
            '<span class="badge badge-' + esc(badgeColor) + '">' + esc(badgeLabel) + '</span>' +
            '<span style="font-size:14px;font-weight:700;color:' + amtColor + ';min-width:100px;text-align:right;">' + esc(fmt(amtAbs)) + '</span>' +
            '<div style="position:relative;">' +
              '<button type="button" class="icon-btn icon-btn-sm" data-act="menu" data-id="' + esc(item.id) + '">' + icon('moreVertical', 14) + '</button>' +
              '<div class="menu" data-menu-for="' + esc(item.id) + '">' +
                (st !== 'confirmed' ? '<button type="button" class="menu-item" data-act="mark-paid" data-id="' + esc(item.id) + '">' + icon('check', 14) + '<span>Marcar como pago</span></button>' : '') +
                '<button type="button" class="menu-item" data-act="edit" data-id="' + esc(item.id) + '">' + icon('edit', 14) + '<span>Editar</span></button>' +
                '<button type="button" class="menu-item is-danger" data-act="trash" data-id="' + esc(item.id) + '">' + icon('trash', 14) + '<span>Excluir</span></button>' +
              '</div>' +
            '</div>' +
          '</div>' +
        '</div>',
      );

      $card.append($row);
    });

    return $card;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $page = $('<div class="fade-in"></div>');

    const $header = pageHeader({
      title: 'A Pagar e Receber',
      nav: periodNavFor(state, { oneBased: false, periodService: deps.periodService, onChange: render }),
    });
    $page.append($header);

    const periodPayable = inPeriod(state.payable);
    const periodReceivable = inPeriod(state.receivable);
    const totalPayable = periodPayable.reduce((s, i) => s + (Number(i.amount) || 0), 0);
    const totalReceivable = periodReceivable.reduce((s, i) => s + (Number(i.amount) || 0), 0);
    const result = totalReceivable - totalPayable;

    function col(label: string, value: number, color: string): string {
      return statCardHtml({ label, value: fmt(value), color, bare: true });
    }
    const $summary = $(
      '<div class="card" style="padding:16px 20px;margin-bottom:16px;display:flex;gap:32px;align-items:center;">' +
        col('A Pagar', totalPayable, 'var(--expense)') +
        '<div style="width:1px;align-self:stretch;background:var(--border);"></div>' +
        col('A Receber', totalReceivable, 'var(--income)') +
        '<div style="width:1px;align-self:stretch;background:var(--border);"></div>' +
        col('Resultado', result, valueColor(result)) +
      '</div>',
    );
    $page.append($summary);

    const $tabs = tabs(
      [
        { id: 'payable', label: 'A pagar (' + periodPayable.length + ')' },
        { id: 'receivable', label: 'A receber (' + periodReceivable.length + ')' },
      ],
      state.tab,
      (id) => {
        if (state) state.tab = id as AccountsPayablePageState['tab'];
        render();
      },
    );
    $page.append($tabs);

    const items = state.tab === 'payable' ? periodPayable : periodReceivable;
    $page.append(renderList(items));

    $root.empty().append($page);
  }

  function openFormModal(existing: PayableListItem | null): void {
    if (!state) return;
    const isEdit = !!existing;
    const defaultType = state.tab === 'receivable' ? 'RECEIVABLE' : 'PAYABLE';
    const uniq = Date.now();
    const ids = { name: 'ap-name-' + uniq, amount: 'ap-amount-' + uniq, due: 'ap-due-' + uniq, account: 'ap-acc-' + uniq, category: 'ap-cat-' + uniq, status: 'ap-st-' + uniq };

    const initial = {
      type: isEdit ? existing?.type || defaultType : defaultType,
      name: isEdit ? (existing as unknown as { name?: string })?.name || '' : '',
      amount: isEdit ? maskCurrency(String(Math.abs(Number(existing?.amount) || 0))) : '',
      due: isEdit ? String(existing?.due || '').slice(0, 10) : new Date().toISOString().slice(0, 10),
      accountId: isEdit ? String(existing?.accountId || '') : '',
      categoryId: isEdit ? String(existing?.categoryId || '') : '',
      status: isEdit ? existing?.status || 'pending' : 'pending',
    };

    function natureForType(t: string): string {
      return t === 'RECEIVABLE' ? 'INCOME' : 'EXPENSE';
    }

    // keepId: mantém a categoria já gravada na lista mesmo se foi inativada depois — sem isso,
    // editar um item com categoria inativa perdia a categoria em silêncio.
    function categoryFieldHtml(t: string, selectedId: string, keepId: string | null): string {
      return categoryPickerHtml({
        items: categoryItemsFor(deps.cache.categories(), natureForType(t), keepId),
        selectedId,
        selectId: ids.category,
        selectAttrs: ' name="categoryId" data-region="category-select"',
        placeholder: 'Selecione',
      });
    }

    const accOpts = accountOptionsHtml(deps.cache.accounts(), initial.accountId, { keepId: isEdit ? initial.accountId : null });

    const statusOpts = ([['pending', 'Pendente'], ['scheduled', 'Agendado'], ['confirmed', 'Confirmado']] as const)
      .map((p) => '<option value="' + esc(p[0]) + '"' + (p[0] === initial.status ? ' selected' : '') + '>' + esc(p[1]) + '</option>')
      .join('');

    const TYPE_OPTIONS = [
      { value: 'PAYABLE', label: '↓ A Pagar', color: 'expense' },
      { value: 'RECEIVABLE', label: '↑ A Receber', color: 'income' },
    ];

    const bodyHtml =
      '<form data-form="ap" autocomplete="off">' +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' + typeToggleHtml(TYPE_OPTIONS, initial.type) + '</div>' +
        '<input type="hidden" name="type" value="' + esc(initial.type) + '" />' +
        '<div class="form-grid">' +
          '<div class="form-group full"><label class="form-label" for="' + ids.name + '">Descrição</label><input id="' + ids.name + '" name="name" type="text" required style="text-transform:uppercase;" placeholder="Ex: Aluguel, Salário..." value="' + esc(initial.name) + '" /></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.amount + '">Valor (R$)</label><input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" placeholder="0,00" value="' + esc(initial.amount) + '" /></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.due + '">Vencimento</label><input id="' + ids.due + '" name="due" type="date" required value="' + esc(initial.due) + '" /></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.category + '">Categoria</label><div data-region="category-select-wrap">' + categoryFieldHtml(initial.type, initial.categoryId, initial.categoryId) + '</div></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.account + '">Conta</label><select id="' + ids.account + '" name="accountId">' + accOpts + '</select></div>' +
          '<div class="form-group full"><label class="form-label" for="' + ids.status + '">Status</label><select id="' + ids.status + '" name="status">' + statusOpts + '</select></div>' +
        '</div>' +
      '</form>';

    const m = formModal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      formName: 'ap',
      body: bodyHtml,
      onSubmit: ($form) => {
        const $amount = $form.find('input[name=amount]');
        const name = (($form.find('input[name=name]').val() as string) || '').trim();
        if (!name) {
          $form.find('input[name=name]').trigger('focus');
          return null;
        }
        const amt = parseCurrency($amount.val() as string);
        if (!isFinite(amt) || amt <= 0) {
          $amount.trigger('focus');
          return null;
        }
        const type = $form.find('input[name=type]').val() as string;
        const due = $form.find('input[name=due]').val() as string;
        const categoryId = $form.find('select[name=categoryId]').val() as string;
        const accountId = $form.find('select[name=accountId]').val() as string;
        const status = $form.find('select[name=status]').val() as string;

        if (!accountId) {
          toast('Selecione uma conta', 'error');
          return null;
        }
        if (!categoryId) {
          toast('Selecione uma categoria', 'error');
          return null;
        }

        // Persist as a transaction (payables are transactions with status pending/scheduled/confirmed).
        const txType = PayableDomain.natureOf(type);
        const signed = Transaction.signedAmount(txType, amt);
        const payload = {
          description: name, amount: Number(signed.toFixed(2)), date: due,
          categoryId, accountId, planned: false, status, type: txType,
        };
        return isEdit ? deps.transactions.update((existing as PayableListItem).id, payload) : deps.transactions.create(payload);
      },
      success: () => (isEdit ? 'Conta atualizada' : 'Conta criada'),
      failure: 'Falha ao salvar conta',
      onDone: loadData,
    });

    const $form = m.$body.find('form[data-form=ap]');
    bindCurrencyMask($form.find('input[name=amount]'));

    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      const t = $(this).attr('data-type') as string;
      $form.find('input[name=type]').val(t);
      m.$body.find('[data-region=type-row]').html(typeToggleHtml(TYPE_OPTIONS, t));
      // Categoria não sobrevive à troca de tipo (nature muda) — sem keepId, sempre reseta ao placeholder.
      m.$body.find('[data-region=category-select-wrap]').html(categoryFieldHtml(t, '', null));
    });
  }

  // Delete/mark-paid delegam para TransactionsApi (contrato público da fatia irmã transactions) —
  // ganha de brinde o escopo de parcelas (SINGLE/FUTURE/ALL) no delete.
  function openDeleteModal(item: PayableListItem): void {
    deps.transactions.openDeleteFlow(item, { onDone: loadData });
  }

  function markPaidItem(item: PayableListItem): void {
    deps.transactions.markPaid(item, { onDone: loadData });
  }

  function closeAllMenus(): void {
    if (!state || !state.$root) return;
    state.$root.find('.menu').hide();
  }

  function bindRoot($root: JQuery): void {
    bindRecordActions($root, '.ap', { find: findItem, onNew: () => openFormModal(null), onEdit: openFormModal, onDelete: openDeleteModal, before: closeAllMenus });

    $root.on('click.ap', '[data-act=menu]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const $menu = $root.find('.menu[data-menu-for="' + id + '"]');
      const wasOpen = $menu.is(':visible');
      closeAllMenus();
      if (!wasOpen) $menu.show();
    });

    $root.on('click.ap', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      closeAllMenus();
      const item = findItem($(this).attr('data-id') as string);
      if (item) markPaidItem(item);
    });

    $(document).on('click.ap-outside', closeAllMenus);
  }

  return createPage<AccountsPayablePageState>({
    ns: '.ap',
    state: () => {
      const p = deps.periodService.get();
      state = { loading: true, payable: [], receivable: [], tab: 'payable', month: p.month - 1, year: p.year };
      return state;
    },
    render,
    bind: bindRoot,
    onMount: () => {
      loadData();
    },
    onUnmount: () => {
      $(document).off('click.ap-outside');
    },
  });
}
