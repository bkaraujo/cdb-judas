import {esc} from '@/core/kernel/_0_domain/format.ts';

export function badge(text: string, color?: string): string {
  const c = color || 'accent';
  return '<span class="badge badge-' + esc(c) + '">' + esc(text) + '</span>';
}
