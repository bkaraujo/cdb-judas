/* _1_domain/money.js — currency value helpers (facade over format.js). Pure. */
(function () {
  function format(v)      { return window.fmt(v); }
  function formatShort(v) { return window.fmtShort(v); }
  function parse(s)       { return window.parseCurrency(s); }
  function colorBySign(v) { return window.valueColor(v); }
  function isPositive(v)  { return (+v || 0) > 0; }
  function isNegative(v)  { return (+v || 0) < 0; }
  function abs(v)         { return Math.abs(+v || 0); }

  window.Domain = window.Domain || {};
  window.Domain.Money = {
    format: format,
    formatShort: formatShort,
    parse: parse,
    colorBySign: colorBySign,
    isPositive: isPositive,
    isNegative: isNegative,
    abs: abs,
  };
})();
