/** Month/year value object. Pure. */
export interface Period {
  month: number;
  year: number;
}

export interface PeriodBounds {
  from: string;
  to: string;
}

function pad2(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}

export function create(month: number, year: number): Period {
  return { month, year };
}

export function currentMonth(): Period {
  const d = new Date();
  return create(d.getMonth() + 1, d.getFullYear());
}

export function shift(p: Period, n: number): Period {
  const d = new Date(p.year, p.month - 1 + n, 1);
  return create(d.getMonth() + 1, d.getFullYear());
}

export function bounds(p: Period): PeriodBounds {
  const last = new Date(p.year, p.month, 0).getDate();
  return {
    from: p.year + '-' + pad2(p.month) + '-01',
    to: p.year + '-' + pad2(p.month) + '-' + pad2(last),
  };
}

/** "yyyyMM" — the period key used by the monthly-balance endpoint. */
export function yyyymm(p: Period): string {
  return '' + p.year + pad2(p.month);
}

export function containsDate(p: Period, dateLike: string | Date): boolean {
  const d = dateLike instanceof Date ? dateLike : new Date(dateLike);
  return d.getFullYear() === p.year && d.getMonth() + 1 === p.month;
}

export function equals(a: Period | null | undefined, b: Period | null | undefined): boolean {
  return !!a && !!b && a.month === b.month && a.year === b.year;
}

/** "YYYY-MM" — the closing-period format used by the accounting-closing endpoint. Distinct from
 * yyyymm() above, which is the balance-endpoint format ("yyyyMM"). */
export function yyyyDashMm(p: Period): string {
  return p.year + '-' + pad2(p.month);
}

export function fromYyyyDashMm(s: string | null | undefined): Period | null {
  if (!s) return null;
  const parts = s.split('-');
  if (parts.length !== 2) return null;
  return create(parseInt(parts[1] as string, 10), parseInt(parts[0] as string, 10));
}
