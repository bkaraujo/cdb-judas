/**
 * A linha de extrato compartilhada entre `statement`, o bloco de card-statement em
 * `credit-cards` e `transactions` (Fase 6): as três telas renderizavam a mesma linha (~85%
 * idêntica) e já haviam divergido em detalhes que deveriam ser opção, não acidente.
 *
 * Duas famílias de layout, escolhidas por `opts.status`:
 *   'dot'   — extrato de conta/cartão: sem índice, com coluna de saldo acumulado, ações reveladas
 *             no hover (.stm-row-actions), linha de "saldo anterior"/"fatura anterior" tratada
 *             como cabeçalho (Domain.StatementItem.isBalanceHeader).
 *   'badge' — lançamentos: ordinal opcional, badge de status em vez de dot, sem coluna de saldo,
 *             ações sempre visíveis.
 *
 * `categories`/`accounts`/`tags`: catálogos completos, passados explicitamente pelo chamador (ex.:
 * `cacheStore.categories()`) em vez de lidos de um cache global — memoização de `statementColumns`
 * (larguras de coluna) é responsabilidade de quem chama (a página já resincroniza via
 * `cachePage`/`CacheStore.subscribe`), não deste módulo.
 */
import type { Account } from '../../_0_domain/account.ts';
import type { Category } from '../../_0_domain/category.ts';
import { categoryById, categoryLabel, esc, flatCategories, fmt, fmtDate, valueColor } from '../../_0_domain/format.ts';
import * as StatementItem from '../../_0_domain/statement-item.ts';
import * as Transaction from '../../_0_domain/transaction.ts';
import type { Tag } from '../../_0_domain/tag.ts';
import { tagFlagHtml } from './ui/tags-dropdown.ts';

export interface StatementColumns {
  catColCh: number;
  accColCh: number;
}

/** Largura fixa das colunas categoria/conta (maior label possível), pra descrição sempre começar
 * no mesmo x independente da linha. Roda o catálogo inteiro — chamar uma vez por render (não por
 * linha) e repassar o resultado a `statementRowHtml`. */
export function statementColumns(categories: readonly Category[], accounts: readonly Account[]): StatementColumns {
  const catLens = flatCategories(categories).map((c) => c.label.length);
  const catColCh = (catLens.length ? Math.max(...catLens) : 12) + 1;
  const accLens = accounts.map((a) => (a.name || '').length);
  const accColCh = (accLens.length ? Math.max(...accLens) : 10) + 1;
  return { catColCh, accColCh };
}

export function rowCountLabel(n: number): string {
  return n + (n === 1 ? ' transação exibida' : ' transações exibidas');
}

export interface StatementRowLike {
  id?: string | null;
  date?: string | null;
  description?: string;
  amount: number;
  status?: string;
  categoryId?: string | null;
  cardId?: string | null;
  invoice?: boolean;
  installmentNumber?: number | null;
  totalInstallments?: number | null;
  tagIds?: string[];
  runningBal?: number;
  type?: string;
}

export interface StatementRowOptions {
  index?: number;
  isLast?: boolean;
  showAccount?: boolean;
  accountName?: string;
  showBalance?: boolean;
  status?: 'dot' | 'badge';
  invoiceLink?: boolean;
  actions?: (row: StatementRowLike) => string;
}

/** row: StatementItem (dot) ou transação crua (badge) — ambas têm date/description/amount/
 * status/categoryId/tagIds/invoice; StatementItem soma runningBal. */
export function statementRowHtml(row: StatementRowLike, categories: readonly Category[], tags: readonly Tag[], cols: StatementColumns, opts: StatementRowOptions = {}): string {
  const isHeader = StatementItem.isBalanceHeader(row as StatementItem.StatementRow);
  const catMap = categoryById(categories);
  const cat = isHeader ? null : (row.categoryId != null ? catMap[row.categoryId] : undefined);
  const catLbl = cat ? categoryLabel(categories, cat) : '';
  const amt = Number(row.amount) || 0;

  const rowCls = 'stm-row' + (isHeader ? ' is-header' : '') + (opts.isLast ? ' is-last' : '');

  // Larguras fixas (catColCh/accColCh) são dinâmicas por catálogo — inline de propósito.
  const catStyle = 'flex:0 0 ' + cols.catColCh + 'ch;width:' + cols.catColCh + 'ch;';

  const indexHtml = opts.index != null ? '<span class="stm-cell-index">' + esc(opts.index) + '</span>' : '';

  const accHtml = opts.showAccount
    ? '<span class="stm-cell-acc" style="flex:0 0 ' + cols.accColCh + 'ch;width:' + cols.accColCh + 'ch;">' + esc(opts.accountName || '—') + '</span>'
    : '';

  // Transferência: `cat.isSystem` já resolvido acima é exclusivo da categoria de sistema
  // "Transferência" (ver `Category.isTransferCategory`) — pula o sufixo "(n de N)" de
  // `describe`, que confundiria as duas pernas com parcela de compra.
  const descText = (cat && cat.isSystem ? row.description || '' : Transaction.describe(row)) || '—';
  const descHtml = opts.invoiceLink && row.invoice
    ? '<a href="#/credit-cards" class="stm-cell-desc stm-cell-desc--link">' + esc(descText) + '</a>'
    : '<span class="stm-cell-desc">' + esc(descText) + '</span>';

  // Extrato (dot): some se for cabeçalho ou valor zerado. Lançamentos (badge): sempre.
  const amountVisible = opts.showBalance ? !isHeader && amt !== 0 : true;
  const amountColor = opts.showBalance ? valueColor(amt) : row.type === 'income' ? 'var(--income)' : row.type === 'transfer' ? 'var(--text-secondary)' : 'var(--expense)';
  const amountHtml = amountVisible ? '<span class="stm-cell-amount" style="color:' + amountColor + ';">' + esc(fmt(amt)) + '</span>' : '';

  const balanceHtml = opts.showBalance
    ? '<span class="stm-cell-balance" style="color:' + valueColor(StatementItem.runningBalance(row as StatementItem.StatementRow)) + ';">' +
      esc(fmt(StatementItem.runningBalance(row as StatementItem.StatementRow))) +
      '</span>'
    : '';

  const stKey = row.status || 'confirmed';
  const statusHtml =
    opts.status === 'badge'
      ? '<span class="badge badge-' + esc(Transaction.statusBadgeVariant(stKey)) + '" style="flex-shrink:0;">' + esc(Transaction.statusLabel(stKey)) + '</span>'
      : (() => {
          const dotVariant = row.status === 'confirmed' ? 'confirmed' : isHeader ? 'balance' : 'pending';
          return '<div class="stm-dot stm-dot--' + dotVariant + '"></div>';
        })();

  const actionsInner = isHeader ? '' : opts.actions ? opts.actions(row) : '';
  const actionsHtml = opts.status === 'badge' ? '<div class="row-actions">' + actionsInner + '</div>' : '<div class="stm-row-actions">' + actionsInner + '</div>';

  const cells =
    opts.status === 'badge'
      ? indexHtml +
        '<span class="stm-cell-date">' + esc(fmtDate(row.date)) + '</span>' +
        accHtml +
        '<span class="stm-cell-cat" style="' + catStyle + '">' + esc(catLbl) + '</span>' +
        tagFlagHtml(tags, row.tagIds) +
        descHtml +
        statusHtml +
        amountHtml +
        actionsHtml
      : '<span class="stm-cell-date">' + esc(row.date ? fmtDate(row.date) : '') + '</span>' +
        '<span class="stm-cell-cat" style="' + catStyle + '">' + esc(catLbl) + '</span>' +
        tagFlagHtml(tags, row.tagIds) +
        descHtml +
        amountHtml +
        balanceHtml +
        statusHtml +
        actionsHtml;

  return '<div class="' + rowCls + '" data-id="' + esc(row.id || '') + '">' + cells + '</div>';
}
