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

    const rowStyle =
      'display:flex;align-items:center;gap:16px;padding:11px 20px;' +
      (opts.isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
      'background:' + (isHeader ? 'var(--bg-hover)' : 'transparent') + ';' +
      'transition:background var(--transition);';

    const descStyle =
      'flex:1;font-size:13px;font-weight:' + (isHeader ? '700' : '500') + ';' +
      'color:' + (isHeader ? 'var(--text-secondary)' : 'var(--text-primary)') + ';' +
      'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;';

    const catStyle =
      'flex:0 0 ' + cols.catColCh + 'ch;width:' + cols.catColCh + 'ch;' +
      'font-size:12px;color:var(--text-muted);' +
      'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

    const indexHtml = opts.index != null
      ? '<span style="font-size:12px;color:var(--text-muted);min-width:24px;text-align:left;">' +
          esc(opts.index) +
        '</span>'
      : '';

    const accHtml = opts.showAccount
      ? '<span style="flex:0 0 ' + cols.accColCh + 'ch;width:' + cols.accColCh + 'ch;' +
          'font-size:12px;color:var(--text-muted);text-align:left;' +
          'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
          esc(opts.accountName || '—') +
        '</span>'
      : '';

    const descText = window.Domain.Transaction.describe(row) || '—';
    const descHtml = (opts.invoiceLink && row.invoice)
      ? '<a href="#/credit-cards" style="' + descStyle + 'color:var(--accent);text-decoration:none;">' +
          esc(descText) +
        '</a>'
      : '<span style="' + descStyle + '">' + esc(descText) + '</span>';

    // Extrato (dot): some se for cabeçalho ou valor zerado. Lançamentos (badge): sempre.
    const amountVisible = opts.showBalance ? (!isHeader && amt !== 0) : true;
    const amountColor = opts.showBalance ? window.valueColor(amt) :
      (row.type === 'income' ? 'var(--income)' : row.type === 'transfer' ? 'var(--text-secondary)' : 'var(--expense)');
    const amountHtml = amountVisible
      ? '<span style="font-size:13px;font-weight:700;color:' + amountColor + ';min-width:100px;text-align:right;">' +
          esc(fmt(amt)) +
        '</span>'
      : '';

    const balanceHtml = opts.showBalance
      ? '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(window.Domain.StatementItem.runningBalance(row)) + ';' +
          'min-width:100px;text-align:right;">' + esc(fmt(window.Domain.StatementItem.runningBalance(row))) + '</span>'
      : '';

    const stKey = row.status || 'confirmed';
    const statusHtml = opts.status === 'badge'
      ? '<span class="badge badge-' + esc(window.Domain.Transaction.statusBadgeVariant(stKey)) + '" style="flex-shrink:0;">' +
          esc(window.Domain.Transaction.statusLabel(stKey)) +
        '</span>'
      : (function () {
          const dotColor =
            row.status === 'confirmed' ? 'var(--income)' :
            isHeader ? 'var(--text-muted)' : 'var(--warning)';
          return '<div style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + dotColor + ';"></div>';
        })();

    const actionsInner = isHeader ? '' : (opts.actions ? opts.actions(row) : '');
    const actionsHtml = opts.status === 'badge'
      ? '<div style="display:flex;align-items:center;justify-content:flex-end;gap:2px;width:40px;flex-shrink:0;">' + actionsInner + '</div>'
      : '<div class="stm-row-actions">' + actionsInner + '</div>';

    const cells = opts.status === 'badge'
      ? (indexHtml +
          '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' + esc(fmtDate(row.date)) + '</span>' +
          accHtml +
          '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(row.tagIds) +
          descHtml +
          statusHtml +
          amountHtml +
          actionsHtml)
      : ('<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
          esc(row.date ? fmtDate(row.date) : '') + '</span>' +
          '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(row.tagIds) +
          descHtml +
          amountHtml +
          balanceHtml +
          statusHtml +
          actionsHtml);

    return '<div class="stm-row" data-id="' + esc(row.id || '') + '" style="' + rowStyle + '">' + cells + '</div>';
  }

  window.statementColumns = statementColumns;
  window.statementRowHtml = statementRowHtml;
  window.rowCountLabel = rowCountLabel;
})();
