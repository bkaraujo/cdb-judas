import $ from 'jquery';
import {esc} from '@/core/kernel/_0_domain/format.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';

let uid = 0;
function nextId(): string {
  return 'ui-' + ++uid;
}

export interface ModalOptions {
  title?: string;
  body?: string | JQuery;
  footer?: string | JQuery;
  persistent?: boolean;
  locked?: boolean;
  onClose?: () => void;
}

export interface Modal {
  open(): void;
  close(): void;
  $el: JQuery;
  $body: JQuery;
}

/** Returns { open, close, $el, $body }. Stacks: each modal has its own overlay. Persistent by
 * default: no overlay-click close and no header X. Pass `opts.persistent === false` to re-enable
 * overlay click + header X. ESC always cancels the operation and closes the top-most modal,
 * regardless of `persistent`. Pass `opts.locked === true` to opt out of ESC too (e.g. the
 * auth/login gate that must not be dismissable). */
export function modal(opts: ModalOptions = {}): Modal {
  const id = nextId();
  const persistent = opts.persistent !== false;
  const locked = opts.locked === true;
  const titleHtml = '<h3>' + esc(opts.title || '') + '</h3>';
  const bodyHtml = opts.body || '';
  const footerContent = opts.footer;
  const footerHtml = opts.footer ? '<div class="modal-footer"></div>' : '';
  const closeBtnHtml = persistent ? '' : '<button class="icon-btn" data-modal-close="1">' + icon('x', 18) + '</button>';
  let overlayClass = persistent ? 'modal-overlay modal-persistent' : 'modal-overlay';
  if (locked) overlayClass += ' modal-locked';
  const $overlay = $(
    '<div class="' + overlayClass + '" data-modal="' + id + '">' +
      '<div class="modal-box fade-in">' +
        '<div class="modal-header">' + titleHtml + closeBtnHtml + '</div>' +
        '<div class="modal-body"></div>' +
        footerHtml +
      '</div>' +
    '</div>',
  );
  const $body = $overlay.find('.modal-body');
  if (typeof bodyHtml === 'string') $body.html(bodyHtml);
  else if (bodyHtml) $body.append(bodyHtml);
  const $footer = $overlay.find('.modal-footer');
  if ($footer.length) {
    if (typeof footerContent === 'string') $footer.html(footerContent);
    else if (footerContent) $footer.append(footerContent);
  }

  function close(): void {
    $overlay.remove();
    if (opts.onClose) opts.onClose();
  }
  function open(): void {
    $('body').append($overlay);
  }
  if (!persistent) {
    $overlay.on('click', (e) => {
      if (e.target === $overlay[0]) close();
    });
  }
  $overlay.on('click', '[data-modal-close]', close);
  $overlay.on('modal:dismiss', close);

  return { open, close, $el: $overlay, $body };
}

/** ESC cancels the operation and closes the top-most modal (skips locked). Chamar uma vez (via
 * `initUi()`, Fase 7) — registra um único listener delegado no document. */
export function installModalEscHandler(): void {
  $(document).on('keydown', (e) => {
    if (e.key !== 'Escape') return;
    const $top = $('.modal-overlay').not('.modal-locked').last();
    if ($top.length) $top.trigger('modal:dismiss');
  });
}
