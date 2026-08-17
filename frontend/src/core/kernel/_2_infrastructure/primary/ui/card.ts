import $ from 'jquery';
import { esc } from '../../../_0_domain/format.ts';
import { icon } from '../icons.ts';

export interface CardOptions {
  style?: string;
}

export function card(content: string | JQuery | null | undefined, opts: CardOptions = {}): JQuery {
  const $el = $('<div class="card"></div>');
  if (opts.style) $el.attr('style', opts.style);
  if (typeof content === 'string') $el.html(content);
  else if (content) $el.append(content);
  return $el;
}

export interface StatCardOptions {
  color?: string;
  icon?: string;
  label?: string;
  value?: string | number | null;
  sub?: string;
  bare?: boolean;
}

/** The label/value(/sub) block repeated by budget's summary cards, transactions' summary row,
 * and accounts-payable's 3-col divided card. `opts.icon` switches to the icon-chip card layout
 * (transactions); `opts.bare` skips the .card wrapper entirely, returning just the inner block
 * (accounts-payable composes 3 of these inside one shared card, divided by vertical rules). */
export function statCardHtml(opts: StatCardOptions = {}): string {
  const color = opts.color || 'var(--text-primary)';
  const hasIcon = !!opts.icon;
  const labelHtml = '<p class="stat-card-label' + (hasIcon ? ' stat-card-label--icon' : '') + '">' + esc(opts.label || '') + '</p>';
  const valueHtml =
    '<p class="stat-card-value' + (hasIcon ? ' stat-card-value--icon' : '') + '" style="color:' + color + ';">' +
    esc(opts.value != null ? opts.value : '') +
    '</p>';
  const subHtml = opts.sub ? '<p class="stat-card-sub">' + esc(opts.sub) + '</p>' : '';
  const body = '<div class="stat-card-body' + (hasIcon ? '' : ' stat-card-body--flex') + '">' + labelHtml + valueHtml + subHtml + '</div>';
  if (opts.bare) return body;
  if (hasIcon) {
    return (
      '<div class="card stat-card stat-card--icon">' +
      '<span style="color:' + color + ';display:flex;">' + icon(opts.icon as string, 20) + '</span>' + body +
      '</div>'
    );
  }
  return '<div class="card stat-card">' + body + '</div>';
}
