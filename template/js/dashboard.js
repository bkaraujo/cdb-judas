/* dashboard.js — Visão Geral: painéis configuráveis, drag&drop, personalizar */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, D = SAS.data;
  var fmt = SAS.fmt;

  var ALL_PANELS = [
    { id:'saldos-caixa',      label:'Saldos de caixa',             defaultOn:true  },
    { id:'resultado-mes',     label:'Resultado do mês',            defaultOn:true  },
    { id:'despesas-cat',      label:'Despesas por categoria',      defaultOn:true  },
    { id:'contas-pagar',      label:'Contas a pagar',              defaultOn:true  },
    { id:'contas-receber',    label:'Contas a receber',            defaultOn:false },
    { id:'cartoes-credito',   label:'Cartões de crédito',          defaultOn:true  },
    { id:'fluxo-caixa',       label:'Fluxo de caixa',              defaultOn:true  },
    { id:'metas-despesa',     label:'Metas de despesa',            defaultOn:true  },
    { id:'ultimos-lanc',      label:'Últimos lançamentos',         defaultOn:true  },
    { id:'balanco',           label:'Balanço patrimonial',         defaultOn:false },
    { id:'despesas-centro',   label:'Despesas por centro',         defaultOn:false },
    { id:'metas-despesa-ctr', label:'Metas de despesa por centro', defaultOn:false },
    { id:'metas-economia',    label:'Metas de economia',           defaultOn:false },
    { id:'metas-invest',      label:'Metas de investimentos',      defaultOn:false },
    { id:'metas-receita',     label:'Metas de receita',            defaultOn:false },
    { id:'receitas-cat',      label:'Receitas por categoria',      defaultOn:false },
    { id:'receitas-centro',   label:'Receitas por centro',         defaultOn:false },
    { id:'resultados-caixa',  label:'Resultados de caixa',         defaultOn:false }
  ];

  function defaults() {
    var enabled = {};
    ALL_PANELS.forEach(function (p) { enabled[p.id] = p.defaultOn; });
    return {
      viewMode: 'caixa', columns: 2, scrollPanels: false, includeInvestments: true,
      enabled: enabled, panelOrder: ALL_PANELS.map(function (p) { return p.id; })
    };
  }
  function loadSettings() {
    try {
      var s = localStorage.getItem('sas-dashboard');
      return s ? $.extend(true, defaults(), JSON.parse(s)) : defaults();
    } catch (e) { return defaults(); }
  }
  function saveSettings(s) { localStorage.setItem('sas-dashboard', JSON.stringify(s)); }

  var settings = loadSettings();

  // ── Mini gráficos ───────────────────────────────────────────
  function categoryBars(data) {
    var max = Math.max.apply(null, data.map(function (d) { return d.amount; }));
    return data.map(function (d) {
      return '<div style="margin-bottom:8px">' +
        '<div class="flex justify-between" style="margin-bottom:4px">' +
        '<span style="font-size:12px;color:var(--text-secondary)">' + d.name + '</span>' +
        '<span style="font-size:12px;font-weight:700;color:' + d.color + '">' + fmt(d.amount) + '</span>' +
        '</div><div class="pbar sm"><div style="width:' + (d.amount / max * 100) + '%;background:' + d.color + '"></div></div></div>';
    }).join('');
  }

  // ── Cabeçalho do painel ─────────────────────────────────────
  function panelHead(title, iconName, action) {
    return '<div class="panel__head">' +
      '<div class="panel__title">' +
      '<span class="grip"><svg width="10" height="14" viewBox="0 0 10 14" fill="currentColor">' +
      '<circle cx="2" cy="2" r="1.3"/><circle cx="2" cy="6" r="1.3"/><circle cx="2" cy="11" r="1.3"/>' +
      '<circle cx="6" cy="2" r="1.3"/><circle cx="6" cy="6" r="1.3"/><circle cx="6" cy="11" r="1.3"/></svg></span>' +
      ui.icon(iconName, { size: 14, color: 'var(--text-muted)' }) +
      '<span>' + title + '</span></div>' +
      (action || '') + '</div>';
  }
  function panel(title, iconName, bodyHtml, action) {
    return '<div class="panel">' + panelHead(title, iconName, action) +
      '<div class="panel__body">' + bodyHtml + '</div></div>';
  }

  // ── Renderizadores de painel ────────────────────────────────
  var PANELS = {
    'saldos-caixa': function () {
      var total = D.ACCOUNTS.reduce(function (s, a) { return s + a.balance; }, 0);
      var body = '<div style="margin-bottom:12px"><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Total</p>' +
        '<p style="font-size:24px;font-weight:800;margin-top:2px">' + fmt(total) + '</p></div>';
      body += D.ACCOUNTS.map(function (a) {
        return '<div class="flex items-center justify-between" style="padding:8px 0;border-top:1px solid var(--border-light)">' +
          '<div class="flex items-center gap-8"><span style="width:8px;height:8px;border-radius:50%;background:' + a.color + '"></span>' +
          '<span style="font-size:12px;color:var(--text-secondary)">' + a.name + '</span></div>' +
          '<span style="font-size:13px;font-weight:700">' + fmt(a.balance) + '</span></div>';
      }).join('');
      return panel('Saldos de Caixa', 'building', body);
    },
    'resultado-mes': function () {
      var inc = 8500, exp = 3900, res = inc - exp;
      var rows = [
        { label: 'Receitas', value: inc, color: 'var(--income)' },
        { label: 'Despesas', value: exp, color: 'var(--expense)' },
        { label: 'Resultado', value: res, color: res >= 0 ? 'var(--income)' : 'var(--expense)', bold: true }
      ];
      var body = '<div style="display:flex;flex-direction:column;gap:10px">';
      body += rows.map(function (r) {
        return '<div class="flex items-center justify-between" style="padding:8px 0;border-bottom:1px solid var(--border-light)">' +
          '<span style="font-size:13px;color:var(--text-secondary)' + (r.bold ? ';font-weight:700' : '') + '">' + r.label + '</span>' +
          '<span style="font-size:14px;font-weight:700;color:' + r.color + '">' + fmt(r.value) + '</span></div>';
      }).join('');
      body += '<div style="height:80px;margin-top:4px">' + ui.lineChart(D.MONTHLY_DATA, { w: 400, h: 100 }) + '</div></div>';
      return panel('Resultado do Mês', 'activity', body);
    },
    'despesas-cat': function () {
      return panel('Despesas por Categoria', 'pieChart', categoryBars(D.EXPENSE_CATS.slice(0, 5)));
    },
    'contas-pagar': function () {
      var bills = D.UPCOMING.filter(function (u) { return u.type === 'expense'; }).slice(0, 5);
      var body = bills.map(function (b) {
        return '<div class="flex items-center justify-between" style="padding:8px 0;border-bottom:1px solid var(--border-light)">' +
          '<div><p style="font-size:12px;font-weight:600">' + b.name + '</p>' +
          '<p style="font-size:11px;color:var(--text-muted)">' + SAS.fmtDate(b.due) + '</p></div>' +
          '<span style="font-size:13px;font-weight:700;color:var(--expense)">-' + fmt(b.amount) + '</span></div>';
      }).join('');
      return panel('Contas a Pagar', 'calendar', body, ui.badge(bills.length, 'expense'));
    },
    'contas-receber': function () {
      var body = D.CONTAS_RECEBER.map(function (r) {
        return '<div class="flex items-center justify-between" style="padding:8px 0;border-bottom:1px solid var(--border-light)">' +
          '<div><p style="font-size:12px;font-weight:600">' + r.name + '</p>' +
          '<p style="font-size:11px;color:var(--text-muted)">' + SAS.fmtDate(r.due) + '</p></div>' +
          '<span style="font-size:13px;font-weight:700;color:var(--income)">+' + fmt(r.amount) + '</span></div>';
      }).join('');
      return panel('Contas a Receber', 'arrowUp', body, ui.badge(D.CONTAS_RECEBER.length, 'income'));
    },
    'cartoes-credito': function () {
      var body = D.DASH_CARDS.map(function (c) {
        var pct = c.used / c.limit * 100;
        var bar = pct > 80 ? 'var(--expense)' : pct > 60 ? 'var(--warning)' : 'var(--accent)';
        return '<div style="margin-bottom:12px">' +
          '<div class="flex justify-between" style="margin-bottom:6px">' +
          '<div class="flex items-center" style="gap:6px"><span style="width:10px;height:10px;border-radius:2px;background:' + c.color + '"></span>' +
          '<span style="font-size:12px;font-weight:600">' + c.name + '</span></div>' +
          '<span style="font-size:12px;color:var(--expense);font-weight:700">' + fmt(c.used) + '</span></div>' +
          '<div class="pbar sm" style="margin-bottom:4px"><div style="width:' + pct + '%;background:' + bar + '"></div></div>' +
          '<div class="flex justify-between" style="font-size:11px;color:var(--text-muted)"><span>' + pct.toFixed(0) + '% usado</span><span>Limite: ' + fmt(c.limit) + '</span></div></div>';
      }).join('');
      return panel('Cartões de Crédito', 'creditCard', body);
    },
    'fluxo-caixa': function () {
      var body = '<div class="flex" style="gap:16px;margin-bottom:10px;font-size:12px">' +
        '<span class="flex items-center" style="gap:4px;color:var(--text-secondary)"><span style="width:14px;height:3px;border-radius:2px;background:var(--income)"></span>Receitas</span>' +
        '<span class="flex items-center" style="gap:4px;color:var(--text-secondary)"><span style="width:14px;height:3px;border-radius:2px;background:var(--expense)"></span>Despesas</span></div>' +
        '<div style="height:110px">' + ui.lineChart(D.MONTHLY_DATA, { w: 400, h: 110 }) + '</div>';
      return panel('Fluxo de Caixa', 'trendingUp', body);
    },
    'metas-despesa': function () {
      var body = '<div style="display:flex;flex-direction:column;gap:10px">';
      body += D.BUDGET_GOALS.map(function (g) {
        var pct = Math.min(g.spent / g.budgeted * 100, 100), over = g.spent > g.budgeted;
        var bar = over ? 'var(--expense)' : pct > 80 ? 'var(--warning)' : 'var(--income)';
        return '<div><div class="flex justify-between" style="margin-bottom:4px;font-size:12px">' +
          '<span style="color:var(--text-secondary)">' + g.name + '</span>' +
          '<span style="font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + '">' + fmt(g.spent) + ' / ' + fmt(g.budgeted) + '</span></div>' +
          '<div class="pbar sm"><div style="width:' + pct + '%;background:' + bar + '"></div></div></div>';
      }).join('');
      body += '</div>';
      return panel('Metas de Despesa', 'target', body);
    },
    'ultimos-lanc': function () {
      var body = D.RECENT.map(function (tx) {
        return '<div class="flex items-center" style="gap:10px;padding:7px 0;border-bottom:1px solid var(--border-light)">' +
          '<span style="font-size:16px;flex-shrink:0">' + tx.icon + '</span>' +
          '<div style="flex:1;min-width:0"><p style="font-size:12px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + tx.desc + '</p>' +
          '<p style="font-size:11px;color:var(--text-muted)">' + tx.cat + '</p></div>' +
          '<span style="font-size:12px;font-weight:700;flex-shrink:0;color:' + (tx.amount > 0 ? 'var(--income)' : 'var(--expense)') + '">' + (tx.amount > 0 ? '+' : '') + fmt(tx.amount) + '</span></div>';
      }).join('');
      return panel('Últimos Lançamentos', 'list', body, ui.btn('Ver todos', { variant: 'ghost', size: 'sm', attrs: 'data-nav-to="lancamentos"' }));
    },
    'balanco': function () {
      var rows = [
        { label: 'Ativo Total', value: 41800, color: 'var(--income)' },
        { label: 'Passivo Total', value: 4235.7, color: 'var(--expense)' },
        { label: 'Patrimônio Líq.', value: 37564.3, color: 'var(--accent)' }
      ];
      var body = rows.map(function (r) {
        return '<div class="flex justify-between" style="padding:8px 0;border-bottom:1px solid var(--border-light)">' +
          '<span style="font-size:13px;color:var(--text-secondary)">' + r.label + '</span>' +
          '<span style="font-size:14px;font-weight:700;color:' + r.color + '">' + fmt(r.value) + '</span></div>';
      }).join('');
      return panel('Balanço Patrimonial', 'database', body);
    }
  };

  function stubPanel(def) {
    return panel(def.label, 'barChart', ui.emptyState({ icon: 'activity', title: 'Em breve', desc: 'Dados disponíveis em breve.' }));
  }

  function renderPanel(id) {
    if (PANELS[id]) return PANELS[id]();
    var def = ALL_PANELS.find(function (p) { return p.id === id; });
    return stubPanel(def || { label: id });
  }

  // ── Tela ────────────────────────────────────────────────────
  function render() {
    var order = settings.panelOrder && settings.panelOrder.length ? settings.panelOrder : ALL_PANELS.map(function (p) { return p.id; });
    var enabled = order.map(function (id) { return ALL_PANELS.find(function (p) { return p.id === id; }); })
      .filter(function (p) { return p && settings.enabled[p.id]; });
    var cols = settings.columns || 2;

    var h = '';
    h += '<div class="page-header"><h1>Visão Geral</h1><div class="page-header-actions">' +
      '<button class="icon-btn" data-act="hide" title="Ocultar valores" style="width:34px;height:34px">' + ui.icon('eye', { size: 16 }) + '</button>' +
      ui.btn('Personalizar', { variant: 'secondary', size: 'sm', icon: 'settings', attrs: 'data-act="customize"' }) +
      ui.btn('Novo Lançamento', { size: 'sm', icon: 'plus', attrs: 'data-act="novo"' }) +
      '</div></div>';

    if (enabled.length === 0) {
      h += ui.emptyState({ icon: 'settings', title: 'Nenhum painel habilitado', desc: 'Clique em "Personalizar" para adicionar painéis.' });
    } else {
      h += '<div class="grid grid-' + cols + '" id="panel-grid">';
      enabled.forEach(function (p) {
        h += '<div data-panel-id="' + p.id + '">' + renderPanel(p.id) + '</div>';
      });
      h += '</div>';
    }
    return h;
  }

  // ── Modal de personalização ─────────────────────────────────
  function radioRow(field, options, current) {
    return '<div class="flex" style="gap:16px;flex-wrap:wrap;margin-top:6px">' +
      options.map(function (o) {
        var checked = String(current) === String(o.value) ? 'checked' : '';
        return '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px">' +
          '<input type="radio" name="' + field + '" value="' + o.value + '" ' + checked + ' style="accent-color:var(--accent);cursor:pointer;width:auto"> ' + o.label + '</label>';
      }).join('') + '</div>';
  }

  function openCustomize() {
    var local = $.extend(true, {}, settings);
    var body = '<div style="display:flex;flex-direction:column;gap:18px">';
    body += '<div><p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px">Exibir painéis de receitas e despesas na visão de</p>' +
      radioRow('viewMode', [{ value: 'caixa', label: 'Caixa' }, { value: 'competencia', label: 'Competência' }, { value: 'definir', label: 'Definir em cada painel' }], local.viewMode) + '</div>';
    body += '<div style="border-top:1px solid var(--border);padding-top:16px"><p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px">Layout de exibição</p>' +
      radioRow('columns', [{ value: 1, label: '1 coluna' }, { value: 2, label: '2 colunas' }, { value: 3, label: '3 colunas' }], local.columns) + '</div>';
    body += '<div style="border-top:1px solid var(--border);padding-top:16px"><p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px">Utilizar barra de rolagem nos painéis</p>' +
      radioRow('scrollPanels', [{ value: true, label: 'Sim' }, { value: false, label: 'Não' }], local.scrollPanels) + '</div>';
    body += '<div style="border-top:1px solid var(--border);padding-top:16px"><p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px">Considerar despesas e receitas vinculadas ao módulo de investimentos</p>' +
      radioRow('includeInvestments', [{ value: true, label: 'Sim' }, { value: false, label: 'Não' }], local.includeInvestments) + '</div>';
    body += '<div style="border-top:1px solid var(--border);padding-top:16px"><p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:10px">Habilitar e desabilitar painéis</p>' +
      '<div style="display:flex;flex-direction:column;gap:6px">';
    ALL_PANELS.forEach(function (p) {
      var checked = local.enabled[p.id] ? 'checked' : '';
      body += '<label class="flex items-center" style="gap:10px;padding:7px 10px;border-radius:var(--radius-sm);cursor:pointer" data-panel-toggle>' +
        '<input type="checkbox" data-pid="' + p.id + '" ' + checked + ' style="width:15px;height:15px;accent-color:var(--accent);cursor:pointer;flex-shrink:0">' +
        '<span style="font-size:13px">' + p.label + '</span></label>';
    });
    body += '</div></div></div>';

    var footer = ui.btn('Cancelar', { variant: 'secondary', attrs: 'data-modal-close' }) + ui.btn('Salvar', { attrs: 'data-save' });

    ui.openModal({
      title: 'Personalizar visão geral', body: body, footer: footer,
      onMount: function ($m) {
        $m.on('change', 'input[name="viewMode"]', function () { local.viewMode = this.value; });
        $m.on('change', 'input[name="columns"]', function () { local.columns = parseInt(this.value, 10); });
        $m.on('change', 'input[name="scrollPanels"]', function () { local.scrollPanels = this.value === 'true'; });
        $m.on('change', 'input[name="includeInvestments"]', function () { local.includeInvestments = this.value === 'true'; });
        $m.on('change', 'input[data-pid]', function () { local.enabled[$(this).data('pid')] = this.checked; });
        $m.on('mouseenter', '[data-panel-toggle]', function () { $(this).css('background', 'var(--bg-hover)'); });
        $m.on('mouseleave', '[data-panel-toggle]', function () { $(this).css('background', 'transparent'); });
        $m.on('click', '[data-save]', function () {
          settings = local; saveSettings(settings); ui.closeModal(); SAS.app.rerender();
        });
      }
    });
  }

  // ── Drag & drop (SortableJS) ────────────────────────────────
  function mount($page) {
    var grid = document.getElementById('panel-grid');
    if (grid && global.Sortable) {
      Sortable.create(grid, {
        animation: 160,
        handle: '.panel__head',
        ghostClass: 'panel-ghost',
        dragClass: 'panel-dragging',
        onEnd: function () {
          var ids = $(grid).children('[data-panel-id]').map(function () { return $(this).data('panel-id'); }).get();
          // Mantém painéis desabilitados na ordem, reordenando apenas os visíveis
          var disabled = settings.panelOrder.filter(function (id) { return !settings.enabled[id]; });
          settings.panelOrder = ids.concat(disabled.filter(function (id) { return ids.indexOf(id) === -1; }));
          saveSettings(settings);
        }
      });
    }

    $page.off('click.dash').on('click.dash', '[data-act]', function () {
      var act = $(this).data('act');
      if (act === 'customize') openCustomize();
      else if (act === 'novo') SAS.app.navigate('lancamentos');
      else if (act === 'hide') {
        var $i = $(this);
        var hidden = $i.attr('data-hidden') === '1';
        $i.attr('data-hidden', hidden ? '0' : '1').attr('title', hidden ? 'Ocultar valores' : 'Mostrar valores');
        $i.html(ui.icon(hidden ? 'eye' : 'eyeOff', { size: 16 }));
        $('#panel-grid').css('filter', hidden ? 'none' : 'blur(7px)');
      }
    });
    $page.on('click.dashnav', '[data-nav-to]', function () { SAS.app.navigate($(this).data('nav-to')); });
  }

  SAS.screens = SAS.screens || {};
  SAS.screens.dashboard = { render: render, mount: mount };
})(window, jQuery);
