/** Matches a raw transaction description against the person's cached import rules (regras de
 * nomenclatura). Pure, used identically in 3 places: the manual transaction form, the
 * bank-statement import preview and the invoice import preview. */

/** Uppercase + strip diacritics, so "Agua" matches "AGUA" — same comparison the backend applies
 * when rejecting ambiguous rules (ImportRuleService.normalize), kept in sync by hand since the
 * two runtimes can't share code. ̀-ͯ is the Unicode combining-diacritical-marks block
 * that NFD decomposition splits accents into. */
export function normalize(s: string | null | undefined): string {
  return (s || '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toUpperCase()
    .trim();
}

export interface ImportRuleLike {
  name?: string | null;
  triggers?: readonly string[] | null;
}

/** First rule (array order) with any normalized trigger that's a substring of the normalized
 * description, or null. Rules ambiguous with each other are already rejected at creation time
 * (server-side, trigger×trigger); independent rules that both happen to match the same description
 * resolve to whichever comes first in `rules`. */
export function match<T extends ImportRuleLike>(description: string | null | undefined, rules: readonly T[] | null | undefined): T | null {
  if (!description || !rules || !rules.length) return null;
  const normalizedDescription = normalize(description);
  for (let i = 0; i < rules.length; i++) {
    const rule = rules[i];
    if (!rule || !rule.triggers || !rule.triggers.length) continue;
    for (let j = 0; j < rule.triggers.length; j++) {
      const trigger = rule.triggers[j];
      if (!trigger) continue;
      if (normalizedDescription.indexOf(normalize(trigger)) >= 0) return rule;
    }
  }
  return null;
}
