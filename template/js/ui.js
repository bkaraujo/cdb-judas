/* ui.js — Helpers de renderização compartilhados (HTML5 + jQuery) */
(function (global, $) {
  'use strict';
  var SAS = global.SAS;

  // ── Escapar HTML ────────────────────────────────────────────
  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // ── Ícone SVG ───────────────────────────────────────────────
  function icon(name, opts) {
    opts = opts || {};
    var size = opts.size || 18;
    var color = opts.color || 'currentColor';
    var sw = opts.strokeWidth || 1.8;
    var path = SAS.ICONS[name] || SAS.ICONS.alertCircle;
    var parts = path.split('M').filter(Boolean).map(function (p) { return 'M' + p; });
    var paths = parts.map(function (d) { return '<path d="' + d.trim() + '"></path>'; }).join('');
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" ' +
      'stroke="' + color + '" stroke-width="' + sw + '" stroke-linecap="round" stroke-linejoin="round" ' +
      'style="flex-shrink:0">' + paths + '</svg>';
  }

  // ── Badge ───────────────────────────────────────────────────
  function badge(text, color) {
    var cls = color && color !== 'accent' ? ' badge--' + color : '';
    return '<span class="badge' + cls + '">' + text + '</span>';
  }

  // ── Botão ───────────────────────────────────────────────────
  // opts: { variant, size, icon, attrs }
  function btn(label, opts) {
    opts = opts || {};
    var variant = opts.variant || 'primary';
    var size = opts.size ? ' ' + opts.size : '';
    var ico = opts.icon ? icon(opts.icon, { size: opts.size === 'sm' ? 13 : 15 }) : '';
    var attrs = opts.attrs || '';
    return '<button class="btn btn--' + variant + size + '" ' + attrs + '>' + ico + '<span>' + label + '</span></button>';
  }

  // ── Caminho suave (Catmull-Rom → Bézier) ────────────────────
  function smoothPath(pts) {
    if (!pts || pts.length < 2) return '';
    var d = 'M ' + pts[0].x + ',' + pts[0].y;
    for (var i = 1; i < pts.length; i++) {
      var p0 = pts[Math.max(0, i - 2)];
      var p1 = pts[i - 1];
      var p2 = pts[i];
      var p3 = pts[Math.min(pts.length - 1, i + 1)];
      var cp1x = p1.x + (p2.x - p0.x) / 6;
      var cp1y = p1.y + (p2.y - p0.y) / 6;
      var cp2x = p2.x - (p3.x - p1.x) / 6;
      var cp2y = p2.y - (p3.y - p1.y) / 6;
      d += ' C ' + cp1x + ',' + cp1y + ' ' + cp2x + ',' + cp2y + ' ' + p2.x + ',' + p2.y;
    }
    return d;
  }

  // ── Gráfico de linha (receitas vs despesas) ─────────────────
  // data: [{month, receitas, despesas}], opts: { w, h, full }
  function lineChart(data, opts) {
    opts = opts || {};
    var W = opts.w || 600, H = opts.h || 200;
    var pad = opts.full
      ? { l: 56, r: 20, t: 16, b: 36 }
      : { l: 10, r: 10, t: 8, b: 20 };
    var cw = W - pad.l - pad.r, ch = H - pad.t - pad.b;
    var allVals = [];
    data.forEach(function (d) { allVals.push(d.receitas, d.despesas); });
    var maxV = Math.max.apply(null, allVals) * 1.1;
    var toX = function (i) { return pad.l + (i / (data.length - 1)) * cw; };
    var toY = function (v) { return pad.t + (1 - v / maxV) * ch; };
    var incPts = data.map(function (d, i) { return { x: toX(i), y: toY(d.receitas) }; });
    var expPts = data.map(function (d, i) { return { x: toX(i), y: toY(d.despesas) }; });
    var incPath = smoothPath(incPts), expPath = smoothPath(expPts);
    var areaInc = incPath + ' L ' + toX(data.length - 1) + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';
    var areaExp = expPath + ' L ' + toX(data.length - 1) + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';
    var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:100%">';
    s += '<defs>' +
      '<linearGradient id="grad-inc" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--income)" stop-opacity="0.22"/><stop offset="100%" stop-color="var(--income)" stop-opacity="0.01"/></linearGradient>' +
      '<linearGradient id="grad-exp" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--expense)" stop-opacity="0.16"/><stop offset="100%" stop-color="var(--expense)" stop-opacity="0.01"/></linearGradient>' +
      '</defs>';
    if (opts.full) {
      [0, 0.25, 0.5, 0.75, 1].forEach(function (f) {
        var y = pad.t + (1 - f) * ch;
        s += '<line x1="' + pad.l + '" y1="' + y + '" x2="' + (W - pad.r) + '" y2="' + y + '" stroke="var(--border)" stroke-width="1"/>';
        s += '<text x="' + (pad.l - 6) + '" y="' + (y + 4) + '" text-anchor="end" font-size="10" fill="var(--text-muted)">' + SAS.fmtShort(f * maxV) + '</text>';
      });
    }
    data.forEach(function (d, i) {
      s += '<text x="' + toX(i) + '" y="' + (H - (opts.full ? 8 : 6)) + '" text-anchor="middle" font-size="' + (opts.full ? 11 : 9) + '" fill="var(--text-muted)">' + d.month + '</text>';
    });
    s += '<path d="' + areaInc + '" fill="url(#grad-inc)"/>';
    s += '<path d="' + areaExp + '" fill="url(#grad-exp)"/>';
    s += '<path d="' + incPath + '" fill="none" stroke="var(--income)" stroke-width="2.5" stroke-linecap="round"/>';
    s += '<path d="' + expPath + '" fill="none" stroke="var(--expense)" stroke-width="2.5" stroke-linecap="round"/>';
    incPts.forEach(function (p) { s += '<circle cx="' + p.x + '" cy="' + p.y + '" r="4" fill="var(--income)" stroke="var(--bg-card)" stroke-width="2"/>'; });
    expPts.forEach(function (p) { s += '<circle cx="' + p.x + '" cy="' + p.y + '" r="4" fill="var(--expense)" stroke="var(--bg-card)" stroke-width="2"/>'; });
    s += '</svg>';
    return s;
  }

  // ── Navegador de período ────────────────────────────────────
  function periodNav(label) {
    return '<div class="flex items-center gap-8" style="background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius-sm);padding:6px 12px">' +
      '<button class="icon-btn sm" data-period="prev">' + icon('chevronLeft', { size: 14 }) + '</button>' +
      '<span style="font-size:13px;font-weight:600;min-width:100px;text-align:center">' + label + '</span>' +
      '<button class="icon-btn sm" data-period="next">' + icon('chevronRight', { size: 14 }) + '</button>' +
    '</div>';
  }

  // ── Estado vazio ────────────────────────────────────────────
  function emptyState(o) {
    o = o || {};
    return '<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;padding:60px 20px;gap:12px;color:var(--text-muted)">' +
      '<div style="width:56px;height:56px;border-radius:50%;background:var(--bg-hover);display:flex;align-items:center;justify-content:center">' +
      icon(o.icon || 'activity', { size: 24 }) + '</div>' +
      '<div style="text-align:center">' +
      '<p style="font-weight:600;color:var(--text-secondary);margin-bottom:4px">' + esc(o.title || '') + '</p>' +
      (o.desc ? '<p style="font-size:13px">' + esc(o.desc) + '</p>' : '') +
      '</div></div>';
  }

  // ── Modal ───────────────────────────────────────────────────
  // openModal({ title, body, footer, onMount })
  function openModal(o) {
    closeModal();
    var html = '<div class="modal-overlay" id="active-modal">' +
      '<div class="modal-box">' +
      '<div class="modal-header"><h3>' + esc(o.title || '') + '</h3>' +
      '<button class="icon-btn" data-modal-close>' + icon('x', { size: 18 }) + '</button></div>' +
      '<div class="modal-content">' + (o.body || '') + '</div>' +
      (o.footer ? '<div class="modal-footer">' + o.footer + '</div>' : '') +
      '</div></div>';
    var $m = $(html).appendTo('#modal-root');
    $m.on('click', function (e) { if (e.target === this) closeModal(); });
    $m.on('click', '[data-modal-close]', closeModal);
    if (typeof o.onMount === 'function') o.onMount($m);
    return $m;
  }
  function closeModal() { $('#active-modal').remove(); }

  // ── Exporta ─────────────────────────────────────────────────
  SAS.ui = {
    esc: esc, icon: icon, badge: badge, btn: btn,
    smoothPath: smoothPath, lineChart: lineChart,
    periodNav: periodNav, emptyState: emptyState,
    openModal: openModal, closeModal: closeModal
  };
})(window, jQuery);
