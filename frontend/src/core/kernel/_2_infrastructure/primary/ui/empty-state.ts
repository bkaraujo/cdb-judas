import {esc} from '@/core/kernel/_0_domain/format.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';

export interface EmptyStateOptions {
  icon?: string;
  title?: string;
  desc?: string;
}

export function emptyState(opts: EmptyStateOptions = {}): string {
  const iconName = opts.icon || 'activity';
  const title = opts.title || '';
  const desc = opts.desc || '';
  return (
    '<div class="empty-state">' +
    '<div class="empty-state-icon">' + icon(iconName, 24) + '</div>' +
    '<div>' +
    '<p class="empty-state-title">' + esc(title) + '</p>' +
    (desc ? '<p class="empty-state-desc">' + esc(desc) + '</p>' : '') +
    '</div>' +
    '</div>'
  );
}
