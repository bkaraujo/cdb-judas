import {describe, expect, it} from 'vitest';
import {periodNavFor} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';

function harness(oneBased: boolean, month: number) {
  const state = { month, year: 2026 };
  const sets: [number, number][] = [];
  let reloads = 0;
  const $nav = periodNavFor(state, {
    oneBased,
    periodService: { set: (m, y) => sets.push([m, y]) },
    onChange: () => { reloads += 1; },
  });
  return { state, sets, $nav, reloads: () => reloads };
}

describe('periodNavFor', () => {
  it('0-based: exibe month+1 e grava month-1 no onChange', () => {
    const h = harness(false, 7); // agosto, 0-based
    expect(h.$nav.find('select').val()).toBe('8');
    h.$nav.find('select').val('1').trigger('change');
    expect(h.state.month).toBe(0);
    expect(h.sets.at(-1)).toEqual([1, 2026]);
    expect(h.reloads()).toBe(1);
  });

  it('1-based: exibe e grava o mesmo valor', () => {
    const h = harness(true, 8);
    expect(h.$nav.find('select').val()).toBe('8');
    h.$nav.find('select').val('1').trigger('change');
    expect(h.state.month).toBe(1);
  });

  it('avança e retrocede virando o ano (1-based)', () => {
    const h = harness(true, 12);
    h.$nav.find('[data-act=next]').trigger('click');
    expect(h.state).toEqual({ month: 1, year: 2027 });
  });

  it('recarrega a cada navegação', () => {
    const h = harness(false, 0);
    h.$nav.find('[data-act=prev]').trigger('click');
    expect(h.reloads()).toBe(1);
    expect(h.state).toEqual({ month: 11, year: 2025 });
  });
});
