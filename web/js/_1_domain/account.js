/* _1_domain/account.js — Account entity + rules. Pure. */
(function () {
  const TYPES = {
    CHECKING:    'CHECKING',
    INVESTMENT:  'INVESTMENT',
  };

  function normalizeCard(raw) {
    if (!raw) return null;
    return {
      id:        raw.id,
      last4:     raw.last4 || '',
      accountId: raw.accountId,
      active:    raw.active !== false,
    };
  }

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
      creditLimit: raw.creditLimit != null ? (+raw.creditLimit || 0) : 0,
      overdraftLimit: raw.overdraftLimit != null ? (+raw.overdraftLimit || 0) : 0,
      closingDay: raw.closingDay != null ? +raw.closingDay : null,
      dueDay:   raw.dueDay != null ? +raw.dueDay : null,
      cards:    Array.isArray(raw.cards) ? raw.cards.map(normalizeCard) : [],
    };
  }

  /* Current balance = opening balance + every transaction (derived backend-side).
     Falls back to the opening balance for payloads without `currentBalance`. */
  function currentBalance(a) {
    if (!a) return 0;
    return a.currentBalance != null ? (+a.currentBalance || 0) : (+a.balance || 0);
  }

  function hasCards(a) { return !!a && Array.isArray(a.cards) && a.cards.length > 0; }

  window.Domain = window.Domain || {};
  window.Domain.Account = {
    TYPES: TYPES,
    normalize: normalize,
    currentBalance: currentBalance,
    hasCards: hasCards,
  };
})();
