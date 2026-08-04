/* _1_domain/invoice.js — ciclo de fatura de cartão + colapso das compras numa linha só. Pure.
 *
 * O ciclo é da CONTA, não do cartão (closingDay/dueDay são colunas de F002_ACCOUNT, compartilhadas
 * por todos os cartões dela). Regra — contrato em docs/backend/invoice-cycle.md, espelhada no
 * backend por f002._0_domain.model.InvoiceCycle; mudou aqui, muda lá:
 *   close(m)  = dia closingDay do mês m, clampado ao tamanho do mês
 *   fechamento: a compra d pertence ao ciclo m tal que close(m-1) < d <= close(m)
 *   vencimento: primeira data com dia dueDay ESTRITAMENTE depois de close(m) — resolve sozinho o
 *               caso dueDay <= closingDay, em que a fatura vence no mês seguinte ao fechamento
 *
 * Datas trafegam como 'yyyy-mm-dd', comparáveis com < e > (ordem lexicográfica = cronológica).
 */
(function () {
  const DEFAULT_CLOSING_DAY = 1;
  const DEFAULT_DUE_DAY = 10;

  function pad2(n) { return n < 10 ? '0' + n : '' + n; }
  function day(dateLike) { return String(dateLike || '').slice(0, 10); }

  function periodOf(dateStr) {
    const d = day(dateStr);
    return window.Domain.Period.create(parseInt(d.slice(5, 7), 10), parseInt(d.slice(0, 4), 10));
  }

  function atDay(period, dayOfMonth) {
    const last = new Date(period.year, period.month, 0).getDate();
    return period.year + '-' + pad2(period.month) + '-' + pad2(Math.min(dayOfMonth, last));
  }

  function configuredDay(value, fallback) {
    const n = +value;
    return n > 0 ? n : fallback;
  }

  function closingOn(account, period) {
    return atDay(period, configuredDay(account && account.closingDay, DEFAULT_CLOSING_DAY));
  }

  function dueOn(account, period) {
    return atDay(period, configuredDay(account && account.dueDay, DEFAULT_DUE_DAY));
  }

  function shift(period, n) { return window.Domain.Period.shift(period, n); }

  /* Vencimento da fatura que fecha no mês `closingPeriod`. */
  function dueForClosing(account, closingPeriod) {
    const closing = closingOn(account, closingPeriod);
    const candidate = dueOn(account, closingPeriod);
    return candidate > closing ? candidate : dueOn(account, shift(closingPeriod, 1));
  }

  /* Vencimento da fatura em que a compra `dateStr` cai. */
  function dueDate(account, dateStr) {
    const d = day(dateStr);
    const period = periodOf(d);
    return dueForClosing(account, d > closingOn(account, period) ? shift(period, 1) : period);
  }

  /* Compras cobertas pela fatura que vence em `dueDateStr` — intervalo fechado {from, to}. */
  function cycleFor(account, dueDateStr) {
    const due = day(dueDateStr);
    const period = periodOf(due);
    const closingPeriod = closingOn(account, period) < due ? period : shift(period, -1);
    const previousClose = new Date(closingOn(account, shift(closingPeriod, -1)) + 'T00:00:00');
    previousClose.setDate(previousClose.getDate() + 1);
    return {
      from: previousClose.getFullYear() + '-' + pad2(previousClose.getMonth() + 1) + '-' + pad2(previousClose.getDate()),
      to: closingOn(account, closingPeriod),
    };
  }

  /* Vencimentos que caem dentro de `period` — normalmente um só; zero ou dois em contas cujo
     closingDay/dueDay empurra a fatura para fora/duas vezes dentro do mês. */
  function dueDatesIn(account, period) {
    const out = [];
    [shift(period, -1), period, shift(period, 1)].forEach(function (closingPeriod) {
      const due = dueForClosing(account, closingPeriod);
      const p = periodOf(due);
      if (p.year === period.year && p.month === period.month && out.indexOf(due) < 0) out.push(due);
    });
    return out.sort();
  }

  /* Janela de datas que precisa ser buscada para montar `period`: o próprio mês (transações sem
     cartão) unido aos ciclos das faturas que vencem nele (compras de meses anteriores). */
  function fetchWindow(accounts, period) {
    const b = window.Domain.Period.bounds(period);
    let from = b.from;
    let to = b.to;
    (accounts || []).forEach(function (a) {
      if (!window.Domain.Account.hasCards(a)) return;
      dueDatesIn(a, period).forEach(function (due) {
        const c = cycleFor(a, due);
        if (c.from < from) from = c.from;
        if (c.to > to) to = c.to;
      });
    });
    return { from: from, to: to };
  }

  function label(account, card) {
    return 'FATURA · ' + ((account && account.name) || '—') + ' ' + ((card && card.last4) || '????');
  }

  function today() {
    const d = new Date();
    return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
  }

  /* Substitui as compras de cartão por UMA linha sintética por (cartão, vencimento) e mantém as
     demais transações do período intactas. Entrada: transações da janela alargada (ver
     fetchWindow) + as contas do cache (que carregam `cards`). Saída: linhas do período.
     Compra cujo vencimento cai fora de `period` é descartada — ela aparece no mês em que vence. */
  function collapse(txs, accounts, period) {
    const bounds = window.Domain.Period.bounds(period);
    const cardIndex = {};
    (accounts || []).forEach(function (a) {
      (a.cards || []).forEach(function (c) { cardIndex[String(c.id)] = { card: c, account: a }; });
    });

    const rows = [];
    const invoices = {};
    (txs || []).forEach(function (t) {
      const ref = t.cardId != null ? cardIndex[String(t.cardId)] : null;
      if (!ref) {
        // Sem cartão (ou cartão desconhecido no cache): linha normal, escopada ao período.
        const d = day(t.date);
        if (d >= bounds.from && d <= bounds.to) rows.push(t);
        return;
      }
      const due = dueDate(ref.account, t.date);
      if (due < bounds.from || due > bounds.to) return;

      const key = String(t.cardId) + '|' + due;
      if (!invoices[key]) {
        invoices[key] = {
          id: 'invoice:' + t.cardId + ':' + due,
          invoice: true,
          cardId: t.cardId,
          accountId: ref.account.id,
          date: due,
          description: label(ref.account, ref.card),
          amount: 0,
          type: 'expense',
          status: due <= today() ? 'confirmed' : 'scheduled',
          categoryId: null,
        };
        rows.push(invoices[key]);
      }
      invoices[key].amount += (+t.amount || 0);
    });

    // Fatura que soma crédito líquido (estorno maior que a compra) deixa de ser despesa.
    Object.keys(invoices).forEach(function (k) {
      if (invoices[k].amount > 0) invoices[k].type = 'income';
    });
    return rows;
  }

  window.Domain = window.Domain || {};
  window.Domain.Invoice = {
    DEFAULT_CLOSING_DAY: DEFAULT_CLOSING_DAY,
    DEFAULT_DUE_DAY:     DEFAULT_DUE_DAY,
    dueDate:             dueDate,
    cycleFor:            cycleFor,
    dueDatesIn:          dueDatesIn,
    fetchWindow:         fetchWindow,
    label:               label,
    collapse:            collapse,
  };
})();
