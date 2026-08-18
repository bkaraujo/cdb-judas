/** Ações de linha de lançamento (transfer detection, exclusão com escopo de grupo, confirmar
 * pagamento), compartilhadas entre a tela de Lançamentos e o Extrato de Contas.
 *
 * Todas recebem a lista de transações do período (`list`) para detectar transferências — uma
 * transferência é um par income/expense no mesmo grupo — e um callback de recarga (`onDone`/
 * `onSaved`) que a página passa para atualizar sua própria visão após a mutação.
 */
import { esc } from '../../core/kernel/_0_domain/format.ts';
import * as Transaction from '../../core/kernel/_0_domain/transaction.ts';
import { confirmModal, modalFooter, modalText, runMutation } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { btn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { modal } from '../../core/kernel/_2_infrastructure/primary/ui/modal.ts';
import type { TransactionFormModalOptions } from './form-modal.ts';
import type { TransactionService } from './service.ts';

export interface TxLike {
  id?: string;
  accountId?: string;
  groupId?: string | null;
  type?: string;
  description?: string;
  totalInstallments?: number | null;
  installmentNumber?: number | null;
}

// A transfer is stored as two legs (one income + one expense) sharing a groupId, unlike
// installments whose legs share a single type. Both legs carry the same date, so they sit in the
// same month view together — detection works off the loaded list.
export function isTransfer(tx: TxLike | null | undefined, list: readonly TxLike[] | null | undefined): boolean {
  if (!tx || !tx.groupId) return false;
  const group = (list || []).filter((t) => String(t.groupId) === String(tx.groupId));
  const hasIncome = group.some((t) => t.type === 'income');
  const hasExpense = group.some((t) => t.type === 'expense');
  return hasIncome && hasExpense;
}

export interface TransactionActionsDeps {
  transactionService: TransactionService;
  formModal: (opts: TransactionFormModalOptions) => unknown;
}

export interface OpenFormModalOptions {
  existing?: TxLike | null;
  list?: readonly TxLike[];
  defaultDate?: string | null;
  onSaved?: () => void;
}

export interface OpenDeleteModalOptions {
  list?: readonly TxLike[];
  onDone?: () => void;
}

export interface MarkPaidOptions {
  onDone?: () => void;
}

export interface TransactionActions {
  isTransfer(tx: TxLike | null | undefined, list: readonly TxLike[] | null | undefined): boolean;
  openFormModal(opts: OpenFormModalOptions): void;
  openDeleteModal(tx: TxLike, opts?: OpenDeleteModalOptions): void;
  markPaid(tx: TxLike, opts?: MarkPaidOptions): void;
}

export function createTransactionActions(deps: TransactionActionsDeps): TransactionActions {
  function openFormModal(opts: OpenFormModalOptions): void {
    deps.formModal({
      existing: (opts.existing || null) as never,
      isTransfer: isTransfer(opts.existing || null, opts.list),
      defaultDate: opts.defaultDate,
      onSaved: typeof opts.onSaved === 'function' ? opts.onSaved : () => undefined,
    });
  }

  // ── Delete (with scope for grouped/recurring) ─────────────
  function openDeleteModal(tx: TxLike, opts: OpenDeleteModalOptions = {}): void {
    const list = opts.list || [];
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : () => undefined;

    const transfer = isTransfer(tx, list);
    // Transfer legs carry a groupId but are not installments — never offer the parcelas scope for them.
    const isGrouped = !!tx.groupId && !transfer;

    if (!isGrouped) {
      const msg = transfer
        ? 'Excluir esta transferência? As duas pernas (saída e entrada) serão removidas. Esta ação não pode ser desfeita.'
        : 'Excluir <strong>' + esc(tx.description || 'lançamento') + '</strong>? Esta ação não pode ser desfeita.';
      confirmModal({
        title: transfer ? 'Excluir Transferência' : 'Excluir Lançamento',
        body: modalText(msg),
        onConfirm: (m, reEnable) => {
          runMutation(deps.transactionService.remove(tx.accountId as string, tx.id as string), {
            modal: m, success: 'Lançamento excluído', failure: 'Falha ao excluir',
            onDone, onError: reEnable,
          });
        },
      });
      return;
    }

    // Grouped: scope choice. Nomeia a parcela — escolher "este e seguintes" sem saber qual das N
    // está em jogo é decidir no escuro.
    const bodyHtml = '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;"><strong>' + esc(Transaction.describe(tx) || 'Lançamento') + '</strong> faz parte de um grupo de parcelas. Como deseja excluí-lo?</p>';
    const $only = btn({ variant: 'secondary', size: 'md', label: 'Apenas este', attrs: 'data-act="del-single" type="button"' });
    const $future = btn({ variant: 'secondary', size: 'md', label: 'Este e seguintes', attrs: 'data-act="del-future" type="button"' });
    const $all = btn({ variant: 'danger', size: 'md', icon: 'trash', label: 'Todos', attrs: 'data-act="del-all" type="button"' });
    const $cancel = btn({ variant: 'ghost', size: 'md', label: 'Cancelar', attrs: 'data-modal-close="1" type="button"' });
    const $footer = modalFooter([$cancel, $only, $future, $all], { align: 'end' });

    const m = modal({ title: 'Excluir Lançamento Recorrente', body: bodyHtml, footer: $footer });
    m.open();

    function doRemove(mode: string): void {
      runMutation(deps.transactionService.remove(tx.accountId as string, tx.id as string, mode), {
        $btn: m.$el.find('button'), modal: m,
        success: 'Lançamento(s) excluído(s)', failure: 'Falha ao excluir', onDone,
      });
    }
    m.$el.on('click', '[data-act=del-single]', () => doRemove('SINGLE'));
    m.$el.on('click', '[data-act=del-future]', () => doRemove('FUTURE'));
    m.$el.on('click', '[data-act=del-all]', () => doRemove('ALL'));
  }

  // ── Quick mark as paid ────────────────────────────────────
  function markPaid(tx: TxLike, opts: MarkPaidOptions = {}): void {
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : () => undefined;
    const today = new Date().toISOString().slice(0, 10);
    runMutation(deps.transactionService.patchStatus(tx.accountId as string, tx.id as string, 'confirmed', today), {
      success: 'Lançamento confirmado', failure: 'Falha ao confirmar', onDone,
    });
  }

  return { isTransfer, openFormModal, openDeleteModal, markPaid };
}
