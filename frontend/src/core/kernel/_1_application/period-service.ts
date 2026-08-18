/**
 * Shared period (month/year) filter.
 *
 * Local-only, session-scoped state (like "última tela"): the period the user last navigated to is
 * kept across screens so switching pages preserves the filter. Canonical convention matches
 * Domain.Period: month is 1-12. Mirrored in sessionStorage so it also survives a reload within the
 * tab; a fresh session defaults to the current month.
 */
import * as Period from '../_0_domain/period.ts';
import type { Storage } from '../_2_infrastructure/secondary/storage.ts';

const KEY = 'cdb-period';

export interface PeriodService {
  /** Returns { month: 1-12, year }. */
  get(): Period.Period;
  /** Persist the navigated period. month is 1-12. */
  set(month: number, year: number): Period.Period | null;
}

export function createPeriodService(storage: Storage): PeriodService {
  let current: Period.Period | null = null;

  function persist(p: Period.Period): void {
    storage.session.set(KEY, p.year + '-' + p.month);
  }

  function restore(): Period.Period | null {
    const raw = storage.session.get(KEY);
    if (!raw) return null;
    const parts = raw.split('-');
    const year = parseInt(parts[0] as string, 10);
    const month = parseInt(parts[1] as string, 10);
    if (!(month >= 1 && month <= 12) || !(year >= 1900 && year <= 9999)) return null;
    return Period.create(month, year);
  }

  function ensure(): Period.Period {
    if (current) return current;
    current = restore() || Period.currentMonth();
    return current;
  }

  return {
    get: ensure,
    set(month, year) {
      if (!(month >= 1 && month <= 12) || !(year >= 1900 && year <= 9999)) return current;
      current = Period.create(month, year);
      persist(current);
      return current;
    },
  };
}
