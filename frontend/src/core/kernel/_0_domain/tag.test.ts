import { describe, expect, it } from 'vitest';
import * as Tag from './tag.ts';

describe('kernel:tag', () => {
  it('normalize aplica defaults', () => {
    const t = Tag.normalize({ id: '1', name: 'Viagem' });
    expect(t?.name).toBe('Viagem');
    expect(t?.color).toBe(null);
    expect(Tag.normalize(null)).toBe(null);
  });

  it('hasColor', () => {
    expect(Tag.hasColor({ id: '1', name: 'A', color: '#fff' })).toBe(true);
    expect(Tag.hasColor({ id: '1', name: 'A', color: null })).toBe(false);
    expect(Tag.hasColor(null)).toBe(false);
  });

  it('resolve mantém a ordem do catálogo e descarta ids órfãos', () => {
    const catalog: Tag.Tag[] = [
      { id: '1', name: 'A', color: null },
      { id: '2', name: 'B', color: null },
      { id: '3', name: 'C', color: null },
    ];
    const out = Tag.resolve(['3', '1', '999'], catalog);
    expect(out.map((t) => t.id)).toEqual(['1', '3']);
  });

  it('resolve com entrada vazia/inválida devolve array vazio', () => {
    expect(Tag.resolve([], [])).toEqual([]);
    expect(Tag.resolve(null, [])).toEqual([]);
  });
});
