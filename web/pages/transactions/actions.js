/* pages/transactions/actions.js — Ações de linha de lançamento (transfer detection,
 * exclusão com escopo de grupo, confirmar pagamento), compartilhadas entre a tela de
 * Lançamentos (transactions.js) e o Extrato de Contas (statement.js).
 *
 * Todas recebem a lista de transações do período (`list`) para detectar transferências
 * — uma transferência é um par income/expense no mesmo grupo — e um callback de recarga
 * (`onDone`/`onSaved`) que a página passa para atualizar sua própria visão após a mutação.
 * O modal de criação/edição vive em create-edit.js (window.transactionFormModal).
 */
(function () {
  // A transfer is stored as two legs (one income + one expense) sharing a groupId, unlike
  // installments whose legs share a single type. Both legs carry the same date, so they sit in
  // the same month view together — detection works off the loaded list.
  function isTransfer(tx, list) {
    if (!tx || !tx.groupId) return false;
    const group = (list || []).filter(function (t) { return String(t.groupId) === String(tx.groupId); });
    const hasIncome = group.some(function (t) { return t.type === 'income'; });
    const hasExpense = group.some(function (t) { return t.type === 'expense'; });
    return hasIncome && hasExpense;
  }

  // Create/edit modal (create-edit.js); transfer detection + default date + reload are
  // supplied by the caller so this stays page-agnostic.
  function openFormModal(opts) {
    opts = opts || {};
    window.transactionFormModal({
      existing: opts.existing || null,
      isTransfer: isTransfer(opts.existing || null, opts.list),
      defaultDate: opts.defaultDate,
      onSaved: typeof opts.onSaved === 'function' ? opts.onSaved : function () { return null; },
    });
  }

  // ── Delete (with scope for grouped/recurring) ─────────────
  function openDeleteModal(tx, opts) {
    opts = opts || {};
    const list = opts.list || [];
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : function () { return null; };

    const transfer = isTransfer(tx, list);
    // Transfer legs carry a groupId but are not installments — never offer the parcelas scope for them.
    const isGrouped = !!tx.groupId && !transfer;

    if (!isGrouped) {
      // Simple confirm. A transfer always removes both legs server-side.
      const msg = transfer
        ? 'Excluir esta transferência? As duas pernas (saída e entrada) serão removidas. Esta ação não pode ser desfeita.'
        : 'Excluir <strong>' + window.esc(tx.description || 'lançamento') + '</strong>? Esta ação não pode ser desfeita.';
      window.confirmModal({
        title: transfer ? 'Excluir Transferência' : 'Excluir Lançamento',
        body: window.modalText(msg),
        onConfirm: function (m, reEnable) {
          window.App.TransactionService.remove(tx.accountId, tx.id).then(function () {
            m.close();
            window.toast('Lançamento excluído', 'success');
            return onDone();
          }).catch(function (err) {
            reEnable();
            window.toast((err && err.message) || 'Falha ao excluir', 'error');
          });
        },
      });
      return;
    }

    // Grouped: scope choice.
    const bodyHtml =
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Este lançamento faz parte de um grupo de parcelas. ' +
        'Como deseja excluí-lo?' +
      '</p>';
    const $only = window.btn({
      variant: 'secondary', size: 'md', label: 'Apenas este',
      attrs: 'data-act="del-single" type="button"'
    });
    const $future = window.btn({
      variant: 'secondary', size: 'md', label: 'Este e seguintes',
      attrs: 'data-act="del-future" type="button"'
    });
    const $all = window.btn({
      variant: 'danger', size: 'md', icon: 'trash', label: 'Todos',
      attrs: 'data-act="del-all" type="button"'
    });
    const $cancel = window.btn({
      variant: 'ghost', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $footer = window.modalFooter([$cancel, $only, $future, $all], { align: 'end' });

    const m = window.modal({
      title: 'Excluir Lançamento Recorrente',
      body: bodyHtml,
      footer: $footer,
    });
    m.open();

    function doRemove(mode) {
      const $btns = m.$el.find('button').prop('disabled', true);
      window.App.TransactionService.remove(tx.accountId, tx.id, mode).then(function () {
        m.close();
        window.toast('Lançamento(s) excluído(s)', 'success');
        return onDone();
      }).catch(function (err) {
        $btns.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao excluir', 'error');
      });
    }
    m.$el.on('click', '[data-act=del-single]', function () { doRemove('SINGLE'); });
    m.$el.on('click', '[data-act=del-future]', function () { doRemove('FUTURE'); });
    m.$el.on('click', '[data-act=del-all]',    function () { doRemove('ALL'); });
  }

  // ── Quick mark as paid ────────────────────────────────────
  function markPaid(tx, opts) {
    opts = opts || {};
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : function () { return null; };
    const today = new Date().toISOString().slice(0, 10);
    window.App.TransactionService.patchStatus(tx.accountId, tx.id, 'confirmed', today).then(function () {
      window.toast('Lançamento confirmado', 'success');
      return onDone();
    }).catch(function (err) {
      window.toast((err && err.message) || 'Falha ao confirmar', 'error');
    });
  }

  window.transactionActions = {
    isTransfer: isTransfer,
    openFormModal: openFormModal,
    openDeleteModal: openDeleteModal,
    markPaid: markPaid,
  };
})();
