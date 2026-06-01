/* relatorios.js — Grade de relatórios + Evolução por Categoria (hierarquia + olho) */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, D = SAS.data, fmt = SAS.fmt;
  var H = D.YEAR_HIERARCHY, MONTHS = D.MONTHS_LABELS, TODAY = D.TODAY_MONTH;
  var REAL_MONTHS = MONTHS.slice(0, TODAY + 1);

  var state = {
    report: null,
    activeCats: H.reduce(function (acc, m) { return acc.concat(m.categories.map(function (c) { return c.id; })); }, []),
    openMacros: { despesas: true, receitas: false },
    openCats: {}
  };

  // ── Cálculos ────────────────────────────────────────────────
  function catMonthData(cat) {
    var out = [];
    for (var m = 0; m < 12; m++) {
      var vals = cat.subcats.map(function (s) { return s.data[m]; }).filter(function (v) { return v !== null; });
      out.push(vals.length ? vals.reduce(function (a, b) { return a + b; }, 0) : null);
    }
    return out;
  }
  function macroMonthData(macro) {
    var out = [];
    for (var m = 0; m < 12; m++) {
      var vals = macro.categories.map(function (c) { return catMonthData(c)[m]; }).filter(function (v) { return v !== null; });
      out.push(vals.length ? vals.reduce(function (a, b) { return a + b; }, 0) : null);
    }
    return out;
  }
  function sumData(d) { return d.filter(function (v) { return v !== null; }).reduce(function (a, b) { return a + b; }, 0); }
  function avgData(d) { var v = d.filter(function (x) { return x !== null; }); return v.length ? v.reduce(function (a, b) { return a + b; }, 0) / v.length : 0; }

  // ── Gráfico ─────────────────────────────────────────────────
  function chart() {
    var W = 700, He = 240, pad = { l: 60, r: 24, t: 20, b: 36 };
    var cw = W - pad.l - pad.r, ch = He - pad.t - pad.b, n = MONTHS.length;
    var series = [];
    H.forEach(function (macro) {
      macro.categories.forEach(function (cat) {
        if (state.activeCats.indexOf(cat.id) >= 0) series.push({ id: cat.id, color: cat.color, data: catMonthData(cat) });
      });
    });
    var allVals = [];
    series.forEach(function (s) { s.data.forEach(function (v) { if (v !== null) allVals.push(v); }); });
    if (!allVals.length) return '<svg viewBox="0 0 ' + W + ' ' + He + '" style="width:100%;height:100%"></svg>';
    var maxV = Math.max.apply(null, allVals) * 1.12;
    var toX = function (i) { return pad.l + (i / (n - 1)) * cw; };
    var toY = function (v) { return pad.t + (1 - v / maxV) * ch; };

    var s = '<svg viewBox="0 0 ' + W + ' ' + He + '" style="width:100%;height:100%">';
    [0, 0.33, 0.66, 1].forEach(function (f) {
      var y = pad.t + (1 - f) * ch;
      s += '<line x1="' + pad.l + '" y1="' + y + '" x2="' + (W - pad.r) + '" y2="' + y + '" stroke="var(--border)" stroke-width="1"/>';
      s += '<text x="' + (pad.l - 6) + '" y="' + (y + 4) + '" text-anchor="end" font-size="9" fill="var(--text-muted)">' + SAS.fmtShort(f * maxV) + '</text>';
    });
    var projX = toX(TODAY) + (toX(1) - toX(0)) / 2;
    s += '<rect x="' + projX + '" y="' + pad.t + '" width="' + (W - pad.r - projX) + '" height="' + ch + '" fill="var(--bg-hover)" opacity="0.5"/>';
    s += '<text x="' + toX(TODAY + 1) + '" y="' + (pad.t + 12) + '" font-size="9" fill="var(--text-muted)" text-anchor="middle">Projeção →</text>';
    MONTHS.forEach(function (l, i) {
      s += '<text x="' + toX(i) + '" y="' + (He - 8) + '" text-anchor="middle" font-size="10" fill="' + (i <= TODAY ? 'var(--text-secondary)' : 'var(--text-muted)') + '">' + l + '</text>';
    });
    series.forEach(function (ser) {
      var pts = [];
      ser.data.forEach(function (v, i) { if (v !== null && i <= TODAY) pts.push({ x: toX(i), y: toY(v) }); });
      if (pts.length > 1) s += '<path d="' + ui.smoothPath(pts) + '" fill="none" stroke="' + ser.color + '" stroke-width="2.5" stroke-linecap="round"/>';
      pts.forEach(function (p) { s += '<circle cx="' + p.x + '" cy="' + p.y + '" r="3.5" fill="' + ser.color + '" stroke="var(--bg-card)" stroke-width="1.5"/>'; });
    });
    s += '</svg>';
    return s;
  }

  // ── Tabela hierárquica ──────────────────────────────────────
  function cell(v, opts) {
    opts = opts || {};
    var content = (v !== null && v !== undefined && v !== 0) ? fmt(v) : '—';
    var style = 'padding:10px 14px;text-align:right;font-size:13px;white-space:nowrap;';
    style += 'font-weight:' + (opts.bold ? '700' : '400') + ';';
    style += 'color:' + (opts.color || (opts.muted ? 'var(--text-muted)' : 'var(--text-primary)')) + ';';
    return '<td style="' + style + '">' + content + '</td>';
  }

  function table() {
    var h = '<div style="overflow-x:auto"><table style="width:100%;border-collapse:collapse;min-width:700px"><thead><tr style="border-bottom:2px solid var(--border)">';
    h += '<th style="padding:10px 16px;text-align:left;font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.05em;min-width:200px">Categoria</th>';
    REAL_MONTHS.forEach(function (m) { h += '<th style="padding:10px 14px;text-align:right;font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase">' + m + '</th>'; });
    h += '<th style="padding:10px 14px;text-align:right;font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase">Total</th>';
    h += '<th style="padding:10px 14px;text-align:right;font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase">Média/mês</th>';
    h += '</tr></thead><tbody>';

    H.forEach(function (macro) {
      var md = macroMonthData(macro);
      var mOpen = state.openMacros[macro.id];
      var mColor = macro.id === 'despesas' ? 'var(--expense)' : 'var(--income)';
      var mLight = macro.id === 'despesas' ? 'var(--expense-light)' : 'var(--income-light)';
      // Macro row
      h += '<tr data-macro="' + macro.id + '" style="background:var(--bg-hover);cursor:pointer;border-top:2px solid var(--border)">';
      h += '<td style="padding:12px 16px"><div class="flex items-center gap-8">' +
        '<div style="width:18px;height:18px;border-radius:4px;background:' + mLight + ';display:flex;align-items:center;justify-content:center">' +
        ui.icon(mOpen ? 'chevronDown' : 'chevronRight', { size: 11, color: mColor }) + '</div>' +
        '<span style="font-size:14px;font-weight:800;color:' + mColor + '">' + macro.label + '</span></div></td>';
      md.slice(0, TODAY + 1).forEach(function (v) { h += cell(v, { bold: true, color: mColor }); });
      h += cell(sumData(md), { bold: true, color: mColor });
      h += cell(avgData(md.slice(0, TODAY + 1)), { bold: true, color: mColor });
      h += '</tr>';

      if (mOpen) {
        macro.categories.forEach(function (cat) {
          var cd = catMonthData(cat);
          var cOpen = state.openCats[cat.id];
          var active = state.activeCats.indexOf(cat.id) >= 0;
          h += '<tr data-cat="' + cat.id + '" style="cursor:pointer;border-bottom:1px solid var(--border-light)">';
          h += '<td style="padding:10px 16px 10px 36px"><div class="flex items-center gap-8">' +
            '<div style="width:16px;height:16px;border-radius:3px;background:' + cat.color + '22;display:flex;align-items:center;justify-content:center">' +
            ui.icon(cOpen ? 'chevronDown' : 'chevronRight', { size: 10, color: cat.color }) + '</div>' +
            '<span style="width:8px;height:8px;border-radius:50%;background:' + cat.color + ';flex-shrink:0"></span>' +
            '<span style="font-size:13px;font-weight:600">' + cat.label + '</span>' +
            ui.badge(cat.subcats.length, 'muted') +
            '<button data-eye="' + cat.id + '" title="' + (active ? 'Ocultar no gráfico' : 'Exibir no gráfico') + '" style="margin-left:auto;background:none;border:none;cursor:pointer;padding:2px 4px;display:flex;align-items:center;color:' + (active ? cat.color : 'var(--text-muted)') + ';opacity:' + (active ? '1' : '0.45') + '">' +
            ui.icon(active ? 'eye' : 'eyeOff', { size: 14 }) + '</button>' +
            '</div></td>';
          cd.slice(0, TODAY + 1).forEach(function (v) { h += cell(v); });
          h += cell(sumData(cd), { bold: true });
          h += cell(avgData(cd.slice(0, TODAY + 1)), { muted: true });
          h += '</tr>';

          if (cOpen) {
            cat.subcats.forEach(function (sub) {
              h += '<tr style="border-bottom:1px solid var(--border-light)">';
              h += '<td style="padding:8px 16px 8px 60px"><span class="sec flex items-center" style="font-size:12px;gap:6px"><span class="muted" style="font-size:11px">↳</span>' + sub.label + '</span></td>';
              sub.data.slice(0, TODAY + 1).forEach(function (v) { h += cell(v, { muted: true }); });
              h += cell(sumData(sub.data));
              h += cell(avgData(sub.data.slice(0, TODAY + 1)), { muted: true });
              h += '</tr>';
            });
          }
        });
      }
    });
    h += '</tbody></table></div>';
    return h;
  }

  // ── Render ──────────────────────────────────────────────────
  var COLOR_MAP = { accent: 'var(--accent)', income: 'var(--income)', expense: 'var(--expense)', warning: 'var(--warning)', info: 'var(--info)' };

  function render() {
    if (state.report === 'cat-evo') {
      var h = '';
      h += '<div class="page-header"><div class="flex items-center" style="gap:12px">' +
        '<button class="icon-btn" data-act="back">' + ui.icon('chevronLeft', { size: 18 }) + '</button>' +
        '<h1>Evolução por Categoria — 2026</h1></div>' +
        '<div class="page-header-actions">' + ui.btn('Exportar', { variant: 'secondary', size: 'sm', icon: 'download' }) + '</div></div>';
      h += '<div class="card" style="margin-bottom:16px"><div style="height:240px">' + chart() + '</div></div>';
      h += '<div class="card flush"><div class="flex items-center justify-between" style="padding:14px 16px 12px;border-bottom:1px solid var(--border)">' +
        '<h3 style="font-size:14px;font-weight:700">Detalhamento por Categoria</h3>' +
        '<div class="flex" style="gap:8px">' +
        '<button data-expand-all style="font-size:12px;color:var(--accent);background:none;border:none;cursor:pointer;font-weight:600">Expandir tudo</button>' +
        '<button data-collapse-all style="font-size:12px;color:var(--text-muted);background:none;border:none;cursor:pointer;font-weight:600">Recolher tudo</button>' +
        '</div></div>' + table() + '</div>';
      return h;
    }

    var g = '<div class="page-header"><h1>Relatórios</h1>' +
      '<div style="position:relative"><div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted)">' + ui.icon('search', { size: 15 }) + '</div>' +
      '<input placeholder="Pesquisar relatório..." style="padding-left:32px;width:220px"></div></div>';
    g += '<div class="grid grid-4">';
    D.REPORT_CARDS.forEach(function (r) {
      g += '<div class="report-card" data-report="' + r.id + '" style="background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;cursor:pointer;display:flex;flex-direction:column;gap:10px" data-color="' + COLOR_MAP[r.color] + '">' +
        '<div style="width:40px;height:40px;border-radius:10px;background:' + COLOR_MAP[r.color] + '18;display:flex;align-items:center;justify-content:center">' + ui.icon(r.icon, { size: 20, color: COLOR_MAP[r.color] }) + '</div>' +
        '<div><p style="font-size:13px;font-weight:700;margin-bottom:4px">' + r.title + '</p><p class="muted" style="font-size:12px;line-height:1.4">' + r.desc + '</p></div>' +
        (r.id === 'cat-evo' ? ui.badge('✦ Destaque', 'income') : '') + '</div>';
    });
    g += '</div>';
    return g;
  }

  function mount($page) {
    $page.off('.rel');
    // Grade
    $page.on('click.rel', '[data-report]', function () {
      if ($(this).data('report') === 'cat-evo') { state.report = 'cat-evo'; SAS.app.rerender(); }
    });
    $page.on('mouseenter.rel', '.report-card', function () {
      var c = $(this).data('color');
      $(this).css({ borderColor: c, transform: 'translateY(-2px)', boxShadow: '0 8px 24px rgba(0,0,0,0.15)' });
    }).on('mouseleave.rel', '.report-card', function () {
      $(this).css({ borderColor: 'var(--border)', transform: 'none', boxShadow: 'none' });
    });
    // Detalhe
    $page.on('click.rel', '[data-act="back"]', function () { state.report = null; SAS.app.rerender(); });
    $page.on('click.rel', '[data-macro]', function (e) {
      if ($(e.target).closest('[data-eye]').length) return;
      var id = $(this).data('macro'); state.openMacros[id] = !state.openMacros[id]; SAS.app.rerender();
    });
    $page.on('click.rel', '[data-cat]', function (e) {
      if ($(e.target).closest('[data-eye]').length) return;
      var id = $(this).data('cat'); state.openCats[id] = !state.openCats[id]; SAS.app.rerender();
    });
    $page.on('click.rel', '[data-eye]', function (e) {
      e.stopPropagation();
      var id = $(this).data('eye');
      var i = state.activeCats.indexOf(id);
      if (i >= 0) state.activeCats.splice(i, 1); else state.activeCats.push(id);
      SAS.app.rerender();
    });
    $page.on('click.rel', '[data-expand-all]', function () { state.openMacros = { despesas: true, receitas: true }; SAS.app.rerender(); });
    $page.on('click.rel', '[data-collapse-all]', function () { state.openMacros = { despesas: false, receitas: false }; state.openCats = {}; SAS.app.rerender(); });
  }

  SAS.screens = SAS.screens || {};
  SAS.screens.relatorios = { render: render, mount: mount };
})(window, jQuery);
