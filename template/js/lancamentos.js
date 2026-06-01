/* lancamentos.js — Tabela de lançamentos com filtros e modal */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, D = SAS.data, fmt = SAS.fmt;

  var txs = D.SAMPLE_TX.slice();
  var search = '';
  var filterType = 'all';

  function filtered() {
    return txs.filter(function (t) {
      var ms = t.desc.toLowerCase().indexOf(search.toLowerCase()) >= 0 || t.cat.toLowerCase().indexOf(search.toLowerCase()) >= 0;
      var mt = filterType === 'all' || t.type === filterType;
      return ms && mt;
    });
  }

  function summaryCard(iconName, color, label, value) {
    return '<div class="card" style="flex:1;padding:14px 18px;display:flex;align-items:center;gap:12px">' +
      ui.icon(iconName, { size: 20, color: 'var(--' + color + ')' }) +
      '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600">' + label + '</p>' +
      '<p style="font-size:18px;font-weight:800;color:var(--' + color + ')">' + fmt(value) + '</p></div></div>';
  }

  function filterBtn(val, label) {
    var active = filterType === val;
    var style = active
      ? 'background:var(--accent);color:#fff;border:1px solid var(--accent)'
      : 'background:transparent;color:var(--text-secondary);border:1px solid var(--border)';
    return '<button data-filter="' + val + '" style="padding:6px 14px;border-radius:var(--radius-sm);font-size:13px;font-weight:500;cursor:pointer;' + style + '">' + label + '</button>';
  }

  function render() {
    var list = filtered();
    var totalIncome = list.filter(function (t) { return t.type === 'income'; }).reduce(function (s, t) { return s + t.amount; }, 0);
    var totalExpense = list.filter(function (t) { return t.type === 'expense'; }).reduce(function (s, t) { return s + Math.abs(t.amount); }, 0);
    var resultado = totalIncome - totalExpense;

    var h = '';
    h += '<div class="page-header"><h1>Lançamentos</h1><div class="page-header-actions">' +
      ui.btn('Exportar', { variant: 'secondary', size: 'sm', icon: 'download' }) +
      ui.btn('Novo Lançamento', { size: 'sm', icon: 'plus', attrs: 'data-act="novo"' }) + '</div></div>';

    h += '<div class="flex" style="gap:12px;margin-bottom:18px">' +
      summaryCard('arrowUp', 'income', 'RECEITAS', totalIncome) +
      summaryCard('arrowDown', 'expense', 'DESPESAS', totalExpense) +
      summaryCard('trendingUp', resultado >= 0 ? 'income' : 'expense', 'RESULTADO', resultado) + '</div>';

    h += '<div class="flex items-center" style="gap:10px;margin-bottom:14px">' +
      '<div style="position:relative;flex:1;max-width:320px">' +
      '<div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted)">' + ui.icon('search', { size: 15 }) + '</div>' +
      '<input id="lanc-search" placeholder="Pesquisar lançamentos..." value="' + ui.esc(search) + '" style="padding-left:32px">' +
      '</div>' +
      filterBtn('all', 'Todos') + filterBtn('income', 'Receitas') + filterBtn('expense', 'Despesas') + filterBtn('transfer', 'Transferências') +
      '</div>';

    h += '<div class="card flush"><table class="data-table"><thead><tr>' +
      '<th>Data</th><th>Descrição</th><th>Categoria</th><th>Conta</th><th>Status</th><th class="right">Valor</th><th></th>' +
      '</tr></thead><tbody>';
    list.forEach(function (tx) {
      var color = tx.amount > 0 ? 'var(--income)' : tx.type === 'transfer' ? 'var(--text-secondary)' : 'var(--expense)';
      h += '<tr>' +
        '<td class="muted" style="font-size:12px">' + SAS.fmtDate(tx.date) + '</td>' +
        '<td style="font-weight:500">' + tx.desc + '</td>' +
        '<td>' + ui.badge(tx.cat, 'muted') + '</td>' +
        '<td class="sec" style="font-size:12px">' + tx.conta + '</td>' +
        '<td>' + ui.badge(D.STATUS_LABEL[tx.status], D.STATUS_COLOR[tx.status]) + '</td>' +
        '<td class="right" style="font-weight:700;color:' + color + '">' + (tx.amount > 0 ? '+' : '') + fmt(tx.amount) + '</td>' +
        '<td style="padding:11px 12px"><button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></td>' +
        '</tr>';
    });
    h += '</tbody></table></div>';
    return h;
  }

  function openAdd() {
    var form = { type: 'expense', desc: '', amount: '', cat: 'Alimentação', conta: 'Nubank', date: new Date().toISOString().slice(0, 10), status: 'confirmed' };
    function typeBtn(val, label, color) {
      var active = form.type === val;
      var style = active
        ? 'border:1px solid var(--' + color + ');background:var(--' + color + '-light);color:var(--' + color + ')'
        : 'border:1px solid var(--border);background:transparent;color:var(--text-secondary)';
      return '<button type="button" data-type="' + val + '" style="flex:1;padding:8px;border-radius:var(--radius-sm);font-size:13px;font-weight:600;cursor:pointer;' + style + '">' + label + '</button>';
    }
    function body() {
      return '<div class="flex" style="gap:8px;margin-bottom:16px" id="type-row">' +
        typeBtn('expense', '↓ Despesa', 'expense') + typeBtn('income', '↑ Receita', 'income') + typeBtn('transfer', '⇄ Transferência', 'accent') +
        '</div>' +
        '<div class="form-grid">' +
        '<div class="form-group full"><label class="form-label">Descrição</label><input data-f="desc" placeholder="Ex: Mercado, Salário..."></div>' +
        '<div class="form-group"><label class="form-label">Valor (R$)</label><input data-f="amount" type="number" min="0" step="0.01" placeholder="0,00"></div>' +
        '<div class="form-group"><label class="form-label">Data</label><input data-f="date" type="date" value="' + form.date + '"></div>' +
        '<div class="form-group"><label class="form-label">Categoria</label><select data-f="cat">' + D.CATS.map(function (c) { return '<option>' + c + '</option>'; }).join('') + '</select></div>' +
        '<div class="form-group"><label class="form-label">Conta</label><select data-f="conta">' + D.CONTAS_NOMES.map(function (c) { return '<option>' + c + '</option>'; }).join('') + '</select></div>' +
        '<div class="form-group full"><label class="form-label">Status</label><select data-f="status"><option value="confirmed">Confirmado</option><option value="pending">Pendente</option><option value="scheduled">Agendado</option></select></div>' +
        '</div>';
    }
    var footer = ui.btn('Cancelar', { variant: 'secondary', attrs: 'data-modal-close' }) + ui.btn('Salvar', { attrs: 'data-save' });
    ui.openModal({
      title: 'Novo Lançamento', body: body(), footer: footer,
      onMount: function ($m) {
        $m.on('click', '[data-type]', function () {
          form.type = $(this).data('type');
          $m.find('#type-row').replaceWith($('<div class="flex" style="gap:8px;margin-bottom:16px" id="type-row">' +
            typeBtn('expense', '↓ Despesa', 'expense') + typeBtn('income', '↑ Receita', 'income') + typeBtn('transfer', '⇄ Transferência', 'accent') + '</div>'));
        });
        $m.on('input change', '[data-f]', function () { form[$(this).data('f')] = this.value; });
        $m.on('click', '[data-save]', function () {
          if (!form.desc || !form.amount) return;
          var amt = parseFloat(form.amount);
          txs.unshift({
            id: Date.now(), desc: form.desc, cat: form.cat, conta: form.conta,
            date: form.date, type: form.type,
            amount: form.type === 'expense' ? -Math.abs(amt) : Math.abs(amt),
            status: form.status
          });
          ui.closeModal();
          SAS.app.rerender();
        });
      }
    });
  }

  function mount($page) {
    $page.off('click.lanc').on('click.lanc', '[data-act="novo"]', openAdd);
    $page.on('click.lancf', '[data-filter]', function () { filterType = $(this).data('filter'); SAS.app.rerender(); });
    $page.on('input.lancs', '#lanc-search', function () {
      search = this.value;
      var pos = this.selectionStart;
      SAS.app.rerender();
      var $s = $('#lanc-search').focus();
      if ($s[0]) $s[0].setSelectionRange(pos, pos);
    });
  }

  SAS.screens = SAS.screens || {};
  SAS.screens.lancamentos = { render: render, mount: mount };
})(window, jQuery);
