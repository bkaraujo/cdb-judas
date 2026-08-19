import { describe, expect, it } from 'vitest';
import * as Period from '@/core/kernel/_0_domain/period.ts';

describe('kernel:period', () => {
  it('create/shift navegam meses, virando o ano nas pontas', () => {
    const p = Period.create(3, 2026);
    expect(Period.shift(p, 1)).toEqual({ month: 4, year: 2026 });
    expect(Period.shift(Period.create(12, 2026), 1)).toEqual({ month: 1, year: 2027 });
    expect(Period.shift(Period.create(1, 2026), -1)).toEqual({ month: 12, year: 2025 });
  });

  it('bounds devolve o primeiro e o último dia do mês (yyyy-mm-dd)', () => {
    expect(Period.bounds(Period.create(2, 2028))).toEqual({ from: '2028-02-01', to: '2028-02-29' });
    expect(Period.bounds(Period.create(2, 2026))).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(Period.bounds(Period.create(4, 2026))).toEqual({ from: '2026-04-01', to: '2026-04-30' });
  });

  it('yyyymm / yyyyDashMm formatam mês com zero à esquerda', () => {
    expect(Period.yyyymm(Period.create(3, 2026))).toBe('202603');
    expect(Period.yyyyDashMm(Period.create(3, 2026))).toBe('2026-03');
  });

  it('fromYyyyDashMm faz o parse inverso de yyyyDashMm', () => {
    expect(Period.fromYyyyDashMm('2026-03')).toEqual({ month: 3, year: 2026 });
    expect(Period.fromYyyyDashMm(null)).toBe(null);
    expect(Period.fromYyyyDashMm('bogus')).toBe(null);
  });

  it('containsDate compara ano e mês, ignorando o dia', () => {
    const p = Period.create(3, 2026);
    // Meio do mês, sem risco de borda de fuso (containsDate faz `new Date(dateLike)`, que
    // parseia string ISO como meia-noite UTC — dias 1 ou 31 poderiam rolar de mês em fuso
    // negativo; dia 15 nunca corre esse risco).
    expect(Period.containsDate(p, '2026-03-15')).toBe(true);
    expect(Period.containsDate(p, '2026-04-15')).toBe(false);
    expect(Period.containsDate(p, '2025-03-15')).toBe(false);
  });

  it('equals compara mês e ano', () => {
    expect(Period.equals(Period.create(3, 2026), Period.create(3, 2026))).toBe(true);
    expect(Period.equals(Period.create(3, 2026), Period.create(4, 2026))).toBe(false);
    expect(Period.equals(null, Period.create(3, 2026))).toBeFalsy();
  });
});
