/* feature/dashboard.js — fatia Dashboard (resultado mensal, painéis de visão geral). Última
 * fatia a migrar: lê de quase todas as outras. Um arquivo por fatia: domain (balance-sheet,
 * dashboard-aggregations) → infrastructure/secondary (DashboardRepository) → application
 * (DashboardService) → infrastructure/primary (10 painéis + página), cada bloco abaixo é um
 * IIFE independente (comentário original de cada arquivo preservado como separador de seção).
 * Sem domain própria de payable/budget/cartão: lê via AccountsPayableApi (fecha V6),
 * CreditCardsApi (fecha V7), TransactionsApi (V8, já fechado no composition-root), AccountsApi
 * e CategoriesApi (onChange, refresh). BudgetApi (novo — expense-goals.js não estava no
 * roadmap original, achado ao migrar). */
/* _1_domain/balance-sheet.js — balance sheet computation. Pure. */
(function () {
  // No account type is a liability post-card-remodel (cards are no longer accounts) —
  // everything sits on the ATIVO side until the model grows a real liability account type.
  function compute(accounts) {
    let assets = 0;
    (accounts || []).forEach(function (a) {
      assets += window.Domain.Account.currentBalance(a);
    });
    return { assets: assets, liabilities: 0, equity: assets };
  }

  window.Domain = window.Domain || {};
  window.Domain.BalanceSheet = { compute: compute };
})();
/* _1_domain/dashboard-aggregations.js — pure analytics for dashboard panels. */
(function () {
  function txIsExpense(t, categoryNature) {
    if (!t) return false;
    if (t.type) {
      const s = String(t.type).toLowerCase();
      if (s === 'expense') return true;
      if (s === 'income' || s === 'revenue') return false;
    }
    if (categoryNature) return String(categoryNature).toUpperCase() === 'EXPENSE';
    return (+t.amount || 0) < 0;
  }

  function categoryNameFor(t, categories) {
    if (!t || t.categoryId == null) return 'Sem categoria';
    const c = window.Domain.Category.byId(categories, t.categoryId);
    return c ? c.name : 'Sem categoria';
  }

  function categoryNatureFor(t, categories) {
    if (!t || t.categoryId == null) return null;
    const c = window.Domain.Category.byId(categories, t.categoryId);
    return c ? c.nature : null;
  }

  /* Sum |amount| per category name across expense transactions. */
  function expenseByCategory(transactions, categories) {
    const out = {};
    (transactions || []).forEach(function (t) {
      const nature = categoryNatureFor(t, categories);
      if (!txIsExpense(t, nature)) return;
      const name = categoryNameFor(t, categories);
      out[name] = (out[name] || 0) + Math.abs(+t.amount || 0);
    });
    const arr = Object.keys(out).map(function (k) { return { name: k, value: out[k] }; });
    arr.sort(function (a, b) { return b.value - a.value; });
    return arr;
  }

  function topN(items, n) {
    return (items || []).slice(0, n || 5);
  }

  /* Monthly receitas/despesas series over the last `monthsBack` months. */
  function monthlySeries(transactions, monthsBack, now) {
    const m = monthsBack || 4;
    const ref = now || new Date();
    const buckets = [];
    for (let i = m - 1; i >= 0; i--) {
      const d = new Date(ref.getFullYear(), ref.getMonth() - i, 1);
      buckets.push({ month: d.getMonth() + 1, year: d.getFullYear(), receitas: 0, despesas: 0 });
    }
    (transactions || []).forEach(function (t) {
      const d = new Date(t.date);
      const idx = buckets.findIndex(function (b) {
        return b.month === d.getMonth() + 1 && b.year === d.getFullYear();
      });
      if (idx < 0) return;
      const v = Math.abs(+t.amount || 0);
      if (txIsExpense(t)) buckets[idx].despesas += v;
      else                buckets[idx].receitas += v;
    });
    return buckets;
  }

  /* Filter & sort upcoming active payables by due/date ascending. */
  function upcomingPayables(payables) {
    return (payables || [])
      .filter(window.AccountsPayableApi.isActive)
      .slice()
      .sort(function (a, b) {
        const da = new Date(a.due || a.date || 0).getTime();
        const db = new Date(b.due || b.date || 0).getTime();
        return da - db;
      });
  }

  window.Domain = window.Domain || {};
  window.Domain.DashboardAggregations = {
    txIsExpense: txIsExpense,
    categoryNameFor: categoryNameFor,
    categoryNatureFor: categoryNatureFor,
    expenseByCategory: expenseByCategory,
    topN: topN,
    monthlySeries: monthlySeries,
    upcomingPayables: upcomingPayables,
  };
})();
/* _3_infrastructure/secondary/dashboard-repository.js — HTTP adapter for /dashboard endpoints. */
(function () {
  function create(http) {
    return {
      getMonthlyResult: function (month, year) {
        return http.get('/dashboard?month=' + month + '&year=' + year);
      },
      getRecentTransactions: function (limit) {
        return http.get('/accounts/transactions?limit=' + (limit || 5) + '&sort=date,desc');
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.DashboardRepository = { create: create };
})();
/* _2_application/dashboard-service.js — dashboard data orchestration. */
(function () {
  let repo = null;
  let txRepo = null;

  function init(deps) {
    repo        = deps.repo;
    txRepo      = deps.txRepo;
    return { ready: true };
  }

  // A Pagar/Receber derivam de transações pendentes (sem recurso de payables).
  function adaptPending(label) {
    return function (txs) {
      return (Array.isArray(txs) ? txs : []).map(function (t) {
        return {
          id: t.id, description: t.description, due: t.date,
          amount: Math.abs(+t.amount || 0), accountId: t.accountId,
          categoryId: t.categoryId, status: t.status, type: label,
        };
      });
    };
  }

  function monthlyResult(period) {
    return repo.getMonthlyResult(period.month, period.year);
  }

  function recentTransactions(limit) {
    return repo.getRecentTransactions(limit);
  }

  /* Loads everything dashboard panels need in parallel. */
  function loadAll(period) {
    const b = window.Domain.Period.bounds(period);
    return Promise.all([
      txRepo.list('from=' + b.from + '&to=' + b.to),
      txRepo.list('status=pending&type=expense').then(adaptPending('PAYABLE')),
      txRepo.list('status=pending&type=income').then(adaptPending('RECEIVABLE')),
    ]).then(function (arr) {
      return {
        transactions: Array.isArray(arr[0]) ? arr[0] : [],
        payables:     Array.isArray(arr[1]) ? arr[1] : [],
        receivables:  Array.isArray(arr[2]) ? arr[2] : [],
      };
    });
  }

  window.App = window.App || {};
  window.App.DashboardService = {
    init: init,
    monthlyResult: monthlyResult,
    recentTransactions: recentTransactions,
    loadAll: loadAll,
  };
})();
/* pages/dashboard/cash-balances.js — Painel: Saldos de Caixa */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['cash-balances'] = function (p, ctx) {
    var accs = ctx.cashAccounts();
    var total = accs.reduce(function (s, a) { return s + window.Domain.Account.currentBalance(a); }, 0);
    var rows = accs.map(function (a, i) {
      var color = ctx.pickColor(i, a.color);
      var bal = window.Domain.Account.currentBalance(a);
      return (
        '<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-top:1px solid var(--border-light);">' +
          '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
            '<span style="width:8px;height:8px;border-radius:50%;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
            '<span style="font-size:12px;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' + ctx.esc(a.name) + '</span>' +
          '</div>' +
          '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(bal) + ';">' + ctx.esc(ctx.v(bal)) + '</span>' +
        '</div>'
      );
    }).join('');
    if (!accs.length) {
      rows = '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:12px 0;">Sem contas cadastradas.</div>';
    }
    var body =
      '<div style="margin-bottom:12px;">' +
        '<p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;letter-spacing:0.04em;">Total</p>' +
        '<p style="font-size:24px;font-weight:800;margin-top:2px;color:' + window.valueColor(total) + ';">' +
          ctx.esc(ctx.v(total)) +
        '</p>' +
      '</div>' + rows;
    return ctx.panelWrap({ title: 'Saldos de Caixa', icon: p.icon, body: body });
  };
})();
/* pages/dashboard/month-result.js — Painel: Resultado do Mês */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['month-result'] = function (p, ctx) {
    var txs = ctx.currentMonthTxs();
    var inc = 0, exp = 0;
    txs.forEach(function (t) {
      var a = Math.abs(+t.amount || 0);
      if (ctx.txIsExpense(t)) exp += a; else inc += a;
    });
    var res = inc - exp;
    var rows = [
      { label: 'Receitas', value: inc, color: 'var(--income)' },
      { label: 'Despesas', value: exp, color: 'var(--expense)' },
      { label: 'Resultado', value: res, color: res >= 0 ? 'var(--income)' : 'var(--expense)', bold: true },
    ];
    var body = '<div style="display:flex;flex-direction:column;gap:10px;">';
    rows.forEach(function (r) {
      body +=
        '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="font-size:13px;color:var(--text-secondary);font-weight:' + (r.bold ? 700 : 400) + ';">' + ctx.esc(r.label) + '</span>' +
          '<span style="font-size:14px;font-weight:700;color:' + r.color + ';">' + ctx.esc(ctx.v(r.value)) + '</span>' +
        '</div>';
    });
    var series = ctx.monthlySeries(4);
    body += '<div style="height:80px;margin-top:4px;">' + ctx.miniLineChart(series, 80) + '</div>';
    body += '</div>';
    return ctx.panelWrap({ title: 'Resultado do Mês', icon: p.icon, body: body });
  };
})();
/* pages/dashboard/expenses-by-category.js — Painel: Despesas por Categoria */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['expenses-cat'] = function (p, ctx) {
    var data = ctx.expenseByCategory().slice(0, 5);
    return ctx.panelWrap({ title: 'Despesas por Categoria', icon: p.icon, body: ctx.categoryBars(data) });
  };
})();
/* pages/dashboard/revenues-by-category.js — Painel: Receitas por Categoria */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['revenues-cat'] = function (p, ctx) {
    var SERIES_PALETTE = ctx.SERIES_PALETTE;
    var map = {};
    ctx.currentMonthTxs().forEach(function (t) {
      if (ctx.txIsExpense(t)) return;
      var n = ctx.categoryName(t.categoryId);
      map[n] = (map[n] || 0) + Math.abs(+t.amount || 0);
    });
    var list = Object.keys(map).map(function (k, i) {
      return { name: k, amount: map[k], color: SERIES_PALETTE[i % SERIES_PALETTE.length] };
    });
    list.sort(function (a, b) { return b.amount - a.amount; });
    return ctx.panelWrap({ title: 'Receitas por Categoria', icon: p.icon, body: ctx.categoryBars(window.Domain.DashboardAggregations.topN(list, 5)) });
  };
})();
/* pages/dashboard/accounts-payable.js — Painéis: Contas a Pagar / Contas a Receber */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  function renderListaPayable(p, ctx, kind) {
    var isExp = kind === 'expense';
    var src = ctx.upcomingPayables(kind).slice(0, 6);
    var bodyEl;
    if (!src.length) {
      bodyEl = window.emptyState({
        icon: isExp ? 'calendar' : 'arrowUp',
        title: isExp ? 'Sem contas a pagar' : 'Sem contas a receber',
        desc: 'Tudo em dia.',
      });
    } else {
      var color = isExp ? 'var(--expense)' : 'var(--income)';
      var html = '<div style="display:flex;flex-direction:column;gap:0;">';
      src.forEach(function (b) {
        var due = b.due || b.date;
        var dueTxt = '';
        try { dueTxt = window.fmtDate(due); } catch (e) {}
        html +=
          '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
            '<div style="min-width:0;">' +
              '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;text-transform:uppercase;">' +
                ctx.esc(b.description || b.name || '—') +
              '</p>' +
              '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(dueTxt) + '</p>' +
            '</div>' +
            '<span style="font-size:13px;font-weight:700;color:' + color + ';flex-shrink:0;margin-left:10px;">' +
              ctx.esc(ctx.v(Math.abs(+b.amount || 0))) +
            '</span>' +
          '</div>';
      });
      html += '</div>';
      bodyEl = html;
    }
    var action = window.badge(String(ctx.upcomingPayables(kind).length), isExp ? 'expense' : 'income');
    return ctx.panelWrap({
      title: isExp ? 'Contas a Pagar' : 'Contas a Receber',
      icon: isExp ? 'calendar' : 'arrowUp',
      action: action,
      body: bodyEl,
    });
  }

  window.DashboardPanels['accounts-payable'] = function (p, ctx) {
    return renderListaPayable(p, ctx, 'expense');
  };

  window.DashboardPanels['accounts-receivable'] = function (p, ctx) {
    return renderListaPayable(p, ctx, 'income');
  };
})();
/* pages/dashboard/credit-cards.js — Painel: Cartões de Crédito */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['credit-cards'] = function (p, ctx) {
    var accounts = ctx.creditCards(); // accounts with at least one card
    if (!accounts.length) {
      return ctx.panelWrap({
        title: 'Cartões de Crédito', icon: p.icon,
        body: window.emptyState({ icon: 'creditCard', title: 'Sem cartões', desc: 'Cadastre um cartão para visualizar aqui.' }),
      });
    }
    var period = window.Domain.Period.currentMonth();
    var html = '';
    accounts.forEach(function (a, i) {
      var limit = a.creditLimit || 0;
      // Invoice total = every card on the account combined, matched by tx.cardId.
      // Usa a lista inteira (não currentMonthTxs): o ciclo da fatura que vence neste mês começa
      // no mês anterior — ver Domain.Invoice.
      var used = window.CreditCardsApi.accountInvoiceTotal(ctx.allTxs(), a, period);
      var pct = window.CreditCardsApi.usagePct(used, limit);
      var color = ctx.pickColor(i + 4, a.color);
      var barColor = 'var(--' + window.CreditCardsApi.barColorByUsage(pct) + ')';
      html +=
        '<div style="margin-bottom:12px;">' +
          '<div style="display:flex;justify-content:space-between;margin-bottom:6px;">' +
            '<div style="display:flex;align-items:center;gap:6px;min-width:0;">' +
              '<span style="width:10px;height:10px;border-radius:2px;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
              '<span style="font-size:12px;font-weight:600;color:var(--text-primary);">' + ctx.esc(a.name) + '</span>' +
            '</div>' +
            '<span style="font-size:12px;color:var(--expense);font-weight:700;">' + ctx.esc(ctx.v(used)) + '</span>' +
          '</div>' +
          '<div style="height:6px;background:var(--bg-hover);border-radius:3px;overflow:hidden;margin-bottom:4px;">' +
            '<div style="height:100%;border-radius:3px;width:' + pct + '%;background:' + barColor + ';transition:width 0.5s ease;"></div>' +
          '</div>' +
          '<div style="display:flex;justify-content:space-between;font-size:11px;color:var(--text-muted);">' +
            '<span>' + (limit > 0 ? pct.toFixed(0) + '% usado' : 'Sem limite') + '</span>' +
            '<span>Limite: ' + ctx.esc(limit > 0 ? ctx.v(limit) : '—') + '</span>' +
          '</div>' +
        '</div>';
    });
    return ctx.panelWrap({ title: 'Cartões de Crédito', icon: p.icon, body: html });
  };
})();
/* pages/dashboard/cash-flow.js — Painel: Fluxo de Caixa */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['cash-flow'] = function (p, ctx) {
    var series = ctx.monthlySeries(6);
    var body =
      '<div style="display:flex;gap:16px;margin-bottom:10px;font-size:12px;">' +
        '<span style="display:flex;align-items:center;gap:4px;color:var(--text-secondary);">' +
          '<span style="width:14px;height:3px;border-radius:2px;background:var(--income);display:inline-block;"></span>Receitas' +
        '</span>' +
        '<span style="display:flex;align-items:center;gap:4px;color:var(--text-secondary);">' +
          '<span style="width:14px;height:3px;border-radius:2px;background:var(--expense);display:inline-block;"></span>Despesas' +
        '</span>' +
      '</div>' +
      '<div style="height:110px;">' + ctx.miniLineChart(series, 110) + '</div>';
    return ctx.panelWrap({ title: 'Fluxo de Caixa', icon: p.icon, body: body });
  };
})();
/* pages/dashboard/expense-goals.js — Painel: Metas de Despesa */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['expense-goals'] = function (p, ctx) {
    var now = new Date();
    var $card = ctx.panelWrap({
      title: 'Metas de Despesa', icon: p.icon,
      body: '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:20px 0;">Carregando…</div>',
    });
    window.BudgetApi.loadPeriod(window.Domain.Period.create(now.getMonth() + 1, now.getFullYear())).then(function (list) {
      var data = (Array.isArray(list) ? list : []).filter(function (g) {
        return g && (g.budgeted || g.budget || g.target);
      });
      var $body = $card.find('div').last();
      if (!data.length) {
        $body.html(window.emptyState({ icon: 'target', title: 'Sem metas', desc: 'Configure metas em Metas / Orçamento.' }));
        return;
      }
      var html = '<div style="display:flex;flex-direction:column;gap:10px;">';
      data.slice(0, 6).forEach(function (g) {
        var name = g.name || g.categoryName || ctx.categoryName(g.categoryId) || 'Categoria';
        var budgeted = +(g.budgeted || g.budget || g.target || 0);
        var spent = +(g.spent || g.actual || 0);
        var pct = window.BudgetApi.consumptionPct(spent, budgeted);
        var over = window.BudgetApi.isOverBudget(spent, budgeted);
        var barColor = over ? 'var(--expense)' : pct > 80 ? 'var(--warning)' : 'var(--income)';
        html +=
          '<div>' +
            '<div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:12px;">' +
              '<span style="color:var(--text-secondary);">' + ctx.esc(name) + '</span>' +
              '<span style="font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + ';">' +
                ctx.esc(ctx.v(spent)) + ' / ' + ctx.esc(ctx.v(budgeted)) +
              '</span>' +
            '</div>' +
            '<div style="height:6px;background:var(--bg-hover);border-radius:3px;overflow:hidden;">' +
              '<div style="height:100%;border-radius:3px;width:' + pct + '%;background:' + barColor + ';transition:width 0.5s ease;"></div>' +
            '</div>' +
          '</div>';
      });
      html += '</div>';
      $body.html(html);
    }).catch(function () {
      var $body = $card.find('div').last();
      $body.html(window.emptyState({ icon: 'target', title: 'Em breve', desc: 'Endpoint de metas indisponível.' }));
    });
    return $card;
  };
})();
/* pages/dashboard/recent-postings.js — Painel: Últimos Lançamentos */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['recent-postings'] = function (p, ctx) {
    var txs = (ctx.state.data.transactions || []).slice(0, 7);
    if (!txs.length) {
      return ctx.panelWrap({
        title: 'Últimos Lançamentos', icon: p.icon,
        body: window.emptyState({ icon: 'list', title: 'Sem lançamentos', desc: 'Nenhum movimento ainda.' }),
      });
    }
    var html = '';
    txs.forEach(function (t) {
      var isExp = ctx.txIsExpense(t);
      var color = isExp ? 'var(--expense)' : 'var(--income)';
      var amt   = Math.abs(+t.amount || 0);
      var dateTxt = '';
      try { dateTxt = window.fmtDate(t.date); } catch (e) {}
      var cat = ctx.categoryName(t.categoryId);
      html +=
        '<div style="display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="width:28px;height:28px;border-radius:6px;background:' + (isExp ? 'var(--expense-light)' : 'var(--income-light)') +
            ';color:' + color + ';display:flex;align-items:center;justify-content:center;flex-shrink:0;">' +
            window.icon(isExp ? 'arrowDown' : 'arrowUp', 14) +
          '</span>' +
          '<div style="flex:1;min-width:0;">' +
            '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;text-transform:uppercase;">' +
              ctx.esc(window.Domain.Transaction.describe(t) || '—') +
            '</p>' +
            '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(cat) + (dateTxt ? ' · ' + ctx.esc(dateTxt) : '') + '</p>' +
          '</div>' +
          '<span style="font-size:12px;font-weight:700;color:' + color + ';flex-shrink:0;">' +
            ctx.esc(ctx.v(amt)) +
          '</span>' +
        '</div>';
    });
    var action = $(
      '<button class="btn btn-ghost btn-sm" data-act="goto-tx" type="button">' +
        '<span>Ver todos</span>' +
      '</button>'
    );
    return ctx.panelWrap({ title: 'Últimos Lançamentos', icon: p.icon, action: action, body: html });
  };
})();
/* pages/dashboard/balance-sheet.js — Painel: Balanço Patrimonial */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['balance-sheet'] = function (p, ctx) {
    var sheet = window.Domain.BalanceSheet.compute(ctx.cbdAccounts());
    var rows = [
      { label: 'Ativo Total',     value: sheet.assets,      color: 'var(--income)' },
      { label: 'Passivo Total',   value: sheet.liabilities,    color: 'var(--expense)' },
      { label: 'Patrimônio Líq.', value: sheet.equity, color: 'var(--accent)', bold: true },
    ];
    var body = '<div style="display:flex;flex-direction:column;gap:8px;">';
    rows.forEach(function (r) {
      body +=
        '<div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="font-size:13px;color:var(--text-secondary);font-weight:' + (r.bold ? 700 : 400) + ';">' + ctx.esc(r.label) + '</span>' +
          '<span style="font-size:14px;font-weight:700;color:' + r.color + ';">' + ctx.esc(ctx.v(r.value)) + '</span>' +
        '</div>';
    });
    body += '</div>';
    return ctx.panelWrap({ title: 'Balanço Patrimonial', icon: p.icon, body: body });
  };
})();
/* pages/dashboard.js — Visão Geral (painéis configuráveis + drag-reorder + modal personalizar). */
(function () {
  window.Pages = window.Pages || {};
  window.DashboardPanels = window.DashboardPanels || {};

  // ── Definição dos painéis (port direto de dashboard.jsx) ──────
  const ALL_PANELS = [
    { id: 'cash-balances',       label: 'Saldos de caixa',                defaultOn: true,  icon: 'building'     },
    { id: 'month-result',      label: 'Resultado do mês',               defaultOn: true,  icon: 'activity'     },
    { id: 'expenses-cat',       label: 'Despesas por categoria',         defaultOn: true,  icon: 'pieChart'     },
    { id: 'accounts-payable',       label: 'Contas a pagar',                 defaultOn: true,  icon: 'calendar'     },
    { id: 'accounts-receivable',     label: 'Contas a receber',               defaultOn: false, icon: 'arrowUp'      },
    { id: 'credit-cards',    label: 'Cartões de crédito',             defaultOn: true,  icon: 'creditCard'   },
    // { id: 'cash-flow',        label: 'Fluxo de caixa',                 defaultOn: true,  icon: 'trendingUp'   },
    // { id: 'expense-goals',      label: 'Metas de despesa',               defaultOn: true,  icon: 'target'       },
    { id: 'recent-postings',       label: 'Últimos lançamentos',            defaultOn: true,  icon: 'list'         },
    // { id: 'balance-sheet',            label: 'Balanço patrimonial',            defaultOn: false, icon: 'database'     },
    // { id: 'expenses-center',    label: 'Despesas por centro',            defaultOn: false, icon: 'barChart'     },
    // { id: 'expense-goals-ctr',  label: 'Metas de despesa por centro',    defaultOn: false, icon: 'target'       },
    // { id: 'saving-goals',     label: 'Metas de economia',              defaultOn: false, icon: 'target'       },
    // { id: 'investment-goals',       label: 'Metas de investimentos',         defaultOn: false, icon: 'target'       },
    // { id: 'revenue-goals',      label: 'Metas de receita',               defaultOn: false, icon: 'target'       },
    // { id: 'revenue-goals-ctr',  label: 'Metas de receita por centro',    defaultOn: false, icon: 'target'       },
    // { id: 'revenues-cat',       label: 'Receitas por categoria',         defaultOn: false, icon: 'pieChart'     },
    // { id: 'revenues-center',    label: 'Receitas por centro',            defaultOn: false, icon: 'barChart'     },
    // { id: 'cash-results',   label: 'Resultados de caixa',            defaultOn: false, icon: 'activity'     },
    // { id: 'results-center',  label: 'Resultados por centro',          defaultOn: false, icon: 'barChart'     },
  ];

  const DEFAULT_ENABLED = {};
  ALL_PANELS.forEach(function (p) { DEFAULT_ENABLED[p.id] = p.defaultOn; });

  const DEFAULT_SETTINGS = {
    viewMode: 'cash',
    columns: 2,
    scrollPanels: false,
    includeInvestments: true,
    enabled: DEFAULT_ENABLED,
    panelOrder: ALL_PANELS.map(function (p) { return p.id; }),
  };

  function loadSettings() {
    const parsed = window.App.PreferencesService.getDashboardSettings();
    if (!parsed) return cloneSettings(DEFAULT_SETTINGS);
    const merged = $.extend(true, {}, DEFAULT_SETTINGS, parsed);
    // Make sure new panels appear in enabled/panelOrder
    ALL_PANELS.forEach(function (p) {
      if (!(p.id in merged.enabled)) merged.enabled[p.id] = p.defaultOn;
      if (merged.panelOrder.indexOf(p.id) === -1) merged.panelOrder.push(p.id);
    });
    return merged;
  }

  function cloneSettings(s) {
    return JSON.parse(JSON.stringify(s));
  }

  function saveSettings(s) {
    window.App.PreferencesService.saveDashboardSettings(s);
  }

  // Color palette for accounts / categories that don't carry one.
  const SERIES_PALETTE = [
    '#6366F1', '#38BDF8', '#F59E0B', '#10B981',
    '#F43F5E', '#A78BFA', '#820AD1', '#1C2951'
  ];
  function pickColor(i, fallback) {
    return fallback || SERIES_PALETTE[i % SERIES_PALETTE.length];
  }

  // ── smoothPath (Catmull-Rom-ish) — porta de theme.jsx ────────
  function smoothPath(pts) {
    if (!pts || pts.length < 2) return '';
    let d = 'M ' + pts[0].x + ',' + pts[0].y;
    for (let i = 1; i < pts.length; i++) {
      const p0 = pts[Math.max(0, i - 2)];
      const p1 = pts[i - 1];
      const p2 = pts[i];
      const p3 = pts[Math.min(pts.length - 1, i + 1)];
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;
      d += ' C ' + cp1x + ',' + cp1y + ' ' + cp2x + ',' + cp2y + ' ' + p2.x + ',' + p2.y;
    }
    return d;
  }

  // ── Per-mount state ──────────────────────────────────────────
  let state = null;

  function resetState() {
    state = {
      $root: null,
      settings: loadSettings(),
      hideValues: false,
      customizeOpen: false,
      draggedId: null,
      dragOverId: null,
      data: {
        transactions: [],
        payables: [],
        receivables: [],
        loaded: false,
      },
    };
  }

  // Accounts read from App.CacheStore (hydrated at login, SSE-refreshed).
  function cbdAccounts() {
    return window.App.CacheStore.accounts();
  }


  // ── Currency formatting respecting hideValues ────────────────
  function v(n) {
    if (state.hideValues) return 'R$ •••••';
    return window.fmt(n);
  }

  // ── Data loading ─────────────────────────────────────────────
  // Accounts/categories are read from App.CacheStore (cache-only, SSE-driven);
  // only transactions/payables/receivables are fetched here.
  function loadAll() {
    const jobs = [
      window.TransactionsApi.list('limit=500&sort=date,desc').catch(function () { return []; }),
      window.AccountsPayableApi.listPayable().catch(function () { return []; }),
      window.AccountsPayableApi.listReceivable().catch(function () { return []; }),
    ];
    return Promise.all(jobs).then(function (arr) {
      state.data.transactions = Array.isArray(arr[0]) ? arr[0] : [];
      state.data.payables = Array.isArray(arr[1]) ? arr[1] : [];
      state.data.receivables = Array.isArray(arr[2]) ? arr[2] : [];
      state.data.loaded = true;
    });
  }

  // ── Data derivations ─────────────────────────────────────────
  function cashAccounts()   { return cbdAccounts(); }
  function creditCards()    { return window.CreditCardsApi.accountsWithCards(); }

  function currentMonthTxs() {
    const p = window.Domain.Period.currentMonth();
    return (state.data.transactions || []).filter(function (t) {
      return t.date && window.Domain.Period.containsDate(p, t.date);
    });
  }

  function categoryName(catId) {
    return window.Domain.DashboardAggregations.categoryNameFor({ categoryId: catId }, window.App.CacheStore.categories());
  }

  function txIsExpense(t) {
    const nat = window.Domain.DashboardAggregations.categoryNatureFor(t, window.App.CacheStore.categories());
    return window.Domain.DashboardAggregations.txIsExpense(t, nat);
  }

  function expenseByCategory() {
    const list = window.Domain.DashboardAggregations.expenseByCategory(
      currentMonthTxs(),
      window.App.CacheStore.categories()
    );
    return list.map(function (e, i) {
      return { name: e.name, amount: e.value, color: SERIES_PALETTE[i % SERIES_PALETTE.length] };
    });
  }

  function monthlySeries(nMonths) {
    const pts = window.Domain.DashboardAggregations.monthlySeries(state.data.transactions || [], nMonths);
    return pts.map(function (p) {
      const d = new Date(p.year, p.month - 1, 1);
      return Object.assign({}, p, { month: monthShortLabel(d) });
    });
  }

  function monthShortLabel(d) {
    const s = new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(d).replace('.', '');
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  function upcomingPayables(typ) {
    const src = typ === 'expense' ? state.data.payables : state.data.receivables;
    return window.Domain.DashboardAggregations.upcomingPayables(src);
  }

  // ── Charts ───────────────────────────────────────────────────
  function miniLineChart(data, height) {
    height = height || 100;
    const W = 400, H = height;
    const pad = { l: 10, r: 10, t: 8, b: 20 };
    const cw = W - pad.l - pad.r;
    const ch = H - pad.t - pad.b;
    const flat = [];
    data.forEach(function (d) { flat.push(d.receitas || 0); flat.push(d.despesas || 0); });
    let maxV = Math.max.apply(null, flat) * 1.1;
    if (!maxV || !isFinite(maxV)) maxV = 1;
    function toX(i) { return pad.l + (i / Math.max(1, data.length - 1)) * cw; }
    function toY(val) { return pad.t + (1 - val / maxV) * ch; }
    const incPts = data.map(function (d, i) { return { x: toX(i), y: toY(d.receitas || 0) }; });
    const expPts = data.map(function (d, i) { return { x: toX(i), y: toY(d.despesas || 0) }; });
    const lastX = toX(data.length - 1);
    const areaInc = smoothPath(incPts) + ' L ' + lastX + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';
    const areaExp = smoothPath(expPts) + ' L ' + lastX + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';

    const defs =
      '<defs>' +
        '<linearGradient id="mg-inc" x1="0" y1="0" x2="0" y2="1">' +
          '<stop offset="0%" stop-color="var(--income)" stop-opacity="0.2"/>' +
          '<stop offset="100%" stop-color="var(--income)" stop-opacity="0.01"/>' +
        '</linearGradient>' +
        '<linearGradient id="mg-exp" x1="0" y1="0" x2="0" y2="1">' +
          '<stop offset="0%" stop-color="var(--expense)" stop-opacity="0.15"/>' +
          '<stop offset="100%" stop-color="var(--expense)" stop-opacity="0.01"/>' +
        '</linearGradient>' +
      '</defs>';

    const labels = data.map(function (d, i) {
      return '<text x="' + toX(i) + '" y="' + (H - 6) + '" text-anchor="middle" font-size="9" fill="var(--text-muted)">' +
        esc(d.month) + '</text>';
    }).join('');

    function circles(pts, color) {
      return pts.map(function (p) {
        return '<circle cx="' + p.x + '" cy="' + p.y + '" r="3" fill="' + color + '" stroke="var(--bg-card)" stroke-width="1.5"/>';
      }).join('');
    }

    return (
      '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:100%;display:block;">' +
        defs +
        labels +
        '<path d="' + areaInc + '" fill="url(#mg-inc)"/>' +
        '<path d="' + areaExp + '" fill="url(#mg-exp)"/>' +
        '<path d="' + smoothPath(incPts) + '" fill="none" stroke="var(--income)" stroke-width="2" stroke-linecap="round"/>' +
        '<path d="' + smoothPath(expPts) + '" fill="none" stroke="var(--expense)" stroke-width="2" stroke-linecap="round"/>' +
        circles(incPts, 'var(--income)') +
        circles(expPts, 'var(--expense)') +
      '</svg>'
    );
  }

  function categoryBars(data) {
    if (!data.length) {
      return window.emptyState({ icon: 'pieChart', title: 'Sem dados', desc: 'Nenhum lançamento no período.' });
    }
    let max = Math.max.apply(null, data.map(function (d) { return d.amount; }));
    if (!max) max = 1;
    let html = '<div style="display:flex;flex-direction:column;gap:8px;">';
    data.forEach(function (d) {
      const pct = (d.amount / max) * 100;
      html +=
        '<div>' +
          '<div style="display:flex;justify-content:space-between;margin-bottom:4px;">' +
            '<span style="font-size:12px;color:var(--text-secondary);">' + esc(d.name) + '</span>' +
            '<span style="font-size:12px;font-weight:700;color:' + esc(d.color) + ';">' + esc(v(d.amount)) + '</span>' +
          '</div>' +
          '<div style="height:6px;background:var(--bg-hover);border-radius:3px;overflow:hidden;">' +
            '<div style="height:100%;border-radius:3px;width:' + pct + '%;background:' + esc(d.color) + ';transition:width 0.5s ease;"></div>' +
          '</div>' +
        '</div>';
    });
    html += '</div>';
    return html;
  }

  // ── Panel skeleton (PanelWrap) ───────────────────────────────
  function panelWrap(opts) {
    // opts: { title, icon, action: jQuery|string, body: jQuery|string }
    const $card = $('<div class="card" style="padding:0;overflow:hidden;display:flex;flex-direction:column;"></div>');

    const $head = $(
      '<div style="' +
        'padding:11px 16px 10px;border-bottom:1px solid var(--border);' +
        'display:flex;align-items:center;justify-content:space-between;flex-shrink:0;' +
        'cursor:grab;"></div>'
    );

    const gripSvg =
      '<svg width="10" height="14" viewBox="0 0 10 14" fill="var(--text-muted)" style="flex-shrink:0;opacity:0.5;">' +
        '<circle cx="2" cy="2" r="1.3"/><circle cx="2" cy="6" r="1.3"/><circle cx="2" cy="11" r="1.3"/>' +
        '<circle cx="6" cy="2" r="1.3"/><circle cx="6" cy="6" r="1.3"/><circle cx="6" cy="11" r="1.3"/>' +
      '</svg>';

    const $left = $(
      '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
        gripSvg +
        '<span style="display:flex;align-items:center;color:var(--text-muted);flex-shrink:0;">' +
          window.icon(opts.icon || 'activity', 14) +
        '</span>' +
        '<span style="font-size:13px;font-weight:700;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' +
          esc(opts.title) +
        '</span>' +
      '</div>'
    );
    $head.append($left);

    const $right = $('<div style="display:flex;align-items:center;gap:6px;flex-shrink:0;"></div>');
    if (opts.action) {
      if (typeof opts.action === 'string') $right.append(opts.action);
      else $right.append(opts.action);
    }
    $head.append($right);

    $card.append($head);

    let bodyStyle = 'padding:14px 16px;flex:1;overflow:hidden;';
    if (state.settings.scrollPanels) {
      bodyStyle = 'padding:14px 16px;flex:1;overflow:auto;max-height:340px;';
    }
    const $body = $('<div style="' + bodyStyle + '"></div>');
    if (opts.body) {
      if (typeof opts.body === 'string') $body.html(opts.body);
      else $body.append(opts.body);
    }
    $card.append($body);

    return $card;
  }

  function stubPanel(p) {
    return panelWrap({
      title: p.label,
      icon: p.icon || 'barChart',
      body: window.emptyState({ icon: 'activity', title: 'Em breve', desc: 'Dados disponíveis em breve.' }),
    });
  }

  // ── Panel context (shared utilities for external panel files) ─
  function buildCtx() {
    return {
      state:              state,
      panelWrap:          panelWrap,
      v:                  v,
      esc:                esc,
      pickColor:          pickColor,
      cashAccounts:       cashAccounts,
      creditCards:        creditCards,
      cbdAccounts:        cbdAccounts,
      currentMonthTxs:    currentMonthTxs,
      allTxs:             function () { return state.data.transactions || []; },
      categoryName:       categoryName,
      txIsExpense:        txIsExpense,
      expenseByCategory:  expenseByCategory,
      monthlySeries:      monthlySeries,
      upcomingPayables:   upcomingPayables,
      categoryBars:       categoryBars,
      miniLineChart:      miniLineChart,
      stubPanel:          stubPanel,
      SERIES_PALETTE:     SERIES_PALETTE,
    };
  }

  // ── Individual panel renderers ───────────────────────────────
  function renderPanel(p) {
    if (!state.data.loaded) {
      return panelWrap({
        title: p.label,
        icon: p.icon,
        body: '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:20px 0;">Carregando…</div>',
      });
    }

    var renderer = window.DashboardPanels[p.id];
    if (renderer) {
      return renderer(p, buildCtx());
    }

    return stubPanel(p);
  }

  // ── Render ───────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;
    $root.empty();

    // Page header
    const $header = window.pageHeader({
      title: 'Visão Geral',
      actions: [
        $('<button class="icon-btn" data-act="toggle-hide" type="button" title="' +
          (state.hideValues ? 'Mostrar valores' : 'Ocultar valores') +
          '" style="width:34px;height:34px;">' +
          window.icon(state.hideValues ? 'eyeOff' : 'eye', 16) +
          '</button>'),
        window.btn({
          variant: 'secondary', size: 'sm', icon: 'settings', label: 'Personalizar',
          attrs: 'data-act="customize"',
        })
      ]
    });

    $root.append($header);

    // Enabled panels in order
    const order = (state.settings.panelOrder && state.settings.panelOrder.length)
      ? state.settings.panelOrder
      : ALL_PANELS.map(function (p) { return p.id; });

    const enabled = order
      .map(function (id) { return ALL_PANELS.find(function (p) { return p.id === id; }); })
      .filter(function (p) { return p && state.settings.enabled[p.id]; });

    if (enabled.length === 0) {
      $root.append(window.emptyState({
        icon: 'settings',
        title: 'Nenhum painel habilitado',
        desc: 'Clique em "Personalizar" para adicionar painéis à visão geral.',
      }));
      return;
    }

    const cols = state.settings.columns || 2;
    const $grid = $(
      '<div class="dash-grid" style="' +
        'display:grid;' +
        'grid-template-columns:repeat(' + cols + ', 1fr);' +
        'gap:14px;align-items:start;"></div>'
    );

    enabled.forEach(function (p) {
      const isDragged  = state.draggedId === p.id;
      const isDragOver = state.dragOverId === p.id && state.draggedId && state.draggedId !== p.id;
      const $wrap = $(
        '<div data-panel="' + esc(p.id) + '" draggable="true" style="' +
          'opacity:' + (isDragged ? 0.35 : 1) + ';' +
          'transform:' + (isDragOver ? 'scale(1.01)' : 'none') + ';' +
          'transition:opacity 0.15s, transform 0.15s;' +
          'border-radius:var(--radius);' +
          'outline:' + (isDragOver ? '2px solid var(--accent)' : '2px solid transparent') + ';' +
        '"></div>'
      );
      $wrap.append(renderPanel(p));
      $grid.append($wrap);
    });

    $root.append($grid);
  }

  // ── Customize modal ──────────────────────────────────────────
  function openCustomizeModal() {
    const local = cloneSettings(state.settings);

    function radioGroup(field, options) {
      let html = '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-top:6px;">';
      options.forEach(function (opt) {
        const key = field + '|' + opt.value;
        const isChecked = String(local[field]) === String(opt.value);
        html +=
          '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;color:var(--text-primary);">' +
            '<input type="radio" data-radio-key="' + esc(key) + '" name="' + esc(field) + '" value="' + esc(opt.value) + '"' +
              (isChecked ? ' checked' : '') +
              ' style="accent-color:var(--accent);cursor:pointer;"/>' +
            esc(opt.label) +
          '</label>';
      });
      html += '</div>';
      return html;
    }

    function section(title, contentHtml, first) {
      return (
        '<div' + (first ? '' : ' style="border-top:1px solid var(--border);padding-top:16px;"') + '>' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px;">' + esc(title) + '</p>' +
          contentHtml +
        '</div>'
      );
    }

    let panelsListHtml = '<div style="display:flex;flex-direction:column;gap:6px;">';
    ALL_PANELS.forEach(function (pp) {
      const checked = !!local.enabled[pp.id];
      panelsListHtml +=
        '<label data-panel-row="1" style="display:flex;align-items:center;gap:10px;padding:7px 10px;border-radius:var(--radius-sm);cursor:pointer;transition:background var(--transition);">' +
          '<input type="checkbox" data-panel-toggle="' + esc(pp.id) + '"' + (checked ? ' checked' : '') +
            ' style="width:15px;height:15px;accent-color:var(--accent);cursor:pointer;flex-shrink:0;"/>' +
          '<span style="font-size:13px;color:var(--text-primary);">' + esc(pp.label) + '</span>' +
        '</label>';
    });
    panelsListHtml += '</div>';

    const bodyHtml =
      '<div style="display:flex;flex-direction:column;gap:18px;">' +
        section(
          'Exibir painéis de receitas e despesas na visão de',
          radioGroup('viewMode', [
            { value: 'cash',       label: 'Caixa' },
            { value: 'accrual', label: 'Competência' },
            { value: 'define',     label: 'Definir em cada painel' },
          ]),
          true
        ) +
        section(
          'Layout de exibição',
          radioGroup('columns', [
            { value: 1, label: '1 coluna' },
            { value: 2, label: '2 colunas' },
            { value: 3, label: '3 colunas' },
          ])
        ) +
        section(
          'Utilizar barra de rolagem nos painéis',
          radioGroup('scrollPanels', [
            { value: true,  label: 'Sim' },
            { value: false, label: 'Não' },
          ])
        ) +
        section(
          'Considerar despesas e receitas vinculadas ao módulo de investimentos',
          radioGroup('includeInvestments', [
            { value: true,  label: 'Sim' },
            { value: false, label: 'Não' },
          ])
        ) +
        section(
          'Habilitar e desabilitar painéis',
          panelsListHtml
        ) +
      '</div>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="customize-save" type="button"',
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($save);

    const m = window.modal({
      title: 'Personalizar visão geral',
      body: bodyHtml,
      footer: $footer,
    });
    m.open();

    // Cast helper for radio values (numbers/booleans)
    function castValue(raw, current) {
      if (typeof current === 'number') return parseInt(raw, 10);
      if (typeof current === 'boolean') return raw === 'true';
      return raw;
    }

    // Radio changes update local copy
    m.$body.on('change', 'input[type=radio]', function () {
      const $this = $(this);
      const field = $this.attr('name');
      const raw   = $this.attr('value');
      local[field] = castValue(raw, DEFAULT_SETTINGS[field]);
    });

    // Panel toggles
    m.$body.on('change', 'input[data-panel-toggle]', function () {
      const id = $(this).attr('data-panel-toggle');
      local.enabled = $.extend({}, local.enabled);
      local.enabled[id] = $(this).prop('checked');
    });

    // Row hover effect
    m.$body.on('mouseenter', '[data-panel-row]', function () {
      $(this).css('background', 'var(--bg-hover)');
    }).on('mouseleave', '[data-panel-row]', function () {
      $(this).css('background', 'transparent');
    });

    // Save
    m.$el.on('click', '[data-act=customize-save]', function () {
      // Ensure panelOrder still references all panels
      const order = (local.panelOrder || []).slice();
      ALL_PANELS.forEach(function (p) {
        if (order.indexOf(p.id) === -1) order.push(p.id);
      });
      local.panelOrder = order;
      state.settings = local;
      saveSettings(state.settings);
      m.close();
      render();
    });
  }

  // ── Drag & drop ──────────────────────────────────────────────
  function bindDragEvents($root) {
    $root.on('dragstart.dash', '[data-panel]', function (e) {
      const id = $(this).attr('data-panel');
      state.draggedId = id;
      const dt = e.originalEvent && e.originalEvent.dataTransfer;
      if (dt) {
        dt.effectAllowed = 'move';
        try { dt.setData('text/plain', id); } catch (err) {}
      }
      // Light visual update without full re-render
      $(this).css('opacity', '0.35');
    });

    $root.on('dragover.dash', '[data-panel]', function (e) {
      e.preventDefault();
      const dt = e.originalEvent && e.originalEvent.dataTransfer;
      if (dt) dt.dropEffect = 'move';
      const id = $(this).attr('data-panel');
      if (state.draggedId && id !== state.draggedId && state.dragOverId !== id) {
        state.dragOverId = id;
        // Update outlines without full re-render
        $root.find('[data-panel]').each(function () {
          const pid = $(this).attr('data-panel');
          const isOver = pid === state.dragOverId && pid !== state.draggedId;
          $(this).css({
            outline: isOver ? '2px solid var(--accent)' : '2px solid transparent',
            transform: isOver ? 'scale(1.01)' : 'none',
          });
        });
      }
    });

    $root.on('drop.dash', '[data-panel]', function (e) {
      e.preventDefault();
      const targetId = $(this).attr('data-panel');
      const draggedId = state.draggedId;
      if (!draggedId || draggedId === targetId) {
        state.draggedId = null;
        state.dragOverId = null;
        render();
        return;
      }
      const order = (state.settings.panelOrder && state.settings.panelOrder.length)
        ? state.settings.panelOrder.slice()
        : ALL_PANELS.map(function (p) { return p.id; });
      const fromIdx = order.indexOf(draggedId);
      const toIdx   = order.indexOf(targetId);
      if (fromIdx !== -1 && toIdx !== -1) {
        order.splice(fromIdx, 1);
        order.splice(toIdx, 0, draggedId);
        state.settings.panelOrder = order;
        saveSettings(state.settings);
      }
      state.draggedId = null;
      state.dragOverId = null;
      render();
    });

    $root.on('dragend.dash', '[data-panel]', function () {
      state.draggedId = null;
      state.dragOverId = null;
      render();
    });
  }

  // ── Event delegation ─────────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.dash', '[data-act=toggle-hide]', function () {
      state.hideValues = !state.hideValues;
      render();
    });
    $root.on('click.dash', '[data-act=customize]', function () {
      openCustomizeModal();
    });

    $root.on('click.dash', '[data-act=goto-tx]', function () {
      window.location.hash = '#/transactions';
    });
    bindDragEvents($root);
  }

  // ── Lifecycle ────────────────────────────────────────────────
  window.Pages['dashboard'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadAll().then(function () {
        if (state && state.$root === $root) render();
      }).catch(function (err) {
        window.toast((err && err.message) || 'Falha ao carregar dashboard');
      });
      // Several panels depend on cadastro state — re-render on any change.
      state.unsubscribeAcc = window.AccountsApi.onChange(function () { render(); });
      state.unsubscribeCat = window.CategoriesApi.onChange(function () { render(); });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.dash');
      }
      if (state && state.unsubscribeAcc) state.unsubscribeAcc();
      if (state && state.unsubscribeCat) state.unsubscribeCat();
      state = null;
    },
  };
})();
