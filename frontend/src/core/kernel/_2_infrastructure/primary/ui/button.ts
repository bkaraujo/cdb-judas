import $ from 'jquery';
import {esc} from '@/core/kernel/_0_domain/format.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';

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

/** Construtor único da string do botão de ação de linha. `rowActionBtn` embrulha em jQuery;
 * `rowActionsHtml` concatena — antes os dois mantinham a mesma marcação digitada duas vezes. */
function rowActionBtnHtml(iconName: string, title: string, id: string, act: string, danger: boolean, size: number, iconSize: number): string {
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

/** `opts` accepts { danger, size, iconSize, act } — `act` defaults to `iconName` (data-act), but
 * some rows dispatch a different action than the icon shown (e.g. icon 'eye' / act 'reactivate').
 * A plain boolean 4th arg is still accepted as shorthand for { danger: bool } (existing callers). */
export function rowActionBtn(iconName: string, title: string, id: string, opts?: RowActionBtnOptions | boolean | null): JQuery {
  const o: RowActionBtnOptions = opts === true || opts === false || opts == null ? { danger: !!opts } : opts;
  return $(rowActionBtnHtml(iconName, title, id, o.act || iconName, !!o.danger, o.size || 28, o.iconSize || 14));
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
  const size = opts.size || 28;
  const iconSize = opts.iconSize || 14;
  return (
    (opts.extra || '') +
    (opts.edit !== false ? rowActionBtnHtml('edit', 'Editar', id, 'edit', false, size, iconSize) : '') +
    (opts.trash !== false ? rowActionBtnHtml('trash', 'Excluir', id, 'trash', true, size, iconSize) : '')
  );
}
