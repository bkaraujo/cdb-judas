/* _1_domain/account.js — Account entity + rules. Pure. */
(function () {
  const TYPES = {
    CHECKING:    'CHECKING',
    SAVINGS:     'SAVINGS',
    INVESTMENT:  'INVESTMENT',
    CREDIT_CARD: 'CREDIT_CARD',
  };

  function normalize(raw) {
    if (!raw) return null;
    const type = String(raw.type || 'CHECKING').toUpperCase();
    return {
      id:       raw.id,
      name:     raw.name || '',
      type:     type,
      balance:  +raw.balance || 0,
      color:    raw.color || null,
      active:   raw.active !== false,
      additionalInfo: raw.additionalInfo || {},
    };
  }

  function isCreditCard(a) { return !!a && a.type === TYPES.CREDIT_CARD; }
  function isCash(a)       { return !!a && a.type !== TYPES.CREDIT_CARD; }
  function isLiability(a)  { return isCreditCard(a); }

  /* Available limit for a credit card = limit - absolute balance owed.
     Returns 0 if not a credit card or limit missing. */
  function availableLimit(a) {
    if (!isCreditCard(a)) return 0;
    const limit = +((a.additionalInfo && a.additionalInfo.limit) || 0);
    const used  = Math.abs(+a.balance || 0);
    return Math.max(0, limit - used);
  }

  /* Display value: balance for cash; available limit for credit card. */
  function displayBalance(a) {
    return isCreditCard(a) ? availableLimit(a) : (+a.balance || 0);
  }

  function balanceSheetSide(a) {
    return isLiability(a) ? 'PASSIVO' : 'ATIVO';
  }

  /* Cards may only link to CHECKING accounts; exclude self. */
  function linkableCheckings(all, excludeId) {
    return (all || []).filter(function (a) {
      return a.type === TYPES.CHECKING && String(a.id) !== String(excludeId);
    });
  }

  window.Domain = window.Domain || {};
  window.Domain.Account = {
    TYPES: TYPES,
    normalize: normalize,
    isCreditCard: isCreditCard,
    isCash: isCash,
    isLiability: isLiability,
    availableLimit: availableLimit,
    displayBalance: displayBalance,
    balanceSheetSide: balanceSheetSide,
    linkableCheckings: linkableCheckings,
  };
})();
