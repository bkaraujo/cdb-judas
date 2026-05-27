/* _3_infrastructure/primary/format.js — value/date formatters + html escape. */

window.pad2 = function (n) {
  return n < 10 ? '0' + n : '' + n;
};

// Expects month to be 1-based (1-12)
window.monthLabel = function (month, year) {
  const d = new Date(year, month - 1, 1);
  const s = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(d);
  return s.charAt(0).toUpperCase() + s.slice(1);
};

window.sortByName = function (a, b) {
  return String(a.name || '').toLowerCase()
    .localeCompare(String(b.name || '').toLowerCase(), 'pt-BR');
};

/* Always returns ABSOLUTE value formatted as BR currency (no minus sign).
   Use valueColor(v) / fmtSigned(v) to convey sign via color. */
window.fmt = function (v) {
  const n = Math.abs(+v || 0);
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(n);
};

window.fmtShort = function (v) {
  const abs = Math.abs(+v || 0);
  if (abs >= 1000) {
    return 'R$ ' + (abs / 1000).toFixed(1).replace('.', ',') + 'k';
  }
  return window.fmt(abs);
};

/* Returns CSS color token for a numeric value: green if >=0, red if <0. */
window.valueColor = function (v) {
  return (+v || 0) < 0 ? 'var(--expense)' : 'var(--income)';
};

/* Returns ready-to-inject HTML span: absolute value, colored by sign. */
window.fmtSigned = function (v) {
  return '<span style="color:' + window.valueColor(v) + ';">' + window.fmt(v) + '</span>';
};

window.fmtDate = function (d) {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(d));
};

window.fmtMonth = function (d) {
  return new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(new Date(d)).replace('.', '');
};

/* Parse "1.234,56" / "R$ 1.234,56" / "1234.56" / number → Number (NaN if invalid). */
window.parseCurrency = function (v) {
  if (typeof v === 'number') return v;
  if (v == null) return NaN;
  let s = String(v).replace(/[^\d,.-]/g, '');
  if (s.indexOf(',') >= 0) s = s.replace(/\./g, '').replace(',', '.');
  return parseFloat(s);
};

/* Mask number / string as "1.234,56" (no R$). Used for currency inputs. */
window.maskCurrency = function (v) {
  let n = window.parseCurrency(v);
  if (!isFinite(n)) n = 0;
  return n.toFixed(2).replace('.', ',').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
};

/* Bind a live BR-currency mask to an <input>. Returns the jQuery element. */
window.bindCurrencyMask = function ($input) {
  return $input.on('input', function () {
    const raw = $(this).val().replace(/\D/g, '');
    if (!raw) { $(this).val(''); return; }
    const n = parseInt(raw, 10) / 100;
    $(this).val(n.toFixed(2).replace('.', ',').replace(/\B(?=(\d{3})+(?!\d))/g, '.'));
  });
};

/* HTML-escape helper. Always wrap interpolated values from API in esc(). */
window.esc = function (s) {
  if (s === null || s === undefined) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};
