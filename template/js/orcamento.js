/* orcamento.js — Metas/Orçamento: barras de progresso + Burn Down chart */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, D = SAS.data, fmt = SAS.fmt;
  var CATS = D.BUDGET_CATS;

  var state = { tab: 'overview', selectedId: CATS[0].id };

  // ── Gera dados burn down ────────────────────────────────────
  function genBurnDown(budgeted, spent, days, today) {
    days = days || 30; today = today || 24;
    var ideal = [];
    for (var i = 0; i <= days; i++) ideal.push({ day: i, value: budgeted - (budgeted / days) * i });
    var rates = [0,0.06,0.02,0.10,0.03,0.00,0.08,0.04,0.00,0.07,0.05,0.00,0.06,0.03,0.09,0.04,0.00,0.05,0.07,0.00,0.06,0.04,0.05,0.06,0.08];
    var totalRate = 0;
    for (var d = 0; d <= today; d++) totalRate += (rates[d] || 0);
    var factor = spent / (budgeted * totalRate || 1);
    var cum = budgeted;
    var actual = [{ day: 0, value: budgeted }];
    for (var dd = 1; dd <= today; dd++) {
      cum -= budgeted * (rates[dd] || 0.04) * factor;
      actual.push({ day: dd, value: Math.max(0, cum) });
    }
    return { ideal: ideal, actual: actual };
  }

  function burnDownChart(cat) {
    var bd = genBurnDown(cat.budgeted, cat.spent);
    var ideal = bd.ideal, actual = bd.actual;
    var today = actual.length - 1;
    var W = 560, H = 220, pad = { l: 60, r: 20, t: 16, b: 36 };
    var cw = W - pad.l - pad.r, ch = H - pad.t - pad.b;
    var days = ideal.length - 1, maxV = cat.budgeted;
    var toX = function (d) { return pad.l + (d / days) * cw; };
    var toY = function (v) { return pad.t + (1 - v / maxV) * ch; };
    var idealPts = ideal.map(function (p) { return { x: toX(p.day), y: toY(p.value) }; });
    var actualPts = actual.map(function (p) { return { x: toX(p.day), y: toY(p.value) }; });
    var idealPath = ui.smoothPath(idealPts), actualPath = ui.smoothPath(actualPts);
    var isOver = cat.spent > cat.budgeted;
    var fillColor = isOver ? 'var(--expense)' : 'var(--income)';
    var areaActual = actualPath + ' L ' + toX(today) + ',' + (pad.t + ch) + ' L ' + pad.l + ',' + (pad.t + ch) + ' Z';
    var todayX = toX(today), lastY = toY(actual[actual.length - 1].value);

    var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:100%">';
    s += '<defs><linearGradient id="burn-grad" x1="0" y1="0" x2="0" y2="1">' +
      '<stop offset="0%" stop-color="' + fillColor + '" stop-opacity="0.18"/>' +
      '<stop offset="100%" stop-color="' + fillColor + '" stop-opacity="0.01"/></linearGradient></defs>';
    [0, 0.25, 0.5, 0.75, 1].forEach(function (f) {
      var y = pad.t + (1 - f) * ch;
      s += '<line x1="' + pad.l + '" y1="' + y + '" x2="' + (W - pad.r) + '" y2="' + y + '" stroke="var(--border)" stroke-width="1"/>';
      s += '<text x="' + (pad.l - 6) + '" y="' + (y + 4) + '" text-anchor="end" font-size="9" fill="var(--text-muted)">' + SAS.fmtShort(f * maxV) + '</text>';
    });
    [0, 5, 10, 15, 20, 25, 30].forEach(function (d) {
      if (d <= days) s += '<text x="' + toX(d) + '" y="' + (H - 8) + '" text-anchor="middle" font-size="9" fill="var(--text-muted)">' + d + '</text>';
    });
    s += '<line x1="' + todayX + '" y1="' + pad.t + '" x2="' + todayX + '" y2="' + (pad.t + ch) + '" stroke="var(--text-muted)" stroke-width="1" stroke-dasharray="4,3"/>';
    s += '<text x="' + (todayX + 4) + '" y="' + (pad.t + 12) + '" font-size="9" fill="var(--text-muted)">Hoje</text>';
    s += '<path d="' + areaActual + '" fill="url(#burn-grad)"/>';
    s += '<path d="' + idealPath + '" fill="none" stroke="var(--text-muted)" stroke-width="1.5" stroke-dasharray="6,4"/>';
    s += '<path d="' + actualPath + '" fill="none" stroke="' + fillColor + '" stroke-width="2.5" stroke-linecap="round"/>';
    s += '<circle cx="' + todayX + '" cy="' + lastY + '" r="5" fill="' + fillColor + '" stroke="var(--bg-card)" stroke-width="2"/>';
    s += '</svg>';
    return s;
  }

  function budgetBar(cat) {
    var pct = Math.min(cat.spent / cat.budgeted * 100, 100);
    var over = cat.spent > cat.budgeted;
    var bar = over ? 'var(--expense)' : (cat.spent / cat.budgeted > 0.8 ? 'var(--warning)' : 'var(--income)');
    return '<div style="display:flex;flex-direction:column;gap:6px">' +
      '<div class="flex items-center justify-between">' +
      '<div class="flex items-center gap-8"><span style="font-size:16px">' + cat.icon + '</span>' +
      '<span style="font-size:13px;font-weight:600">' + cat.name + '</span>' +
      (over ? ui.badge('Estourado', 'expense') : '') + '</div>' +
      '<div class="flex" style="gap:12px"><span class="muted" style="font-size:12px">' + fmt(cat.spent) + '</span>' +
      '<span class="sec" style="font-size:12px;font-weight:600">/ ' + fmt(cat.budgeted) + '</span></div></div>' +
      '<div class="pbar"><div style="width:' + pct + '%;background:' + bar + '"></div></div>' +
      '<div class="flex justify-between"><span class="muted" style="font-size:11px">' + pct.toFixed(0) + '% utilizado</span>' +
      '<span style="font-size:11px;font-weight:600;color:' + (over ? 'var(--expense)' : 'var(--income)') + '">' +
      (over ? '+' + fmt(cat.spent - cat.budgeted) + ' acima' : fmt(cat.budgeted - cat.spent) + ' restante') + '</span></div></div>';
  }

  function render() {
    var totalBudgeted = CATS.reduce(function (s, c) { return s + c.budgeted; }, 0);
    var totalSpent = CATS.reduce(function (s, c) { return s + c.spent; }, 0);
    var over = CATS.filter(function (c) { return c.spent > c.budgeted; });
    var sel = CATS.find(function (c) { return c.id === state.selectedId; }) || CATS[0];

    var h = '';
    h += '<div class="page-header"><h1>Metas / Orçamento</h1><div class="page-header-actions">' +
      ui.periodNav('Abril 2026') + ui.btn('Nova Meta', { icon: 'plus' }) + '</div></div>';

    h += '<div class="grid grid-3" style="margin-bottom:20px">' +
      '<div class="card" style="padding:16px 20px"><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;margin-bottom:8px">Orçamento Total</p><p style="font-size:22px;font-weight:800">' + fmt(totalBudgeted) + '</p></div>' +
      '<div class="card" style="padding:16px 20px"><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;margin-bottom:8px">Total Gasto</p><p style="font-size:22px;font-weight:800;color:' + (totalSpent > totalBudgeted ? 'var(--expense)' : 'var(--text-primary)') + '">' + fmt(totalSpent) + '</p></div>' +
      '<div class="card" style="padding:16px 20px"><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;margin-bottom:8px">Categorias Estouradas</p>' +
      '<div class="flex items-center" style="gap:10px"><p style="font-size:22px;font-weight:800;color:' + (over.length ? 'var(--expense)' : 'var(--income)') + '">' + over.length + '</p>' +
      (over.length ? '<span class="muted" style="font-size:12px">' + over.map(function (c) { return c.name; }).join(', ') + '</span>' : '') + '</div></div></div>';

    h += '<div class="tabs">' +
      '<button class="tab ' + (state.tab === 'overview' ? 'active' : '') + '" data-tab="overview">Visão Geral</button>' +
      '<button class="tab ' + (state.tab === 'burndown' ? 'active' : '') + '" data-tab="burndown">Burn Down</button></div>';

    if (state.tab === 'overview') {
      h += '<div style="display:flex;flex-direction:column;gap:14px">';
      CATS.forEach(function (cat) {
        var selected = cat.id === state.selectedId;
        h += '<div class="card" data-cat="' + cat.id + '" style="padding:16px 20px;cursor:pointer;border-color:' + (selected ? 'var(--accent)' : 'var(--border)') + '">' + budgetBar(cat) + '</div>';
      });
      h += '</div>';
    } else {
      h += '<div class="flex" style="gap:8px;flex-wrap:wrap;margin-bottom:16px">';
      CATS.forEach(function (cat) {
        var selected = cat.id === state.selectedId;
        var style = selected
          ? 'border:1px solid ' + cat.color + ';background:' + cat.color + '22;color:' + cat.color
          : 'border:1px solid var(--border);background:transparent;color:var(--text-secondary)';
        h += '<button data-cat-sel="' + cat.id + '" style="padding:6px 14px;border-radius:var(--radius-sm);font-size:12px;font-weight:600;cursor:pointer;' + style + '">' + cat.icon + ' ' + cat.name + '</button>';
      });
      h += '</div>';

      var proj = sel.spent * (30 / 24);
      h += '<div class="card"><div class="flex items-center justify-between" style="margin-bottom:16px">' +
        '<div><h3 style="font-size:14px;font-weight:700">Burn Down — ' + sel.name + '</h3>' +
        '<p class="muted" style="font-size:12px;margin-top:3px">Evolução do saldo orçado ao longo do mês</p></div>' +
        '<div class="flex" style="gap:16px;font-size:12px">' +
        '<span class="sec flex items-center" style="gap:5px"><span style="width:20px;height:0;border-top:2px dashed var(--text-muted);display:inline-block"></span>Ideal</span>' +
        '<span class="sec flex items-center" style="gap:5px"><span style="width:20px;height:3px;border-radius:2px;background:' + (sel.spent > sel.budgeted ? 'var(--expense)' : 'var(--income)') + ';display:inline-block"></span>Realizado</span>' +
        '</div></div>' +
        '<div style="height:220px">' + burnDownChart(sel) + '</div>' +
        '<div class="flex justify-between" style="margin-top:14px;padding-top:12px;border-top:1px solid var(--border)">' +
        '<span class="sec" style="font-size:13px">Orçado: <strong>' + fmt(sel.budgeted) + '</strong></span>' +
        '<span class="sec" style="font-size:13px">Gasto até hoje: <strong style="color:' + (sel.spent > sel.budgeted ? 'var(--expense)' : 'var(--income)') + '">' + fmt(sel.spent) + '</strong></span>' +
        '<span class="sec" style="font-size:13px">Restante: <strong style="color:var(--accent)">' + fmt(Math.max(0, sel.budgeted - sel.spent)) + '</strong></span>' +
        '<span class="sec" style="font-size:13px">Projeção final: <strong style="color:' + (proj > sel.budgeted ? 'var(--expense)' : 'var(--income)') + '">' + fmt(proj) + '</strong></span>' +
        '</div></div>';
    }
    return h;
  }

  function mount($page) {
    $page.off('click.orc').on('click.orc', '[data-tab]', function () { state.tab = $(this).data('tab'); SAS.app.rerender(); });
    $page.on('click.orcc', '[data-cat]', function () { state.selectedId = $(this).data('cat'); SAS.app.rerender(); });
    $page.on('click.orcs', '[data-cat-sel]', function () { state.selectedId = $(this).data('cat-sel'); SAS.app.rerender(); });
  }

  SAS.screens = SAS.screens || {};
  SAS.screens.orcamento = { render: render, mount: mount };
})(window, jQuery);
