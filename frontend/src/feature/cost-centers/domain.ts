/** CostCenter entity. Pure. */
import type { CostCenter as CostCenterWire } from '../../api/types.ts';

export interface CostCenter {
  id: string;
  name: string;
  description: string;
  color: string | null;
}

/** `CostCenterWire` (gerado) só declara `id`/`description` — `name`/`color` nunca chegam assim no
 * payload real, mas não estão na lista de remoções pré-aprovadas do plano, e o teste portado de
 * `web/test/feature/cost-centers.test.js` cobre `name` explicitamente — mantidos como campos
 * defensivos além do tipo gerado (mesmo caso de `account.ts`, Fase 3). */
type CostCenterWireExt = CostCenterWire & { name?: string; color?: string };

export function normalize(raw: CostCenterWireExt | null | undefined): CostCenter | null {
  if (!raw || raw.id == null) return null;
  return {
    id: raw.id,
    name: raw.name || '',
    description: raw.description || '',
    color: raw.color || null,
  };
}

/** Show secondary line only when description differs from the name. */
export function displaySubtitle(c: { name?: string; description?: string } | null | undefined): string {
  if (!c) return '';
  if (c.name && c.description && c.name !== c.description) return c.description;
  return '';
}
