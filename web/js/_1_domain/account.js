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
      currentBalance: raw.currentBalance != null ? (+raw.currentBalance || 0) : (+raw.balance || 0),
      color:    raw.color || null,
      active:   raw.active !== false,
      linkedAccountId: raw.linkedAccountId || null,
      last4:    raw.last4 || null,
      dueDay:   raw.dueDay != null ? +raw.dueDay : null,
      closingDay: raw.closingDay != null ? +raw.closingDay : null,
      creditLimit: raw.creditLimit != null ? (+raw.creditLimit || 0) : 0,
      overdraftLimit: raw.overdraftLimit != null ? (+raw.overdraftLimit || 0) : 0,
    };
  }

  function isCreditCard(a) { return !!a && a.type === TYPES.CREDIT_CARD; }
  function isCash(a)       { return !!a && a.type !== TYPES.CREDIT_CARD; }
  function isLiability(a)  { return isCreditCard(a); }

  /* Current balance = opening balance + every transaction (derived backend-side).
     Falls back to the opening balance for payloads without `currentBalance`. */
  function currentBalance(a) {
    if (!a) return 0;
    return a.currentBalance != null ? (+a.currentBalance || 0) : (+a.balance || 0);
  }

  /* Available limit for a credit card = limit - absolute current balance owed.
     Returns 0 if not a credit card or limit missing. */
  function availableLimit(a) {
    if (!isCreditCard(a)) return 0;
    const limit = +(a.creditLimit || 0);
    const used  = Math.abs(currentBalance(a));
    return Math.max(0, limit - used);
  }

  /* Display value: current balance for cash; available limit for credit card. */
  function displayBalance(a) {
    return isCreditCard(a) ? availableLimit(a) : currentBalance(a);
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
    currentBalance: currentBalance,
    availableLimit: availableLimit,
    displayBalance: displayBalance,
    balanceSheetSide: balanceSheetSide,
    linkableCheckings: linkableCheckings,
  };
})();
