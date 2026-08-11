/* _1_domain/import-rule-matcher.js — matches a raw transaction description against the person's
 * cached import rules (regras de nomenclatura). Pure, used identically in 3 places: the manual
 * transaction form, the bank-statement import preview and the invoice import preview. */
(function () {
  /* Uppercase + strip diacritics, so "Agua" matches "AGUA" — same comparison the backend applies
   * when rejecting ambiguous rules (ImportRuleService.normalize), kept in sync by hand since the
   * two runtimes can't share code. ̀-ͯ is the Unicode combining-diacritical-marks block
   * that NFD decomposition splits accents into. */
  function normalize(s) {
    return (s || '')
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toUpperCase()
      .trim();
  }

  /* First rule (array order) whose normalized name is a substring of the normalized description,
   * or null. Rules ambiguous with each other are already rejected at creation time (server-side);
   * independent rules that both happen to match the same description resolve to whichever comes
   * first in `rules`. */
  function match(description, rules) {
    if (!description || !rules || !rules.length) return null;
    const normalizedDescription = normalize(description);
    for (let i = 0; i < rules.length; i++) {
      const rule = rules[i];
      if (!rule || !rule.name) continue;
      if (normalizedDescription.indexOf(normalize(rule.name)) >= 0) return rule;
    }
    return null;
  }

  window.Domain = window.Domain || {};
  window.Domain.ImportRuleMatcher = {
    normalize: normalize,
    match: match,
  };
})();
