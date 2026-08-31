/**
 * Uniform "entity has linked transactions" dialog, shown on a 409 LINKED_TRANSACTIONS response
 * (accounts/cards/categories/tags).
 */
import $ from 'jquery';
import {esc} from '@/core/kernel/_0_domain/format.ts';
import {confirmModal, modalFooter, modalText, runMutation} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import {btn} from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import type {Modal} from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import {modal} from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';

export interface DeleteChoice {
  value: string;
  label: string;
}

export interface LinkedDeleteOption {
  value: string;
  label: string;
  hint?: string;
  choices?: DeleteChoice[];
  danger?: boolean;
}

export interface LinkedDeleteDialogOptions {
  title?: string;
  intro?: string;
  options?: LinkedDeleteOption[];
  onConfirm?: (choice: { strategy: string; targetId: string | null }, m: Modal, reEnable: () => void) => void;
}

function optionRowHtml(opt: LinkedDeleteOption, idx: number): string {
  const inputId = 'linked-delete-opt-' + idx;
  const hint = opt.hint ? '<div style="font-size:12px;color:var(--text-secondary);margin-top:2px;">' + esc(opt.hint) + '</div>' : '';
  const selectHtml = opt.choices
    ? '<select data-target-select disabled style="margin-top:6px;width:100%;" class="input">' +
      opt.choices.map((c) => '<option value="' + esc(c.value) + '">' + esc(c.label) + '</option>').join('') +
      '</select>'
    : '';
  return (
    '<label for="' + inputId + '" data-option-row data-value="' + esc(opt.value) + '" ' +
      'style="display:block;padding:10px 12px;border:1px solid var(--border);border-radius:8px;margin-bottom:8px;cursor:pointer;">' +
      '<div style="display:flex;gap:8px;align-items:flex-start;">' +
        '<input type="radio" name="linked-delete-strategy" id="' + inputId + '" value="' + esc(opt.value) + '" ' +
          'style="width:16px;height:16px;flex-shrink:0;margin-top:3px;accent-color:var(--accent);">' +
        '<div style="flex:1;">' +
          '<div style="font-weight:500;">' + esc(opt.label) + '</div>' + hint + selectHtml +
        '</div>' +
      '</div>' +
    '</label>'
  );
}

export function linkedDeleteDialog(opts: LinkedDeleteDialogOptions = {}): Modal {
  const visibleOptions = (opts.options || []).filter((o) => !o.choices || o.choices.length > 0);

  const bodyHtml = (opts.intro ? modalText(opts.intro) : '') + '<div data-region="strategy-options">' + visibleOptions.map(optionRowHtml).join('') + '</div>';

  const $cancel = btn({
    variant: 'secondary', size: 'md', label: 'Cancelar',
    attrs: 'data-modal-close="1" type="button"',
  });
  const $confirm = btn({
    variant: 'secondary', size: 'md', label: 'Confirmar', disabled: true,
    attrs: 'data-act="linked-delete-confirm" type="button"',
  });

  const m = modal({
    title: opts.title || 'Excluir',
    body: bodyHtml,
    footer: modalFooter([$cancel, $confirm]),
  });
  m.open();

  function optionRow(value: string): JQuery {
    return m.$body.find('[data-option-row][data-value="' + value + '"]');
  }
  function selectedOption(): LinkedDeleteOption | null {
    const value = m.$body.find('input[name="linked-delete-strategy"]:checked').val() as string | undefined;
    return visibleOptions.filter((o) => o.value === value)[0] || null;
  }

  function refreshConfirmState(): void {
    const opt = selectedOption();
    let valid = !!opt;
    if (opt && opt.choices) {
      valid = valid && !!optionRow(opt.value).find('[data-target-select]').val();
    }
    m.$el
      .find('[data-act=linked-delete-confirm]')
      .prop('disabled', !valid)
      .removeClass('btn-danger btn-secondary')
      .addClass(opt && opt.danger ? 'btn-danger' : 'btn-secondary');
  }

  m.$body.on('change', 'input[name="linked-delete-strategy"]', () => {
    m.$body.find('[data-target-select]').prop('disabled', true);
    const opt = selectedOption();
    if (opt && opt.choices) optionRow(opt.value).find('[data-target-select]').prop('disabled', false);
    refreshConfirmState();
  });
  m.$body.on('change', '[data-target-select]', refreshConfirmState);

  m.$el.on('click', '[data-act=linked-delete-confirm]', function () {
    const opt = selectedOption();
    if (!opt) return;
    let targetId: string | null = null;
    if (opt.choices) {
      targetId = (optionRow(opt.value).find('[data-target-select]').val() as string) || null;
      if (!targetId) return;
    }
    const $b = $(this).prop('disabled', true);
    if (typeof opts.onConfirm === 'function') {
      opts.onConfirm({ strategy: opt.value, targetId }, m, function reEnable() {
        $b.prop('disabled', false);
      });
    }
  });

  return m;
}

// 'N transações vinculadas' / '1 transação vinculada' — montado à mão em 5 lugares antes disto.
export function pluralTransactions(count: number): string {
  return count + (count === 1 ? ' transação vinculada' : ' transações vinculadas');
}

interface LinkedFlowOptions<T> {
  title?: string;
  intro?: (count: number) => string;
  options?: (count: number) => LinkedDeleteOption[];
  dispatch: (choice: { strategy: string; targetId: string | null }) => Promise<T>;
  success?: string | ((choice: { strategy: string; targetId: string | null }, result: T) => string | void | null | undefined);
  failure?: string;
}

export interface DeleteWithLinkedFallbackOptions<TRemove, TLinked> {
  title?: string;
  body?: string;
  remove: () => Promise<TRemove>;
  success?: string | ((result: TRemove) => string | void | null | undefined);
  failure?: string;
  onDone?: (result: TRemove | TLinked) => void;
  linked?: LinkedFlowOptions<TLinked>;
}

interface ApiErrorLike {
  status?: number;
  code?: string;
  count?: number;
  message?: string;
}

/** O fluxo completo de confirm→remove→(409 LINKED_TRANSACTIONS?)→linkedDeleteDialog, repetido em
 * accounts/tags/categories. `opts.remove()` é a chamada de exclusão simples (sem opts); em caso
 * de 409 o dialog `opts.linked` assume. Retorna o modal de confirmação (mesmo contrato de
 * confirmModal). */
export function deleteWithLinkedFallback<TRemove, TLinked>(opts: DeleteWithLinkedFallbackOptions<TRemove, TLinked>): Modal {
  return confirmModal({
    title: opts.title,
    body: opts.body,
    onConfirm: (m, reEnable) => {
      runMutation(opts.remove(), {
        modal: m,
        success: opts.success,
        failure: (err) => ((err as ApiErrorLike)?.message) || opts.failure || 'Falha ao excluir',
        onDone: opts.onDone,
        onError: (err) => {
          const e = err as ApiErrorLike;
          if (e && e.status === 409 && e.code === 'LINKED_TRANSACTIONS') {
            m.close();
            openLinkedFallbackDialog(opts.linked, e.count || 0, opts.onDone);
            return true;
          }
          reEnable();
          return false;
        },
      });
    },
  });
}

function openLinkedFallbackDialog<TLinked>(linked: LinkedFlowOptions<TLinked> | undefined, count: number, onDone?: (result: TLinked) => void): Modal {
  const l = linked || ({} as LinkedFlowOptions<TLinked>);
  return linkedDeleteDialog({
    title: l.title,
    intro: l.intro ? l.intro(count) : undefined,
    options: l.options ? l.options(count) : [],
    onConfirm: (choice, m, reEnable) => {
      runMutation(l.dispatch(choice), {
        modal: m,
        success: (result) => (typeof l.success === 'function' ? l.success(choice, result) : l.success),
        failure: (err) => ((err as ApiErrorLike)?.message) || l.failure || 'Falha ao excluir',
        onDone: onDone,
        onError: () => {
          reEnable();
          return false;
        },
      });
    },
  });
}
