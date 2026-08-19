import type { Tag as TagWire } from '@/api/types.ts';

/** Tag entity. Pure. */
export interface Tag {
  id: string;
  name: string;
  color: string | null;
}

export function normalize(raw: TagWire | null | undefined): Tag | null {
  if (!raw || raw.id == null) return null;
  return {
    id: raw.id,
    name: raw.name || '',
    color: raw.color || null,
  };
}

export function hasColor(t: Tag | null | undefined): boolean {
  return !!(t && t.color);
}

/** Resolve ids → tags do catálogo, na ordem do catálogo. Id órfão é descartado: apagar uma tag
 * com strategy=DETACH desfaz o vínculo no servidor, mas o `tagIds` já carregado numa tela aberta
 * continua citando o id até a próxima busca — resolver contra o catálogo é o que impede um
 * indicador de tags apontando para nada. */
export function resolve(tagIds: readonly string[] | null | undefined, catalog: readonly Tag[] | null | undefined): Tag[] {
  if (!Array.isArray(tagIds) || tagIds.length === 0) return [];
  const wanted: Record<string, boolean> = {};
  tagIds.forEach((id) => {
    wanted[String(id)] = true;
  });
  return (catalog || []).filter((t) => t && wanted[String(t.id)]);
}
