import $ from 'jquery';
import { esc } from '../../../_0_domain/format.ts';
import { icon } from '../icons.ts';

export interface BtnOptions {
  variant?: string;
  size?: 'sm' | 'md';
  icon?: string;
  label?: string;
  attrs?: string;
  disabled?: boolean;
  onClick?: (e: JQuery.ClickEvent) => void;
}

export function btn(opts: BtnOptions = {}): JQuery {
  const variant = opts.variant || 'primary';
  const size = opts.size || 'md';
  const iconHtml = opts.icon ? icon(opts.icon, size === 'sm' ? 13 : 15) : '';
  const label = opts.label != null ? esc(opts.label) : '';
  const attrs = opts.attrs || '';
  const disabled = opts.disabled ? ' disabled' : '';
  const html =
    '<button class="btn btn-' + variant + ' btn-' + size + '"' + disabled + ' ' + attrs + '>' +
    iconHtml + (label ? '<span>' + label + '</span>' : '') +
    '</button>';
  const $el = $(html);
  if (opts.onClick) $el.on('click', opts.onClick);
  return $el;
}

export interface IconBtnOptions {
  size?: number;
  title?: string;
  onClick?: (e: JQuery.ClickEvent) => void;
}

export function iconBtn(name: string, opts: IconBtnOptions = {}): JQuery {
  const size = opts.size || 18;
  const $el = $('<button class="icon-btn" type="button">' + icon(name, size) + '</button>');
  if (opts.title) $el.attr('title', opts.title);
  if (opts.onClick) $el.on('click', opts.onClick);
  return $el;
}

/* Tamanho vira classe fixa nos dois casos comuns (28px = a maioria das linhas, 22px = chip de
 * tag); qualquer outro valor é genuinamente dinâmico e continua inline. */
function iconBtnSizeClass(size: number): string {
  if (size === 28) return ' icon-btn-sm';
  if (size === 22) return ' chip-btn';
  return '';
}
function iconBtnSizeStyle(size: number, cls: string): string {
  return cls ? '' : 'width:' + size + 'px;height:' + size + 'px;';
}

export interface RowActionBtnOptions {
  danger?: boolean;
  size?: number;
  iconSize?: number;
  act?: string;
}

/** `opts` accepts { danger, size, iconSize, act } — `act` defaults to `iconName` (data-act), but
 * some rows dispatch a different action than the icon shown (e.g. icon 'eye' / act 'reactivate').
 * A plain boolean 4th arg is still accepted as shorthand for { danger: bool } (existing callers). */
export function rowActionBtn(iconName: string, title: string, id: string, opts?: RowActionBtnOptions | boolean | null): JQuery {
  const o: RowActionBtnOptions = opts === true || opts === false || opts == null ? { danger: !!opts } : opts;
  const size = o.size || 28;
  const iconSize = o.iconSize || 14;
  const sizeCls = iconBtnSizeClass(size);
  const sizeStyle = iconBtnSizeStyle(size, sizeCls);
  const cls = 'icon-btn' + sizeCls + (o.danger ? ' is-danger' : '');
  return $(
    '<button type="button" class="' + cls + '" title="' + esc(title) + '" ' +
      'data-act="' + esc(o.act || iconName) + '" data-id="' + esc(id) + '"' +
      (sizeStyle ? ' style="' + sizeStyle + '"' : '') + '>' +
      icon(iconName, iconSize) +
    '</button>',
  );
}

export interface RowActionsHtmlOptions {
  size?: number;
  iconSize?: number;
  extra?: string;
  edit?: boolean;
  trash?: boolean;
}

// String twin of rowActionBtn, for rows built as HTML strings (statement/transactions rows)
// rather than jQuery chains. `extra` is raw HTML prepended before edit/trash (e.g. a
// mark-paid button that only shows for some rows).
export function rowActionsHtml(id: string, opts: RowActionsHtmlOptions = {}): string {
  function btnHtml(iconName: string, title: string, act: string, danger: boolean): string {
    const size = opts.size || 28;
    const iconSize = opts.iconSize || 14;
    const sizeCls = iconBtnSizeClass(size);
    const sizeStyle = iconBtnSizeStyle(size, sizeCls);
    const cls = 'icon-btn' + sizeCls + (danger ? ' is-danger' : '');
    return (
      '<button type="button" class="' + cls + '" title="' + esc(title) + '" ' +
      'data-act="' + esc(act) + '" data-id="' + esc(id) + '"' +
      (sizeStyle ? ' style="' + sizeStyle + '"' : '') + '>' +
      icon(iconName, iconSize) +
      '</button>'
    );
  }
  return (
    (opts.extra || '') +
    (opts.edit !== false ? btnHtml('edit', 'Editar', 'edit', false) : '') +
    (opts.trash !== false ? btnHtml('trash', 'Excluir', 'trash', true) : '')
  );
}
