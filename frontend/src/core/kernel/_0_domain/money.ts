/** Currency value helpers (facade over format.ts). Pure. */
import {fmt, fmtShort, parseCurrency, valueColor} from '@/core/kernel/_0_domain/format.ts';

export function format(v: number | string | null | undefined): string {
  return fmt(v);
}
export function formatShort(v: number | string | null | undefined): string {
  return fmtShort(v);
}
export function parse(s: number | string | null | undefined): number {
  return parseCurrency(s);
}
export function colorBySign(v: number | string | null | undefined): string {
  return valueColor(v);
}
export function isPositive(v: number | string | null | undefined): boolean {
  return (+(v ?? 0) || 0) > 0;
}
export function isNegative(v: number | string | null | undefined): boolean {
  return (+(v ?? 0) || 0) < 0;
}
export function abs(v: number | string | null | undefined): number {
  return Math.abs(+(v ?? 0) || 0);
}
