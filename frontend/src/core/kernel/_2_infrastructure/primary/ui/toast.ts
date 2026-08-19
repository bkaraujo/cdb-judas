import $ from 'jquery';
import { esc } from '@/core/kernel/_0_domain/format.ts';

export type ToastVariant = 'info' | 'success' | 'error' | 'warning';

export function toast(msg: string, variant?: ToastVariant): void {
  const v = variant || 'info';
  const $t = $('<div class="toast toast-' + esc(v) + '"></div>').text(msg);
  let $stack = $('#toast-stack');
  if (!$stack.length) $stack = $('<div id="toast-stack"></div>').appendTo('body');
  $stack.append($t);
  setTimeout(() => {
    $t.addClass('toast-fade');
  }, 2400);
  setTimeout(() => {
    $t.remove();
  }, 2900);
}
