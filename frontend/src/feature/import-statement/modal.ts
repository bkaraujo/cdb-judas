/** Importação de extrato/fatura em PDF. Modal único que cobre dois fluxos, detectados
 * automaticamente pelo backend a partir do PDF:
 *   - CREDIT_CARD_INVOICE: fatura de cartão (Santander/BTG) → escolhe cartão de destino.
 *   - BANK_STATEMENT: extrato de conta corrente (BTG) → escolhe conta + estado (novo/concilia/duplicado).
 *
 * Sem domain/application próprios: lê/escreve transações via TransactionsApi.{importPreview,
 * importConfirm}, regras de nomenclatura via ImportRulesApi.{listCached,match} (fatias irmãs,
 * injetadas como porta). PDFs protegidos pedem senha sob demanda (códigos PASSWORD_REQUIRED /
 * WRONG_PASSWORD).
 */
import $ from 'jquery';
import type { ImportRule } from '@/api/types.ts';
import { esc, flatCategories, fmt, valueColor } from '@/core/kernel/_0_domain/format.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { runMutation } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { openCategoryCreateModal, openTagCreateModal, categoryPickerHtml } from '@/core/kernel/_2_infrastructure/primary/pickers.ts';
import { btn } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import type { Modal } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { modal } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { refreshSearchSelect } from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import { modalFooter } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { refreshTagsDropdownLabel, tagsDropdownHtml } from '@/core/kernel/_2_infrastructure/primary/ui/tags-dropdown.ts';
import { toast } from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';

/** Linha do preview — superset mutável dos dois formatos (fatura/extrato); campos ausentes num
 * fluxo simplesmente não são lidos pelas colunas do outro (ver COLUMNS). */
export interface ImportRowUI {
  checked?: boolean;
  type: string;
  categoryId: string | null;
  planned?: boolean | null;
  description: string;
  tagIds?: string[];
  date: string;
  originalDate?: string;
  amount: number;
  closed?: boolean;
  cardId?: string | null;
  suggestedCardId?: string | null;
  installmentNumber?: number | null;
  installmentTotal?: number | null;
  groupId?: string | null;
  duplicate?: boolean;
  status?: string;
  state?: 'NEW' | 'DUPLICATE' | 'RECONCILE';
  reconcileDescription?: string | null;
}

export interface ImportCardCandidate {
  id: string;
  name: string;
  last4: string | null;
}
export interface ImportAccountCandidate {
  id: string;
  name: string;
}

export interface ImportDataUI {
  rows: ImportRowUI[];
  documentType?: string;
  issuer?: string;
  last4s?: string[];
  candidateCards?: ImportCardCandidate[];
  candidateAccounts?: ImportAccountCandidate[];
  selectedAccountId?: string | null;
  closingPeriod?: string | null;
}

export interface ImportStatementModalOptions {
  onImported?: () => void;
}

export interface ImportTransactionsPort {
  importPreview(file: File, password: string | undefined, accountId: string | undefined | null): Promise<ImportDataUI | null>;
  importConfirm(payload: unknown): Promise<{ created?: number; skipped?: number; reconciled?: number } | null>;
}

export interface ImportRulesPort {
  listCached(): ImportRule[];
  match(description: string | null | undefined, rules: readonly ImportRule[] | null | undefined): ImportRule | null;
}

export interface ImportStatementDeps {
  cache: CacheStore;
  transactions: ImportTransactionsPort;
  importRules: ImportRulesPort;
}

interface ImportColumnCtx {
  kind: string;
  data: ImportDataUI;
}
interface ImportColumn {
  key: string | null;
  label: string;
  sortable: boolean;
  align: string;
  width: string;
  tdWidth?: boolean;
  tdExtra?: string;
  thHtml?: string;
  cell: (row: ImportRowUI, idx: number, ctx: ImportColumnCtx) => string;
}

export function createImportStatementModal(deps: ImportStatementDeps) {
  return function importStatementModal(opts: ImportStatementModalOptions = {}): Modal {
    const onImported = typeof opts.onImported === 'function' ? opts.onImported : () => null;

    const uniq = Date.now();
    const fileId = 'import-file-' + uniq;
    const pwdId = 'import-pwd-' + uniq;
    const cardSelectId = 'import-card-' + uniq;

    const bodyHtml =
      '<div class="form-group">' +
        '<label class="form-label" for="' + fileId + '">Selecionar fatura (PDF)</label>' +
        '<input id="' + fileId + '" type="file" accept=".pdf,application/pdf" ' +
          'style="padding: 24px; border: 2px dashed var(--border); border-radius: var(--radius); background: var(--bg-hover); color: var(--text-primary); cursor: pointer; text-align: center; width: 100%;" />' +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:8px;">Envie o PDF original: fatura do cartão (Santander ou BTG) ou extrato da conta corrente (BTG). O tipo é detectado automaticamente.</p>' +
        '<button type="button" data-act="toggle-password" style="margin-top:6px;background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;padding:0;">PDF protegido? Informar senha</button>' +
      '</div>' +
      '<div class="form-group" data-region="password-block" style="display:none;">' +
        '<label class="form-label" for="' + pwdId + '">Senha do PDF</label>' +
        '<input id="' + pwdId + '" type="password" autocomplete="off" placeholder="Senha da fatura" />' +
        '<p data-region="password-hint" style="font-size:12px;color:var(--expense);margin-top:6px;display:none;"></p>' +
      '</div>';

    const $cancel = btn({ variant: 'ghost', size: 'md', label: 'Cancelar', attrs: 'data-modal-close="1" type="button"' });
    const $import = btn({ variant: 'primary', size: 'md', icon: 'download', label: 'Importar', attrs: 'data-act="do-import" type="button"' });
    const $footer = modalFooter([$cancel, $import]);

    const m = modal({ title: 'Importar Fatura ou Extrato', body: bodyHtml, footer: $footer, persistent: true });
    m.open();
    m.$el.find('.modal-box').css({ width: '70vw', 'min-width': '560px', 'max-height': '90vh', display: 'flex', 'flex-direction': 'column', overflow: 'hidden' });
    m.$body.css({ flex: '1', 'overflow-y': 'scroll', 'min-height': '0', display: 'flex', 'flex-direction': 'column' });

    let selectedFile: File | null = null;
    let cardCandidates: ImportCardCandidate[] = [];
    let selectedAccountId: string | null = null;
    let previewData: ImportDataUI | null = null;
    let statementData: ImportDataUI | null = null;
    let lastPassword: string | null = null;
    let sortCol: string | null = null;
    let sortAsc = true;
    const $pwdBlock = m.$el.find('[data-region=password-block]');
    const $pwd = m.$el.find('#' + pwdId);
    const $pwdHint = m.$el.find('[data-region=password-hint]');

    m.$el.find('#' + fileId).on('change', (e) => {
      const input = e.target as HTMLInputElement;
      selectedFile = (input.files && input.files[0]) || null;
    });

    function revealPassword(message: string): void {
      $pwdBlock.show();
      if (message) $pwdHint.text(message).show();
      $pwd.trigger('focus');
    }

    function fmtIsoDate(iso: string | null | undefined): string {
      const mm = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(iso || ''));
      return mm ? mm[3] + '/' + mm[2] + '/' + mm[1] : iso || '';
    }

    // Fechamento contábil: linha cuja data cai no período fechado não pode virar transação — a
    // confirmação inteira é recusada. A linha fica fora da seleção (checkbox desmarcado e
    // travado) e ganha o ícone de aviso com a explicação no hover.
    function closedMessage(period: string | null | undefined): string {
      return 'Período fechado' + (period ? ' até ' + period : '') + ': este lançamento não pode ser importado.';
    }

    function closedWarningHtml(period: string | null | undefined): string {
      return '<span title="' + esc(closedMessage(period)) + '" style="color:var(--expense);display:inline-flex;vertical-align:middle;margin-left:6px;cursor:help;">' + icon('alertCircle', 14) + '</span>';
    }

    function statusTag(status: string | undefined): string {
      const scheduled = status === 'scheduled';
      const label = scheduled ? 'Agendado' : 'Confirmado';
      const color = scheduled ? 'var(--text-muted)' : 'var(--income)';
      return '<span style="font-size:11px;color:' + color + ';">' + esc(label) + '</span>';
    }

    // Import category set for a given movement type. Memoizado por tipo dentro do passe de
    // render: flatCategories monta o rótulo encadeado de cada categoria e ordena com
    // localeCompare — rodar isso por linha, numa fatura com centenas de linhas, é o custo
    // dominante do preview.
    let catCache: Record<string, { id: string; label: string }[]> = {};

    function importCategoriesFor(type: string, keepId: string | null | undefined): { id: string; label: string }[] {
      const key = type === 'income' ? 'income' : 'expense';
      const cached = catCache[key];
      const covered = cached && (!keepId || cached.some((c) => String(c.id) === String(keepId)));
      if (covered) return cached as { id: string; label: string }[];
      const nature = key === 'income' ? 'INCOME' : 'EXPENSE';
      const categories = deps.cache.categories();
      const byNature = flatCategories(categories, nature, true, keepId);
      const list = byNature.length ? byNature : flatCategories(categories, undefined, true, keepId);
      if (!keepId) catCache[key] = list;
      return list;
    }

    function typeSelectHtml(selectedType: string, idx: number): string {
      const type = selectedType === 'income' ? 'income' : 'expense';
      return (
        '<select data-row-type data-idx="' + idx + '" style="width:auto;font-size:12px;padding:4px 6px;">' +
        '<option value="expense"' + (type === 'expense' ? ' selected' : '') + '>Despesa</option>' +
        '<option value="income"' + (type === 'income' ? ' selected' : '') + '>Receita</option>' +
        '</select>'
      );
    }

    function categoryItems(cats: { id: string; label: string }[], selectedId: string | null | undefined): { value: string; label: string }[] {
      if (!cats.length) return [{ value: '', label: 'Sem categorias' }];
      const items = cats.map((c) => ({ value: c.id, label: c.label }));
      const hasSel = cats.some((c) => String(c.id) === String(selectedId));
      return hasSel ? items : [{ value: '', label: 'Selecione' }, ...items];
    }

    function catSelectId(idx: number): string {
      return 'imp-cat-' + uniq + '-' + idx;
    }

    function categoryComboHtml(cats: { id: string; label: string }[], selectedId: string | null | undefined, idx: number, extraSelectAttrs: string, locked: boolean): string {
      const id = catSelectId(idx);
      const title = locked ? 'Segue a categoria da 1ª parcela do grupo' : '';
      return categoryPickerHtml({
        items: cats.map((c) => ({ value: c.id, label: c.label })),
        selectedId: selectedId || undefined,
        selectId: id,
        selectAttrs: ' data-row-category data-idx="' + idx + '"' + extraSelectAttrs + (cats.length ? '' : ' disabled'),
        lazy: true,
        floating: true,
        compact: true,
        disabled: locked || !cats.length,
        title,
      });
    }

    function categorySelectHtml(selectedId: string | null | undefined, idx: number, groupId: string | null | undefined, locked: boolean, type: string): string {
      const groupAttr = ' data-group-id="' + esc(groupId || '') + '"';
      const lockedAttrs = locked ? ' disabled title="Segue a categoria da 1ª parcela do grupo"' : '';
      return categoryComboHtml(importCategoriesFor(type, selectedId), selectedId, idx, groupAttr + lockedAttrs, locked);
    }

    function plannedSelectHtml(selected: boolean | null | undefined, idx: number): string {
      const selectedVal = selected === true ? 'true' : selected === false ? 'false' : '';
      return (
        '<select data-row-planned data-idx="' + idx + '" style="width:auto;font-size:12px;padding:4px 6px;">' +
          '<option value=""' + (selectedVal === '' ? ' selected' : '') + '>Não definido</option>' +
          '<option value="true"' + (selectedVal === 'true' ? ' selected' : '') + '>Sim</option>' +
          '<option value="false"' + (selectedVal === 'false' ? ' selected' : '') + '>Não</option>' +
        '</select>'
      );
    }

    // Regra de nomenclatura: casa a descrição crua de cada linha contra o cache de regras
    // (qualquer gatilho cadastrado) e, ao bater, aplica categoria/planned (nunca conta,
    // nem descrição) antes da linha ser renderizada. Roda antes de alignGroupFields(), pra a
    // propagação de parcelas herdar o resultado da 1ª parcela.
    function applyImportRules(rows: ImportRowUI[]): void {
      const rules = deps.importRules.listCached();
      if (!rules.length) return;
      rows.forEach((row) => {
        const rule = deps.importRules.match(row.description, rules);
        if (!rule) return;
        if (rule.categoryId) row.categoryId = rule.categoryId;
        if (rule.planned !== null && rule.planned !== undefined) row.planned = rule.planned;
      });
    }

    function quickCreateButtonsHtml(): string {
      return (
        '<div style="display:flex;gap:16px;flex-shrink:0;">' +
        '<button type="button" data-act="new-category" style="background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' + icon('plus', 12) + 'Nova categoria</button>' +
        '<button type="button" data-act="new-tag" style="background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' + icon('plus', 12) + 'Nova tag</button>' +
        '</div>'
      );
    }

    // Lê de volta pro `data.rows` os campos que só são relidos do DOM no confirm/na ordenação —
    // precisa rodar antes de qualquer re-render (sort ou quick-create).
    function syncRowsFromDom(data: ImportDataUI): void {
      m.$el.find('[data-row-include]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        (data.rows[idx] as ImportRowUI).checked = (this as HTMLInputElement).checked;
      });
      m.$el.find('[data-row-type]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        (data.rows[idx] as ImportRowUI).type = (this as HTMLSelectElement).value;
      });
      m.$el.find('[data-row-category]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        (data.rows[idx] as ImportRowUI).categoryId = (this as HTMLSelectElement).value;
      });
      m.$el.find('[data-row-planned]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        const val = (this as HTMLSelectElement).value;
        (data.rows[idx] as ImportRowUI).planned = val === '' ? null : val === 'true';
      });
      m.$el.find('[data-row-description]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        (data.rows[idx] as ImportRowUI).description = (this as HTMLInputElement).value;
      });
      m.$el.find('[data-row-card]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        (data.rows[idx] as ImportRowUI).cardId = (this as HTMLSelectElement).value;
      });
    }

    function cardOptionShort(card: ImportCardCandidate): string {
      return card.last4 ? card.name + ' — •••• ' + card.last4 : card.name;
    }

    // Per-row card selector: o cartão é definido por transação. Só os cartões presentes na
    // fatura são oferecidos.
    function cardSelectHtml(selectedId: string | null | undefined, idx: number): string {
      if (!cardCandidates.length) return '—';
      const hasSel = cardCandidates.some((c) => String(c.id) === String(selectedId));
      const placeholder = hasSel ? '' : '<option value="" selected>Selecione</option>';
      const options =
        placeholder +
        cardCandidates
          .map((c) => {
            const sel = String(c.id) === String(selectedId) ? ' selected' : '';
            return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(cardOptionShort(c)) + '</option>';
          })
          .join('');
      return '<select data-row-card data-idx="' + idx + '" style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    // ── Tabela de preview table-driven ──────────────────────────
    function isDuplicateRow(kind: string, row: ImportRowUI): boolean {
      return kind === 'CREDIT_CARD_INVOICE' ? !!row.duplicate : row.state === 'DUPLICATE';
    }

    function isGroupLocked(row: ImportRowUI): boolean {
      return !!(row.groupId && row.installmentNumber != null && row.installmentNumber !== 1);
    }

    function rowCheckedAttr(kind: string, row: ImportRowUI): string {
      if (row.closed) return '';
      if (row.checked !== undefined) return row.checked ? 'checked' : '';
      return isDuplicateRow(kind, row) ? '' : 'checked';
    }

    function descriptionSuffix(kind: string, row: ImportRowUI): string {
      if (kind === 'CREDIT_CARD_INVOICE') {
        return row.duplicate ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">já importado</span>' : '';
      }
      return row.state === 'RECONCILE' && row.reconcileDescription ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">↔ ' + esc(row.reconcileDescription) + '</span>' : '';
    }

    function amountCellHtml(kind: string, row: ImportRowUI): string {
      if (kind === 'BANK_STATEMENT') {
        const amt = Number(row.amount) || 0;
        return '<span style="color:' + valueColor(amt) + ';">' + esc(fmt(amt)) + '</span>';
      }
      return esc(fmt(row.amount));
    }

    function colInclude(): ImportColumn {
      return {
        key: null, label: '', sortable: false, align: 'center', width: '34px',
        thHtml: '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />',
        cell: (row, idx, ctx) => {
          const closed = !!row.closed;
          const closingPeriod = ctx.data && ctx.data.closingPeriod;
          return (
            '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + rowCheckedAttr(ctx.kind, row) +
            (closed ? ' disabled title="' + esc(closedMessage(closingPeriod)) + '"' : '') + ' ' +
            'style="width:16px;height:16px;cursor:pointer;" />'
          );
        },
      };
    }
    function colDate(): ImportColumn {
      return {
        key: 'date', label: 'Data', sortable: true, align: 'left', width: '90px', tdWidth: true,
        tdExtra: 'white-space:nowrap;color:var(--text-secondary);',
        cell: (row, _idx, ctx) => {
          const closingPeriod = ctx.data && ctx.data.closingPeriod;
          return esc(fmtIsoDate(row.date)) + (row.closed ? closedWarningHtml(closingPeriod) : '');
        },
      };
    }
    function colType(): ImportColumn {
      return { key: 'type', label: 'Tipo', sortable: true, align: 'left', width: '', cell: (row, idx) => typeSelectHtml(row.type, idx) };
    }
    function colCategory(): ImportColumn {
      return { key: 'categoryId', label: 'Categoria', sortable: true, align: 'left', width: '', cell: (row, idx) => categorySelectHtml(row.categoryId, idx, row.groupId, isGroupLocked(row), row.type) };
    }
    function colTags(): ImportColumn {
      return {
        key: null, label: 'Tags', sortable: false, align: 'left', width: '',
        cell: (row, idx) => {
          const locked = isGroupLocked(row);
          return tagsDropdownHtml(deps.cache.tags(), row.tagIds, String(idx), { floating: true, compact: true, disabled: locked, title: locked ? 'Segue as tags da 1ª parcela do grupo' : '' });
        },
      };
    }
    function colPlanned(): ImportColumn {
      return { key: 'planned', label: 'Planejado', sortable: true, align: 'left', width: '', cell: (row, idx) => plannedSelectHtml(row.planned, idx) };
    }
    function colInstallment(): ImportColumn {
      return {
        key: 'installmentNumber', label: 'Parcela', sortable: true, align: 'center', width: '', tdExtra: 'color:var(--text-secondary);',
        cell: (row) => {
          const has = row.installmentNumber != null && row.installmentTotal != null;
          return has ? esc(row.installmentNumber + '/' + row.installmentTotal) : '—';
        },
      };
    }
    function colDescription(): ImportColumn {
      return {
        key: 'description', label: 'Descrição', sortable: true, align: 'left', width: '',
        cell: (row, idx, ctx) => {
          const locked = isGroupLocked(row);
          const descLockedAttrs = locked ? ' readonly title="Segue a descrição da 1ª parcela do grupo"' : '';
          return (
            '<input type="text" data-row-description data-idx="' + idx + '" data-group-id="' + esc(row.groupId || '') + '" value="' + esc(row.description) + '"' + descLockedAttrs + ' ' +
            'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;text-transform:uppercase;' + (locked ? 'opacity:0.6;' : '') + '" ' +
            'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
            'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            descriptionSuffix(ctx.kind, row)
          );
        },
      };
    }
    function colCard(): ImportColumn {
      return {
        key: 'suggestedCardId', label: 'Cartão', sortable: true, align: 'left', width: '',
        cell: (row, idx) => cardSelectHtml(row.cardId !== undefined ? row.cardId : row.suggestedCardId, idx),
      };
    }
    function colStatus(): ImportColumn {
      return { key: 'status', label: 'Status', sortable: true, align: 'center', width: '', tdExtra: 'white-space:nowrap;', cell: (row) => statusTag(row.status) };
    }
    function colState(): ImportColumn {
      return { key: 'state', label: 'Estado', sortable: true, align: 'center', width: '', tdExtra: 'white-space:nowrap;', cell: (row) => stateBadge(row.state) };
    }
    function colAmount(): ImportColumn {
      return { key: 'amount', label: 'Valor', sortable: true, align: 'right', width: '', tdExtra: 'font-weight:700;white-space:nowrap;', cell: (row, _idx, ctx) => amountCellHtml(ctx.kind, row) };
    }

    const COLUMNS: Record<string, ImportColumn[]> = {
      CREDIT_CARD_INVOICE: [colInclude(), colDate(), colType(), colCategory(), colTags(), colPlanned(), colInstallment(), colDescription(), colCard(), colStatus(), colAmount()],
      BANK_STATEMENT: [colInclude(), colDate(), colType(), colCategory(), colTags(), colPlanned(), colDescription(), colState(), colAmount()],
    };

    function sortIcon(col: string): string {
      if (sortCol !== col) return '';
      return sortAsc ? ' ↑' : ' ↓';
    }

    function theadCellHtml(col: ImportColumn): string {
      if (col.thHtml) {
        return '<th style="' + (col.width ? 'width:' + col.width + ';' : '') + 'text-align:' + col.align + ';">' + col.thHtml + '</th>';
      }
      const sortAttr = col.sortable ? ' data-sort="' + col.key + '"' : '';
      const cursor = col.sortable ? 'cursor:pointer;' : '';
      const widthStyle = col.width ? 'width:' + col.width + ';' : '';
      const iconStr = col.sortable && col.key ? sortIcon(col.key) : '';
      return '<th' + sortAttr + ' style="' + widthStyle + 'text-align:' + col.align + ';' + cursor + '">' + esc(col.label) + iconStr + '</th>';
    }

    function tdCellHtml(col: ImportColumn, row: ImportRowUI, idx: number, ctx: ImportColumnCtx): string {
      const widthStyle = col.tdWidth && col.width ? 'width:' + col.width + ';' : '';
      return '<td style="' + widthStyle + 'text-align:' + col.align + ';' + (col.tdExtra || '') + '">' + col.cell(row, idx, ctx) + '</td>';
    }

    function renderPreviewTable(kind: string, data: ImportDataUI): string {
      const rows = (data && data.rows) || [];
      if (!rows.length) {
        return '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">' + (kind === 'CREDIT_CARD_INVOICE' ? 'Nenhum lançamento encontrado na fatura.' : 'Nenhum lançamento encontrado no extrato.') + '</p>';
      }
      const cols = COLUMNS[kind] as ImportColumn[];
      const ctx: ImportColumnCtx = { kind, data };
      const theadRow = cols.map(theadCellHtml).join('');
      const tbodyRows = rows
        .map((row, idx) => {
          const cls = isDuplicateRow(kind, row) || row.closed ? ' style="opacity:0.6;"' : '';
          return '<tr' + cls + '>' + cols.map((col) => tdCellHtml(col, row, idx, ctx)).join('') + '</tr>';
        })
        .join('');
      return (
        '<div style="flex:1;overflow-y:scroll;min-height:0;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
        '<table class="import-table"><thead><tr class="import-table-head-row">' + theadRow + '</tr></thead><tbody>' + tbodyRows + '</tbody></table>' +
        '</div>'
      );
    }

    // ── Preview orchestration ────────────────────────────────────
    function renderPreview(kind: string, data: ImportDataUI): void {
      if (kind === 'CREDIT_CARD_INVOICE') previewData = data;
      else statementData = data;
      catCache = {};
      const rows = (data && data.rows) || [];
      const issuer = (data && data.issuer) || 'UNKNOWN';
      const issuerLabel = issuer === 'SANTANDER' ? 'Santander' : issuer === 'BTG' ? 'BTG Pactual' : issuer;

      let bannerTitle: string;
      let bannerExtra: string;
      let belowBanner: string;
      let confirmAct: string;
      let footerMsg: string;

      if (kind === 'CREDIT_CARD_INVOICE') {
        alignGroupFields(rows);
        const candidateCards = (data && data.candidateCards) || [];
        cardCandidates = candidateCards;
        const last4s = (data && data.last4s) || [];
        bannerTitle = 'Banco detectado';
        bannerExtra = last4s.length
          ? '<p style="font-size:12px;color:var(--text-muted);margin-top:2px;">Cartões: ' + esc(last4s.map((l) => '•••• ' + l).join('  ')) + '</p>'
          : '';
        belowBanner = !candidateCards.length && rows.length
          ? '<p style="font-size:12px;color:var(--expense);margin-top:12px;">Nenhum cartão cadastrado corresponde aos 4 últimos dígitos desta fatura. Cadastre o cartão para importar os lançamentos.</p>'
          : '';
        confirmAct = 'do-confirm';
        footerMsg = rows.length + ' lançamento(s) encontrado(s). Revise, ajuste as categorias e o cartão de cada linha e confirme a importação.';
      } else {
        const accounts = (data && data.candidateAccounts) || [];
        selectedAccountId = (data && data.selectedAccountId) || selectedAccountId || null;
        bannerTitle = 'Extrato detectado';
        bannerExtra = '';
        const accOptions =
          (selectedAccountId ? '' : '<option value="" selected>Selecione a conta</option>') +
          accounts.map((a) => '<option value="' + esc(a.id) + '"' + (String(a.id) === String(selectedAccountId) ? ' selected' : '') + '>' + esc(a.name) + '</option>').join('');
        belowBanner = accounts.length
          ? '<div class="form-group" style="margin-top:12px;"><label class="form-label" for="' + cardSelectId + '">Conta de destino</label><select id="' + cardSelectId + '" data-region="account-select">' + accOptions + '</select></div>'
          : '<p style="font-size:12px;color:var(--expense);margin-top:12px;">Nenhuma conta disponível para importação.</p>';
        confirmAct = 'do-statement-confirm';
        footerMsg = rows.length + ' lançamento(s) encontrado(s). Escolha a conta, revise os estados e confirme a importação.';
      }

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + icon('check', 22) + '</span>' +
          '<div style="flex:1;"><p style="font-size:13px;color:var(--text-muted);">' + esc(bannerTitle) + '</p><p style="font-size:18px;font-weight:800;">' + esc(issuerLabel) + '</p>' + bannerExtra + '</div>' +
          quickCreateButtonsHtml() +
        '</div>' +
        belowBanner +
        renderPreviewTable(kind, data) +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' + esc(footerMsg) + '</p>',
      );

      if (kind === 'BANK_STATEMENT') {
        m.$el.find('[data-region=account-select]').on('change', function () {
          selectedAccountId = (this as HTMLSelectElement).value || null;
          refreshStatementPreview();
        });
      }

      m.$el.find('[data-act=do-import]').hide();
      const $foot = m.$el.find('.modal-footer');
      $foot.find('[data-act=do-confirm],[data-act=do-statement-confirm]').remove();
      if (rows.length) {
        $foot.append(btn({ variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação', attrs: 'data-act="' + confirmAct + '" type="button"' }));
      }
    }

    // Coleta as linhas marcadas do DOM (fonte de verdade em edição) — só a fatura exige cardId
    // por linha e carrega os campos de parcela.
    function collectRows(kind: string): { rows: Record<string, unknown>[]; missingCard: boolean; missingCategory: boolean } {
      const src = kind === 'CREDIT_CARD_INVOICE' ? previewData : statementData;
      const rows: Record<string, unknown>[] = [];
      let missingCard = false;
      let missingCategory = false;
      m.$el.find('[data-row-include]').each(function () {
        if (!(this as HTMLInputElement).checked) return;
        const idx = Number($(this).attr('data-idx'));
        const s = src && src.rows && src.rows[idx];
        if (!s) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $planned = m.$el.find('[data-row-planned][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const categoryId = ($cat.val() as string) || null;
        if (!categoryId) missingCategory = true;
        const plannedVal = $planned.length ? ($planned.val() as string) : (s.planned !== null && s.planned !== undefined ? (s.planned ? 'true' : 'false') : '');
        const planned = plannedVal === '' ? undefined : plannedVal === 'true';
        const description = (($desc.val() as string) || s.description || '').trim();
        const row: Record<string, unknown> = {
          description, amount: s.amount, date: s.date, transactionType: s.type,
          categoryId, planned, tagIds: s.tagIds || [],
        };
        if (kind === 'CREDIT_CARD_INVOICE') {
          const $card = m.$el.find('[data-row-card][data-idx="' + idx + '"]');
          const cardId = $card.length ? (($card.val() as string) || null) : null;
          if (!cardId) missingCard = true;
          row.originalDate = s.originalDate;
          row.installmentNumber = s.installmentNumber;
          row.installmentTotal = s.installmentTotal;
          row.cardId = cardId;
        }
        rows.push(row);
      });
      return { rows, missingCard, missingCategory };
    }

    function confirmImport(): void {
      if (!previewData) return;
      const collected = collectRows('CREDIT_CARD_INVOICE');
      if (!collected.rows.length) {
        toast('Selecione ao menos um lançamento', 'error');
        return;
      }
      if (collected.missingCard) {
        toast('Selecione o cartão de cada lançamento', 'error');
        return;
      }
      if (collected.missingCategory) {
        toast('Selecione a categoria de cada lançamento', 'error');
        return;
      }

      const $btn = m.$el.find('[data-act=do-confirm]');
      runMutation(deps.transactions.importConfirm({ type: 'CREDIT_CARD_INVOICE', rows: collected.rows }), {
        $btn,
        failure: 'Falha ao confirmar a importação',
        onDone: (res) => {
          const created = (res && res.created) || 0;
          const skipped = (res && res.skipped) || 0;
          showConfirmSummary(created, skipped);
          return onImported();
        },
      });
    }

    function showConfirmSummary(created: number, skipped: number, reconciled?: number): void {
      const parts = [created + ' criados'];
      if (reconciled) parts.push(reconciled + ' conciliados');
      parts.push(skipped + ' ignorados');
      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + icon('check', 22) + '</span>' +
          '<div><p style="font-size:18px;font-weight:800;">Importação concluída</p><p style="font-size:13px;color:var(--text-muted);margin-top:2px;">' + esc(parts.join(', ')) + '</p></div>' +
        '</div>',
      );
      const $foot = m.$el.find('.modal-footer');
      $foot.empty().append(btn({ variant: 'primary', size: 'md', label: 'Fechar', attrs: 'data-modal-close="1" type="button"' }));
    }

    // ── Bank statement (extrato) preview ───────────────────────
    function routePreview(preview: ImportDataUI | null): void {
      applyImportRules((preview && preview.rows) || []);
      const kind = preview && preview.documentType === 'BANK_STATEMENT' ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE';
      renderPreview(kind, preview as ImportDataUI);
    }

    function stateBadge(st: string | undefined): string {
      if (st === 'DUPLICATE') return '<span class="badge badge-muted">já importado</span>';
      if (st === 'RECONCILE') return '<span class="badge badge-warning">concilia</span>';
      return '<span class="badge badge-income">novo</span>';
    }

    // Re-runs the preview against the chosen account so duplicate/reconcile states refresh.
    function refreshStatementPreview(): void {
      if (!selectedFile) return;
      runMutation(deps.transactions.importPreview(selectedFile, lastPassword || undefined, selectedAccountId), {
        failure: 'Falha ao atualizar o preview',
        onDone: (preview) => {
          if (preview && preview.documentType === 'BANK_STATEMENT') {
            applyImportRules((preview && preview.rows) || []);
            renderPreview('BANK_STATEMENT', preview);
          }
        },
      });
    }

    function confirmStatementImport(): void {
      if (!statementData) return;
      if (!selectedAccountId) {
        toast('Selecione a conta de destino', 'error');
        return;
      }
      const collected = collectRows('BANK_STATEMENT');
      if (!collected.rows.length) {
        toast('Selecione ao menos um lançamento', 'error');
        return;
      }
      if (collected.missingCategory) {
        toast('Selecione a categoria de cada lançamento', 'error');
        return;
      }

      const $btn = m.$el.find('[data-act=do-statement-confirm]');
      runMutation(deps.transactions.importConfirm({ type: 'BANK_STATEMENT', accountId: selectedAccountId, rows: collected.rows }), {
        $btn,
        failure: 'Falha ao confirmar a importação',
        onDone: (res) => {
          showConfirmSummary((res && res.created) || 0, (res && res.skipped) || 0, res ? res.reconciled : undefined);
          return onImported();
        },
      });
    }

    // Parcelamento: as demais parcelas do grupo seguem a categoria/descrição da 1ª (campos
    // travados); aqui alinhamos os valores sugeridos pelo backend antes de qualquer edição do usuário.
    function alignGroupFields(rows: ImportRowUI[]): void {
      const masterByGroup: Record<string, { categoryId: string | null; description: string; type: string; tagIds: string[] }> = {};
      rows.forEach((r) => {
        if (r.groupId && r.installmentNumber === 1) {
          masterByGroup[r.groupId] = { categoryId: r.categoryId, description: r.description, type: r.type, tagIds: (r.tagIds || []).slice() };
        }
      });
      rows.forEach((r) => {
        const master = r.groupId && r.installmentNumber !== 1 ? masterByGroup[r.groupId] : null;
        if (!master) return;
        r.categoryId = master.categoryId;
        r.description = master.description;
        r.type = master.type;
        r.tagIds = master.tagIds.slice();
      });
    }

    // Parcelamento: editar descrição/categoria da 1ª parcela do grupo replica para as demais.
    function propagateGroupEdit(idx: number, field: string, value: unknown): void {
      if (!previewData || !previewData.rows) return;
      const row = previewData.rows[idx];
      if (!row || !row.groupId || row.installmentNumber !== 1) return;
      previewData.rows.forEach((r, i) => {
        if (i === idx || r.groupId !== row.groupId) return;
        (r as unknown as Record<string, unknown>)[field] = value;
        if (field === 'description') {
          m.$el.find('[data-row-description][data-idx="' + i + '"]').val(value as string);
        } else if (field === 'categoryId') {
          m.$el.find('[data-row-category][data-idx="' + i + '"]').val(value as string);
          refreshSearchSelect(catSelectId(i));
        } else if (field === 'type') {
          m.$el.find('[data-row-type][data-idx="' + i + '"]').val(value as string);
          refreshCategoryOptions(i, value as string);
        } else if (field === 'tagIds') {
          const ids = ((value as string[]) || []).map(String);
          const $checks = m.$el.find('[data-region=tags-dropdown][data-idx="' + i + '"] [data-tag-check]');
          $checks.each(function () {
            $(this).prop('checked', ids.indexOf(String($(this).attr('data-tag-id'))) !== -1);
          });
          if ($checks.length) refreshTagsDropdownLabel($checks.first());
        }
      });
    }

    // Rebuilds the category <select> of a row so its options track the row's current type;
    // called on load and whenever the user flips the type dropdown.
    function refreshCategoryOptions(idx: number, type: string): void {
      const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
      if (!$cat.length) return;
      const currentId = $cat.val() as string;
      const cats = importCategoriesFor(type, currentId);
      const stillValid = cats.some((c) => String(c.id) === String(currentId));
      const selId = stillValid ? currentId : '';
      const optionsHtml = categoryItems(cats, selId)
        .map((it) => '<option value="' + esc(it.value) + '"' + (String(it.value) === String(selId) ? ' selected' : '') + '>' + esc(it.label) + '</option>')
        .join('');
      $cat.html(optionsHtml).prop('disabled', !cats.length);
      refreshSearchSelect(catSelectId(idx));
      const data = previewData || statementData;
      if (data && data.rows && data.rows[idx]) (data.rows[idx] as ImportRowUI).categoryId = selId || null;
    }

    m.$el.on('input', '[data-row-description]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].description = (this as HTMLInputElement).value;
      propagateGroupEdit(idx, 'description', (this as HTMLInputElement).value);
    });

    m.$el.on('change', '[data-row-type]', function () {
      const idx = Number($(this).attr('data-idx'));
      const value = (this as HTMLSelectElement).value;
      const data = previewData || statementData;
      if (data && data.rows && data.rows[idx]) data.rows[idx].type = value;
      refreshCategoryOptions(idx, value);
      propagateGroupEdit(idx, 'type', value);
    });

    m.$el.on('change', '[data-row-category]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].categoryId = (this as HTMLSelectElement).value;
      propagateGroupEdit(idx, 'categoryId', (this as HTMLSelectElement).value);
    });

    m.$el.on('change', '[data-tag-check]', function () {
      const idx = Number($(this).attr('data-idx'));
      const tagId = String($(this).attr('data-tag-id'));
      const data = previewData || statementData;
      if (!data || !data.rows || !data.rows[idx]) return;
      const row = data.rows[idx] as ImportRowUI;
      const current = (row.tagIds || []).map(String);
      row.tagIds = (this as HTMLInputElement).checked ? current.concat([tagId]) : current.filter((id) => id !== tagId);
      refreshTagsDropdownLabel($(this));
      propagateGroupEdit(idx, 'tagIds', row.tagIds);
    });

    m.$el.on('change', '[data-row-planned]', function () {
      const idx = Number($(this).attr('data-idx'));
      const val = (this as HTMLSelectElement).value;
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].planned = val === '' ? null : val === 'true';
    });

    // Quick-create no topo: sem linha de origem, o Tipo (Despesa/Receita) fica editável no modal
    // (`nature: null`). O re-render inteiro reaproveita o mesmo caminho da ordenação.
    function reRenderPreview(): void {
      const data = previewData || statementData;
      if (!data) return;
      syncRowsFromDom(data);
      renderPreview(statementData ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE', data);
    }

    m.$el.on('click', '[data-act=new-category]', (e) => {
      e.preventDefault();
      openCategoryCreateModal(deps.cache.categories(), null, (created) => {
        deps.cache.upsert('categories', created as Record<string, unknown>);
        catCache = {};
        reRenderPreview();
      });
    });

    m.$el.on('click', '[data-act=new-tag]', (e) => {
      e.preventDefault();
      openTagCreateModal((created) => {
        deps.cache.upsert('tags', created as Record<string, unknown>);
        reRenderPreview();
      });
    });

    m.$el.on('click', '[data-act=toggle-password]', () => revealPassword(''));

    m.$el.on('click', '[data-act=do-confirm]', () => confirmImport());

    m.$el.on('click', '[data-act=do-statement-confirm]', () => confirmStatementImport());

    m.$el.on('click', '[data-act=do-import]', function () {
      if (!selectedFile) {
        toast('Selecione um arquivo PDF', 'error');
        return;
      }
      const password = ((($pwd.val() as string) || '')).trim();
      lastPassword = password || null;
      const $btn = $(this);

      runMutation(deps.transactions.importPreview(selectedFile, lastPassword || undefined, null), {
        $btn,
        failure: 'Falha ao importar a fatura',
        onDone: (preview) => {
          routePreview(preview);
        },
        onError: (err) => {
          const apiErr = err as { code?: string };
          const code = apiErr && apiErr.code;
          if (code === 'PASSWORD_REQUIRED') {
            revealPassword('Este PDF está protegido. Informe a senha para continuar.');
            return true;
          }
          if (code === 'WRONG_PASSWORD') {
            revealPassword('Senha incorreta. Verifique e tente novamente.');
            return true;
          }
          return false;
        },
      });
    });

    m.$el.on('change', '[data-act=select-all]', function () {
      const checked = (this as HTMLInputElement).checked;
      // Linha em período fechado tem o checkbox travado: marcar tudo nunca a inclui.
      m.$el.find('[data-row-include]').not(':disabled').prop('checked', checked);
    });

    m.$el.on('click', '[data-sort]', function () {
      const col = $(this).attr('data-sort') as string;
      const isStatement = !!statementData;
      const data = (isStatement ? statementData : previewData) as ImportDataUI;
      const kind = isStatement ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE';

      syncRowsFromDom(data);

      if (sortCol === col) sortAsc = !sortAsc;
      else {
        sortCol = col;
        sortAsc = true;
      }

      data.rows.sort((a, b) => {
        let va: unknown = (a as unknown as Record<string, unknown>)[col];
        let vb: unknown = (b as unknown as Record<string, unknown>)[col];
        if (va == null) va = '';
        if (vb == null) vb = '';
        if (typeof va === 'string') va = va.toLowerCase();
        if (typeof vb === 'string') vb = vb.toLowerCase();

        if ((va as string | number) < (vb as string | number)) return sortAsc ? -1 : 1;
        if ((va as string | number) > (vb as string | number)) return sortAsc ? 1 : -1;
        return 0;
      });

      renderPreview(kind, data);
    });

    return m;
  };
}
