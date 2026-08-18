import { describe, expect, it } from 'vitest';
import * as CostCenterDomain from './domain.ts';

describe('feature:cost-centers — domain', () => {
  it('normalize aplica defaults', () => {
    const c = CostCenterDomain.normalize({ id: '1', name: 'TI' });
    expect(c?.name).toBe('TI');
    expect(c?.description).toBe('');
    expect(CostCenterDomain.normalize(null)).toBe(null);
  });

  it('displaySubtitle só aparece quando description difere do name', () => {
    expect(CostCenterDomain.displaySubtitle({ name: 'TI', description: 'Tecnologia' })).toBe('Tecnologia');
    expect(CostCenterDomain.displaySubtitle({ name: 'TI', description: 'TI' })).toBe('');
    expect(CostCenterDomain.displaySubtitle(null)).toBe('');
  });
});
