/**
 * Page-level helpers shared across the CRUD page modules. Builds on `ui/` (btn/modal/toast) +
 * `_0_domain/format.ts` (esc) to factor out patterns that were copy-pasted in nearly every page.
 */
import $ from 'jquery';
import { esc } from '../../_0_domain/format.ts';
import { btn } from './ui/button.ts';
import type { Modal } from './ui/modal.ts';
import { modal } from './ui/modal.ts';
import { toast } from './ui/toast.ts';

/* ---- Record lookup ----
 * Replaces the per-page findAccount/findItem/findCard/findTag/findTx helpers. String-compares
 * ids so a numeric record id matches a data-id attribute. */
export function byId<T extends { id: unknown }>(arr: readonly T[] | null | undefined, id: unknown): T | null {
  if (!arr) return null;
  for (let i = 0; i < arr.length; i++) {
    if (String(arr[i]?.id) === String(id)) return arr[i] as T;
  }
  return null;
}

export interface ModalFooterOptions {
  align?: 'end';
}

// A horizontal row of footer buttons. `buttons` is a jQuery element or array of them.
// opts.align === 'end' right-aligns and wraps (grouped-delete style).
export function modalFooter(buttons: JQuery | (JQuery | null | undefined)[], opts: ModalFooterOptions = {}): JQuery {
  const style = opts.align === 'end' ? 'display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;' : 'display:flex;gap:10px;';
  const $footer = $('<div style="' + style + '"></div>');
  ([] as (JQuery | null | undefined)[]).concat(buttons).forEach(($b) => {
    if ($b) $footer.append($b);
  });
  return $footer;
}

export interface SaveCancelFooterOptions {
  cancelLabel?: string;
  saveLabel?: string;
  saveAttrs?: string;
}

// The Cancelar + Salvar footer shared by every create/edit form modal.
export function saveCancelFooter(opts: SaveCancelFooterOptions = {}): JQuery {
  const $cancel = btn({
    variant: 'secondary', size: 'md', label: opts.cancelLabel || 'Cancelar',
    attrs: 'data-modal-close="1" type="button"',
  });
  const $save = btn({
    variant: 'primary', size: 'md', label: opts.saveLabel || 'Salvar',
    attrs: opts.saveAttrs || 'data-act="save" type="submit"',
  });
  return modalFooter([$cancel, $save]);
}

/* ---- Modal body text ----
 * The muted paragraph used in confirm/info modal bodies. `html` may contain inline markup (e.g.
 * <strong>name</strong>) — callers must esc() user data. */
export function modalText(html: string): string {
  return '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' + html + '</p>';
}

export interface ConfirmModalOptions {
  title?: string;
  body?: string;
  confirmLabel?: string;
  confirmIcon?: string | null;
  danger?: boolean;
  cancelLabel?: string;
  onConfirm?: (m: Modal, reEnable: () => void) => void;
}

/** Cancelar + a confirm button (danger/trash by default). Opens immediately and wires the
 * confirm click to disable the button and invoke opts.onConfirm(m, reEnable). The callback owns
 * closing on success (m.close()) and re-enabling on failure (reEnable()), matching the existing
 * delete flows. Returns the modal handle. */
export function confirmModal(opts: ConfirmModalOptions = {}): Modal {
  const $cancel = btn({
    variant: opts.danger === false ? 'ghost' : 'secondary',
    size: 'md', label: opts.cancelLabel || 'Cancelar',
    attrs: 'data-modal-close="1" type="button"',
  });
  const confirmBtn: Parameters<typeof btn>[0] = {
    variant: opts.danger === false ? 'primary' : 'danger',
    size: 'md', label: opts.confirmLabel || 'Excluir',
    attrs: 'data-act="modal-confirm" type="button"',
  };
  if (opts.confirmIcon !== null) confirmBtn.icon = opts.confirmIcon || 'trash';
  const $confirm = btn(confirmBtn);

  const m = modal({
    title: opts.title || 'Confirmar',
    body: opts.body || '',
    footer: modalFooter([$cancel, $confirm]),
  });
  m.open();

  m.$el.on('click', '[data-act=modal-confirm]', function () {
    const $b = $(this).prop('disabled', true);
    if (typeof opts.onConfirm === 'function') {
      opts.onConfirm(m, function reEnable() {
        $b.prop('disabled', false);
      });
    }
  });
  return m;
}

/* ---- Color swatches ----
 * Round color-swatch buttons for a <input type=color> picker. Used by the accounts and tags form
 * modals. swatchesHtml(colors, selectedHex) → HTML string of [data-swatch] buttons. */
export function swatchesHtml(colors: readonly string[], selectedHex: string | null | undefined): string {
  return colors
    .map((c) => {
      const isSel = c.toLowerCase() === String(selectedHex || '').toLowerCase();
      return '<button type="button" class="swatch' + (isSel ? ' is-active' : '') + '" ' + 'data-swatch="' + esc(c) + '" style="background:' + esc(c) + ';" ' + 'title="' + esc(c) + '"></button>';
    })
    .join('');
}

// Wire the swatch grid inside a modal to a color <input>: clicking a swatch sets the input value
// and re-paints borders; editing the color input repaints too. Swatch clicks are ignored while
// the color input is disabled. Returns a paint(hex) fn so callers can repaint after programmatic
// color changes.
export function bindSwatches(m: Modal, $colorInput: JQuery): (activeHex?: string | null) => void {
  function paint(activeHex?: string | null): void {
    m.$body.find('[data-region=swatches] [data-swatch]').each(function () {
      const c = $(this).attr('data-swatch');
      const on = c && c.toLowerCase() === String(activeHex || '').toLowerCase();
      $(this).css('border', on ? '2px solid var(--text-primary)' : '1px solid var(--border)');
    });
  }
  m.$body.on('click', '[data-swatch]', function (e) {
    e.preventDefault();
    if ($colorInput.prop('disabled')) return;
    const c = $(this).attr('data-swatch');
    $colorInput.val(c || '');
    paint(c);
  });
  $colorInput.on('input change', () => paint($colorInput.val() as string));
  return paint;
}

export interface ShiftMonthState {
  month: number;
  year: number;
}

export interface PeriodServicePort {
  set(month: number, year: number): unknown;
}

/* ---- Period navigation ----
 * Advance/rewind a { month, year } state object by `delta` months, handling year roll-over.
 * `oneBased` true → month is 1-12 (credit-cards, statement); false → month is 0-11 (transactions,
 * accounts-payable, budget). `periodService` (opcional): persiste o período navegado — mesmo
 * papel do `window.App.PeriodService` global do original, agora injetado. */
export function shiftMonth(state: ShiftMonthState, delta: number, oneBased: boolean, periodService?: PeriodServicePort): void {
  const base = oneBased ? 1 : 0;
  let m = state.month - base + delta; // normalize to 0-based
  const y = state.year + Math.floor(m / 12);
  m = ((m % 12) + 12) % 12;
  state.month = m + base;
  state.year = y;
  if (periodService) periodService.set(oneBased ? state.month : state.month + 1, state.year);
}

export interface RunMutationOptions<T> {
  $btn?: JQuery;
  modal?: Modal;
  success?: string | ((result: T) => string | void | null | undefined);
  failure?: string | ((err: unknown) => string);
  onDone?: (result: T) => void;
  onError?: (err: unknown) => boolean | void;
}

/** Replaces the disable→then(close+toast)→catch(reEnable+toast) tail repeated ~40x across every
 * create/update/remove call. `success`/`failure` may be a string or a function of the resolved
 * value / error. `onError` runs before the default failure toast and may return truthy to
 * suppress it (e.g. to hand off to a 409 LINKED_TRANSACTIONS dialog instead of toasting a generic
 * failure). */
export function runMutation<T>(promise: Promise<T>, opts: RunMutationOptions<T> = {}): Promise<T | void> {
  if (opts.$btn) opts.$btn.prop('disabled', true);
  return promise
    .then((result) => {
      if (opts.modal) opts.modal.close();
      const msg = typeof opts.success === 'function' ? opts.success(result) : opts.success;
      if (msg) toast(msg, 'success');
      if (opts.onDone) opts.onDone(result);
      return result;
    })
    .catch((err) => {
      if (opts.$btn) opts.$btn.prop('disabled', false);
      if (opts.onError && opts.onError(err)) return;
      const msg = typeof opts.failure === 'function' ? opts.failure(err) : opts.failure || (err && (err as Error).message) || 'Falha ao salvar';
      toast(msg, 'error');
    });
}

export interface FormModalOptions<T> extends SaveCancelFooterOptions {
  title?: string;
  formName: string;
  body?: string | JQuery;
  autofocus?: string;
  onSubmit: ($form: JQuery, m: Modal) => Promise<T> | null | undefined;
  success?: RunMutationOptions<T>['success'];
  failure?: RunMutationOptions<T>['failure'];
  onDone?: RunMutationOptions<T>['onDone'];
  onError?: RunMutationOptions<T>['onError'];
}

/** The skeleton every create/edit form modal repeats: modal({title,body,footer:saveCancelFooter()})
 * → open() → locate the <form>, wire both the form's own submit AND the footer's Salvar button
 * (which lives outside the <form>, in modal-footer). `onSubmit($form, m)` returns a Promise to
 * run through runMutation, or a falsy value to abort (validation failed — onSubmit itself is
 * responsible for focusing the offending field). */
export function formModal<T>(opts: FormModalOptions<T>): Modal {
  const m = modal({
    title: opts.title,
    body: opts.body,
    footer: saveCancelFooter(opts),
  });
  m.open();

  const $form = m.$body.find('form[data-form=' + opts.formName + ']');
  if (opts.autofocus) {
    setTimeout(() => {
      $form.find(opts.autofocus as string).trigger('focus');
    }, 0);
  }

  function submit(e?: JQuery.TriggeredEvent): void {
    if (e) e.preventDefault();
    const result = opts.onSubmit($form, m);
    if (!result) return;
    const $btn = m.$el.find('[data-act=save]');
    runMutation(result, {
      $btn, modal: m,
      success: opts.success, failure: opts.failure,
      onDone: opts.onDone, onError: opts.onError,
    });
  }

  $form.on('submit', submit);
  m.$el.on('click', '[data-act=save]', () => submit());
  return m;
}
