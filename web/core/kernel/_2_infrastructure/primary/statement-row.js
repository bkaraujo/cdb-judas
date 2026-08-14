/* _3_infrastructure/primary/statement-row.js — a linha de extrato compartilhada entre
 * feature/statement.js, o bloco de card-statement em feature/credit-cards.js e
 * feature/transactions.js: as três telas renderizavam a mesma linha (~85% idêntica) e já haviam
 * divergido em detalhes que deveriam ser opção, não acidente (ver .claude/plan.md Fase 2.2).
 *
 * Duas famílias de layout, escolhidas por `opts.status`:
 *   'dot'   — extrato de conta/cartão: sem índice, com coluna de saldo acumulado, ações reveladas
 *             no hover (.stm-row-actions), linha de "saldo anterior"/"fatura anterior" tratada
 *             como cabeçalho (Domain.StatementItem.isBalanceHeader).
 *   'badge' — lançamentos: ordinal opcional, badge de status em vez de dot, sem coluna de saldo,
 *             ações sempre visíveis.
 */
(function () {
  let columnsCache = null;
  function invalidateColumns() { columnsCache = null; }
  if (window.App && window.App.CacheStore && window.App.CacheStore.subscribe) {
    window.App.CacheStore.subscribe('CATEGORY', invalidateColumns);
    window.App.CacheStore.subscribe('ACCOUNT', invalidateColumns);
  }

  /* Largura fixa das colunas categoria/conta (maior label possível), pra descrição sempre
   * começar no mesmo x independente da linha. flatCategories()/accounts() rodam o catálogo
   * inteiro — memoizado aqui porque as 3 telas recalculavam isso a cada render. */
  function statementColumns() {
    if (columnsCache) return columnsCache;
    const catLens = window.flatCategories().map(function (c) { return c.label.length; });
    const catColCh = (catLens.length ? Math.max.apply(null, catLens) : 12) + 1;
    const accs = window.App.CacheStore.accounts();
    const accLens = accs.map(function (a) { return (a.name || '').length; });
    const accColCh = (accLens.length ? Math.max.apply(null, accLens) : 10) + 1;
    columnsCache = { catColCh: catColCh, accColCh: accColCh };
    return columnsCache;
  }

  function rowCountLabel(n) {
    return n + (n === 1 ? ' transação exibida' : ' transações exibidas');
  }

  /* row: StatementItem (dot) ou transação crua (badge) — ambas têm date/description/amount/
   * status/categoryId/tagIds/invoice; StatementItem soma runningBal.
   * opts: { index, isLast, showAccount, accountName, showBalance, status: 'dot'|'badge',
   *         invoiceLink, actions: fn(row) => html } */
  function statementRowHtml(row, opts) {
    opts = opts || {};
    const cols = statementColumns();
    const isHeader = window.Domain.StatementItem.isBalanceHeader(row);
    const catMap = window.categoryById();
    const cat = isHeader ? null : catMap[row.categoryId];
    const catLbl = cat ? window.categoryLabel(cat) : '';
    const amt = Number(row.amount) || 0;

    const rowCls = 'stm-row' + (isHeader ? ' is-header' : '') + (opts.isLast ? ' is-last' : '');

    // Larguras fixas (catColCh/accColCh) são dinâmicas por catálogo — inline de propósito.
    const catStyle = 'flex:0 0 ' + cols.catColCh + 'ch;width:' + cols.catColCh + 'ch;';

    const indexHtml = opts.index != null
      ? '<span class="stm-cell-index">' + esc(opts.index) + '</span>'
      : '';

    const accHtml = opts.showAccount
      ? '<span class="stm-cell-acc" style="flex:0 0 ' + cols.accColCh + 'ch;width:' + cols.accColCh + 'ch;">' +
          esc(opts.accountName || '—') +
        '</span>'
      : '';

    const descText = window.Domain.Transaction.describe(row) || '—';
    const descHtml = (opts.invoiceLink && row.invoice)
      ? '<a href="#/credit-cards" class="stm-cell-desc stm-cell-desc--link">' + esc(descText) + '</a>'
      : '<span class="stm-cell-desc">' + esc(descText) + '</span>';

    // Extrato (dot): some se for cabeçalho ou valor zerado. Lançamentos (badge): sempre.
    const amountVisible = opts.showBalance ? (!isHeader && amt !== 0) : true;
    const amountColor = opts.showBalance ? window.valueColor(amt) :
      (row.type === 'income' ? 'var(--income)' : row.type === 'transfer' ? 'var(--text-secondary)' : 'var(--expense)');
    const amountHtml = amountVisible
      ? '<span class="stm-cell-amount" style="color:' + amountColor + ';">' + esc(fmt(amt)) + '</span>'
      : '';

    const balanceHtml = opts.showBalance
      ? '<span class="stm-cell-balance" style="color:' + window.valueColor(window.Domain.StatementItem.runningBalance(row)) + ';">' +
          esc(fmt(window.Domain.StatementItem.runningBalance(row))) +
        '</span>'
      : '';

    const stKey = row.status || 'confirmed';
    const statusHtml = opts.status === 'badge'
      ? '<span class="badge badge-' + esc(window.Domain.Transaction.statusBadgeVariant(stKey)) + '" style="flex-shrink:0;">' +
          esc(window.Domain.Transaction.statusLabel(stKey)) +
        '</span>'
      : (function () {
          const dotVariant =
            row.status === 'confirmed' ? 'confirmed' :
            isHeader ? 'balance' : 'pending';
          return '<div class="stm-dot stm-dot--' + dotVariant + '"></div>';
        })();

    const actionsInner = isHeader ? '' : (opts.actions ? opts.actions(row) : '');
    const actionsHtml = opts.status === 'badge'
      ? '<div class="row-actions">' + actionsInner + '</div>'
      : '<div class="stm-row-actions">' + actionsInner + '</div>';

    const cells = opts.status === 'badge'
      ? (indexHtml +
          '<span class="stm-cell-date">' + esc(fmtDate(row.date)) + '</span>' +
          accHtml +
          '<span class="stm-cell-cat" style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(row.tagIds) +
          descHtml +
          statusHtml +
          amountHtml +
          actionsHtml)
      : ('<span class="stm-cell-date">' + esc(row.date ? fmtDate(row.date) : '') + '</span>' +
          '<span class="stm-cell-cat" style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(row.tagIds) +
          descHtml +
          amountHtml +
          balanceHtml +
          statusHtml +
          actionsHtml);

    return '<div class="' + rowCls + '" data-id="' + esc(row.id || '') + '">' + cells + '</div>';
  }

  window.statementColumns = statementColumns;
  window.statementRowHtml = statementRowHtml;
  window.rowCountLabel = rowCountLabel;
})();
