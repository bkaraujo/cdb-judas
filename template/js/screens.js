/* screens.js — Contas a Pagar/Receber, Extrato, Categorias, Cartões, Contas, Centros, Tags */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, D = SAS.data, fmt = SAS.fmt;
  SAS.screens = SAS.screens || {};

  // ── Contas a Pagar e Receber ───────────────────────────────
  var cpState = { tab: 'pagar' };
  SAS.screens['contas-pagar'] = {
    render: function () {
      var list = cpState.tab === 'pagar' ? D.PAGAR : D.RECEBER;
      var totPagar = D.PAGAR.reduce(function (s, i) { return s + i.amount; }, 0);
      var totReceber = D.RECEBER.reduce(function (s, i) { return s + i.amount; }, 0);

      var h = '<div class="page-header"><h1>A Pagar e Receber</h1><div class="page-header-actions">' +
        ui.periodNav('Maio 2026') + ui.btn('Nova Conta', { icon: 'plus' }) + '</div></div>';

      h += '<div class="card" style="padding:16px 20px;margin-bottom:16px;display:flex;gap:32px">' +
        '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">A Pagar</p><p style="font-size:20px;font-weight:800;color:var(--expense);margin-top:4px">' + fmt(totPagar) + '</p></div>' +
        '<div style="width:1px;background:var(--border)"></div>' +
        '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">A Receber</p><p style="font-size:20px;font-weight:800;color:var(--income);margin-top:4px">' + fmt(totReceber) + '</p></div>' +
        '<div style="width:1px;background:var(--border)"></div>' +
        '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Resultado</p><p style="font-size:20px;font-weight:800;color:var(--income);margin-top:4px">' + fmt(totReceber - totPagar) + '</p></div></div>';

      h += '<div class="tabs">' +
        '<button class="tab ' + (cpState.tab === 'pagar' ? 'active' : '') + '" data-cptab="pagar">A pagar (' + D.PAGAR.length + ')</button>' +
        '<button class="tab ' + (cpState.tab === 'receber' ? 'active' : '') + '" data-cptab="receber">A receber (' + D.RECEBER.length + ')</button></div>';

      h += '<div class="card flush">';
      list.forEach(function (item, i) {
        var dot = item.status === 'pending' ? 'var(--expense)' : 'var(--warning)';
        h += '<div class="list-row' + (i < list.length - 1 ? ' bordered' : '') + '">' +
          '<div class="flex items-center" style="gap:14px"><div style="width:10px;height:10px;border-radius:50%;background:' + dot + ';flex-shrink:0"></div>' +
          '<div><p style="font-size:13px;font-weight:600">' + item.name + '</p><p class="muted" style="font-size:12px;margin-top:2px">' + item.cat + '</p></div></div>' +
          '<div class="flex items-center" style="gap:24px">' +
          '<span class="muted" style="font-size:12px">' + SAS.fmtDate(item.due) + '</span>' +
          ui.badge(item.status === 'pending' ? 'Pendente' : 'Agendado', item.status === 'pending' ? 'expense' : 'warning') +
          '<span style="font-size:14px;font-weight:700;min-width:90px;text-align:right;color:' + (cpState.tab === 'pagar' ? 'var(--expense)' : 'var(--income)') + '">' + (cpState.tab === 'receber' ? '+' : '-') + fmt(item.amount) + '</span>' +
          '<button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></div></div>';
      });
      h += '</div>';
      return h;
    },
    mount: function ($page) {
      $page.off('click.cp').on('click.cp', '[data-cptab]', function () { cpState.tab = $(this).data('cptab'); SAS.app.rerender(); });
    }
  };

  // ── Extrato de Contas ──────────────────────────────────────
  var extState = { conta: 'Conta Corrente Itaú' };
  SAS.screens.extrato = {
    render: function () {
      var running = 4820.4;
      var rows = D.EXTRATO_TX.map(function (t) {
        if (t.status === 'balance') return $.extend({}, t, { runningBal: t.balance });
        running += t.amount;
        return $.extend({}, t, { runningBal: running });
      }).reverse();

      var h = '<div class="page-header"><h1>Extrato de Contas</h1>' + ui.periodNav('Abril 2026') + '</div>';
      h += '<div class="grid" style="grid-template-columns:280px 1fr">';
      h += '<div style="display:flex;flex-direction:column;gap:12px">';
      D.EXTRATO_CONTAS.forEach(function (c) {
        var active = extState.conta === c.name;
        h += '<button data-conta="' + c.name + '" style="padding:14px 16px;border-radius:var(--radius);text-align:left;cursor:pointer;font-size:13px;' +
          'background:' + (active ? 'var(--accent-light)' : 'var(--bg-card)') + ';border:1px solid ' + (active ? 'var(--accent)' : 'var(--border)') + ';' +
          'color:' + (active ? 'var(--accent)' : 'var(--text-primary)') + ';font-weight:' + (active ? '700' : '400') + '">' +
          '<p>' + c.name + '</p><p style="font-size:11px;color:' + (active ? 'var(--accent)' : 'var(--text-muted)') + ';margin-top:4px">' + c.saldo + '</p></button>';
      });
      h += '</div>';

      h += '<div class="card flush">';
      rows.forEach(function (tx, i) {
        var isBal = tx.status === 'balance';
        var dot = tx.status === 'confirmed' ? 'var(--income)' : isBal ? 'var(--text-muted)' : 'var(--warning)';
        h += '<div class="flex items-center" style="gap:16px;padding:11px 20px;' + (i < rows.length - 1 ? 'border-bottom:1px solid var(--border-light);' : '') + (isBal ? 'background:var(--bg-hover);' : '') + '">' +
          '<span class="muted" style="font-size:12px;min-width:56px">' + SAS.fmtDate(tx.date) + '</span>' +
          '<span style="flex:1;font-size:13px;font-weight:' + (isBal ? '700' : '500') + ';color:' + (isBal ? 'var(--text-secondary)' : 'var(--text-primary)') + '">' + tx.desc + '</span>' +
          (tx.amount !== 0 ? '<span style="font-size:13px;font-weight:700;color:' + (tx.amount > 0 ? 'var(--income)' : 'var(--expense)') + '">' + (tx.amount > 0 ? '+' : '') + fmt(tx.amount) + '</span>' : '') +
          '<span class="sec" style="font-size:13px;font-weight:700;min-width:100px;text-align:right">' + fmt(tx.runningBal) + '</span>' +
          '<div style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + dot + '"></div></div>';
      });
      h += '</div></div>';
      return h;
    },
    mount: function ($page) {
      $page.off('click.ext').on('click.ext', '[data-conta]', function () { extState.conta = $(this).data('conta'); SAS.app.rerender(); });
    }
  };

  // ── Categorias ─────────────────────────────────────────────
  var catState = { tab: 'expense', expanded: {} };
  SAS.screens.categorias = {
    render: function () {
      var cats = D.CATS_DATA.filter(function (c) { return c.type === catState.tab; });
      var nExp = D.CATS_DATA.filter(function (c) { return c.type === 'expense'; }).length;
      var nInc = D.CATS_DATA.filter(function (c) { return c.type === 'income'; }).length;

      var h = '<div class="page-header"><h1>Categorias</h1>' + ui.btn('Nova Categoria', { icon: 'plus' }) + '</div>';
      h += '<div class="grid" style="grid-template-columns:240px 1fr">';
      // Sidebar tabs
      h += '<div class="card" style="padding:16px"><div style="display:flex;flex-direction:column;gap:4px">';
      h += '<button data-cattab="expense" style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:var(--radius-sm);border:none;cursor:pointer;font-weight:600;font-size:13px;' +
        'background:' + (catState.tab === 'expense' ? 'var(--expense-light)' : 'transparent') + ';color:' + (catState.tab === 'expense' ? 'var(--expense)' : 'var(--text-secondary)') + '">' +
        '<span class="flex items-center gap-8">' + ui.icon('arrowDown', { size: 15 }) + 'Despesas</span>' + ui.badge(nExp, 'expense') + '</button>';
      h += '<button data-cattab="income" style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:var(--radius-sm);border:none;cursor:pointer;font-weight:600;font-size:13px;' +
        'background:' + (catState.tab === 'income' ? 'var(--income-light)' : 'transparent') + ';color:' + (catState.tab === 'income' ? 'var(--income)' : 'var(--text-secondary)') + '">' +
        '<span class="flex items-center gap-8">' + ui.icon('arrowUp', { size: 15 }) + 'Receitas</span>' + ui.badge(nInc, 'income') + '</button>';
      h += '</div></div>';

      // List
      h += '<div class="card flush">';
      cats.forEach(function (cat) {
        var hasSub = cat.sub.length > 0;
        var open = catState.expanded[cat.name];
        h += '<div><div class="list-row" data-cat-toggle="' + ui.esc(cat.name) + '" style="' + (hasSub ? 'cursor:pointer' : 'cursor:default') + '">' +
          '<div class="flex items-center" style="gap:10px">' +
          (hasSub ? ui.icon(open ? 'chevronDown' : 'chevronRight', { size: 14, color: 'var(--text-muted)' }) : '') +
          '<span style="font-size:13px;font-weight:600">' + cat.name + '</span>' +
          (hasSub ? ui.badge(cat.sub.length, 'muted') : '') + '</div>' +
          '<button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></div>';
        if (open) {
          cat.sub.forEach(function (sub) {
            h += '<div class="list-row" style="padding:10px 20px 10px 44px">' +
              '<span class="sec" style="font-size:12px">↳ ' + sub + '</span>' +
              '<button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></div>';
          });
        }
        h += '</div>';
      });
      h += '</div></div>';
      return h;
    },
    mount: function ($page) {
      $page.off('click.cat').on('click.cat', '[data-cattab]', function () { catState.tab = $(this).data('cattab'); SAS.app.rerender(); });
      $page.on('click.catt', '[data-cat-toggle]', function () {
        var name = $(this).data('cat-toggle');
        catState.expanded[name] = !catState.expanded[name];
        SAS.app.rerender();
      });
    }
  };

  // ── Cartões de Crédito ─────────────────────────────────────
  SAS.screens.cartoes = {
    render: function () {
      var h = '<div class="page-header"><h1>Cartões de Crédito</h1>' + ui.btn('Novo Cartão', { icon: 'plus' }) + '</div>';
      h += '<div class="grid grid-2">';
      D.CARDS_DATA.forEach(function (card) {
        var pct = card.used / card.limit * 100;
        var bar = pct > 80 ? 'var(--expense)' : pct > 60 ? 'var(--warning)' : 'var(--accent)';
        h += '<div class="card">' +
          '<div style="background:linear-gradient(135deg,' + card.color + ',' + card.color + '99);border-radius:var(--radius);padding:20px;margin-bottom:20px;position:relative;overflow:hidden;min-height:140px">' +
          '<div style="position:absolute;right:-30px;top:-30px;width:160px;height:160px;border-radius:50%;background:rgba(255,255,255,0.06)"></div>' +
          '<p style="color:rgba(255,255,255,0.7);font-size:11px;font-weight:600;letter-spacing:0.1em">CRÉDITO</p>' +
          '<p style="color:#fff;font-size:18px;font-weight:800;margin-top:24px">' + card.name + '</p>' +
          '<p style="color:rgba(255,255,255,0.6);font-size:13px;margin-top:4px">•••• •••• •••• ' + card.last4 + '</p>' +
          '<p style="color:rgba(255,255,255,0.5);font-size:11px;position:absolute;bottom:16px;right:16px">Vence dia ' + card.due + '</p></div>' +
          '<div class="flex justify-between" style="margin-bottom:10px">' +
          '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Fatura Atual</p><p style="font-size:20px;font-weight:800;color:var(--expense)">' + fmt(card.used) + '</p></div>' +
          '<div style="text-align:right"><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Limite Total</p><p style="font-size:20px;font-weight:800">' + fmt(card.limit) + '</p></div></div>' +
          '<div class="pbar" style="margin-bottom:6px"><div style="width:' + pct + '%;background:' + bar + '"></div></div>' +
          '<div class="flex justify-between muted" style="font-size:12px"><span>' + pct.toFixed(0) + '% utilizado</span><span>' + fmt(card.limit - card.used) + ' disponível</span></div></div>';
      });
      h += '</div>';
      return h;
    }
  };

  // ── Contas Bancárias ───────────────────────────────────────
  SAS.screens.contas = {
    render: function () {
      var total = D.CONTAS_DATA.reduce(function (s, c) { return s + c.balance; }, 0);
      var h = '<div class="page-header"><h1>Contas Bancárias</h1>' + ui.btn('Nova Conta', { icon: 'plus' }) + '</div>';
      h += '<div class="card" style="margin-bottom:16px;padding:16px 20px;display:flex;align-items:center;justify-content:space-between">' +
        '<div><p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Saldo Total</p><p style="font-size:24px;font-weight:800;margin-top:4px">' + fmt(total) + '</p></div>' +
        ui.badge(D.CONTAS_DATA.length + ' contas ativas', 'income') + '</div>';
      h += '<div class="card flush">';
      D.CONTAS_DATA.forEach(function (conta) {
        h += '<div class="list-row" style="padding:16px 20px">' +
          '<div class="flex items-center" style="gap:16px">' +
          '<div style="width:44px;height:44px;border-radius:12px;background:var(--accent-light);display:flex;align-items:center;justify-content:center;flex-shrink:0">' + ui.icon('building', { size: 20, color: 'var(--accent)' }) + '</div>' +
          '<div><p style="font-size:13px;font-weight:700">' + conta.name + '</p><p class="muted" style="font-size:12px;margin-top:2px">' + conta.bank + '</p></div></div>' +
          '<div class="flex items-center" style="gap:16px">' + ui.badge('Disponível', 'income') +
          '<p style="font-size:16px;font-weight:800;min-width:120px;text-align:right">' + fmt(conta.balance) + '</p>' +
          '<button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></div></div>';
      });
      h += '</div>';
      return h;
    }
  };

  // ── Centros de Custo ───────────────────────────────────────
  SAS.screens['centros-custo'] = {
    render: function () {
      var h = '<div class="page-header"><h1>Centros de Custo</h1>' + ui.btn('Novo Centro', { icon: 'plus' }) + '</div>';
      h += '<div class="card flush">';
      D.CENTROS.forEach(function (c, i) {
        h += '<div class="list-row' + (i < D.CENTROS.length - 1 ? ' bordered' : '') + '">' +
          '<div class="flex items-center" style="gap:12px">' +
          '<div style="width:36px;height:36px;border-radius:8px;background:var(--accent-light);display:flex;align-items:center;justify-content:center">' + ui.icon('briefcase', { size: 16, color: 'var(--accent)' }) + '</div>' +
          '<span style="font-size:13px;font-weight:600">' + c + '</span></div>' +
          '<button class="icon-btn sm">' + ui.icon('moreVertical', { size: 14 }) + '</button></div>';
      });
      h += '</div>';
      return h;
    }
  };

  // ── Tags ───────────────────────────────────────────────────
  SAS.screens.tags = {
    render: function () {
      var h = '<div class="page-header"><h1>Tags</h1>' + ui.btn('Nova Tag', { icon: 'plus' }) + '</div>';
      h += '<div class="flex" style="flex-wrap:wrap;gap:12px">';
      D.TAGS.forEach(function (tag) {
        h += '<div class="tag-chip" data-color="' + tag.color + '" style="display:flex;align-items:center;gap:10px;padding:10px 16px;border-radius:40px;background:var(--bg-card);border:1px solid var(--border);cursor:pointer">' +
          '<span style="width:10px;height:10px;border-radius:50%;background:' + tag.color + '"></span>' +
          '<span style="font-size:13px;font-weight:600">#' + tag.name + '</span>' +
          '<span style="font-size:11px;background:var(--bg-hover);padding:2px 7px;border-radius:20px;color:var(--text-muted);font-weight:600">' + tag.count + '</span></div>';
      });
      h += '</div>';
      return h;
    },
    mount: function ($page) {
      $page.off('mouseenter.tags').on('mouseenter.tags', '.tag-chip', function () {
        var c = $(this).data('color');
        $(this).css({ borderColor: c, background: c + '18' });
      }).on('mouseleave.tags', '.tag-chip', function () {
        $(this).css({ borderColor: 'var(--border)', background: 'var(--bg-card)' });
      });
    }
  };
})(window, jQuery);
