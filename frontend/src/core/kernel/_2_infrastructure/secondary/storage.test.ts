import { describe, expect, it } from 'vitest';
import { createStorage, STORAGE_KEYS } from '@/core/kernel/_2_infrastructure/secondary/storage.ts';

describe('createStorage', () => {
  it('faz round-trip de string em local e session', () => {
    const s = createStorage();
    s.local.set('k', 'v');
    s.session.set('k', 'w');
    expect(s.local.get('k')).toBe('v');
    expect(s.session.get('k')).toBe('w');
    s.local.del('k');
    s.session.del('k');
    expect(s.local.get('k')).toBeNull();
    expect(s.session.get('k')).toBeNull();
  });

  it('json devolve null para chave ausente e para JSON inválido', () => {
    const s = createStorage();
    expect(s.local.json('ausente')).toBeNull();
    s.local.set('ruim', '{nao-e-json');
    expect(s.local.json('ruim')).toBeNull();
    s.local.del('ruim');
  });

  it('setJson/json fazem round-trip de objeto', () => {
    const s = createStorage();
    s.local.setJson('obj', { a: 1, b: ['x'] });
    expect(s.local.json('obj')).toEqual({ a: 1, b: ['x'] });
    s.local.del('obj');
  });

  it('expõe as chaves conhecidas', () => {
    expect(createStorage().KEYS).toBe(STORAGE_KEYS);
  });
});
