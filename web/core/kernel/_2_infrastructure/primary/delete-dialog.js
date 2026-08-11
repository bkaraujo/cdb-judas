/* _3_infrastructure/primary/delete-dialog.js — uniform "entity has linked transactions" dialog,
 * shown on a 409 LINKED_TRANSACTIONS response (accounts/cards/categories/tags).
 *
 * window.linkedDeleteDialog({ title, intro, options, onConfirm })
 *   options: [{ value, label, hint, choices, danger }]
 *     - choices: [{value,label}] turns the option into a target picker (embeds a <select>,
 *       enabled only while that radio is checked); an option with an EMPTY choices array is
 *       hidden entirely (e.g. no other account/card/tag to move into).
 *     - danger: true switches the Confirm button to the danger variant while selected.
 *   onConfirm({strategy, targetId}, m, reEnable) — mirrors confirmModal's (m, reEnable) contract;
 *     targetId is null for options without choices.
 * Load order: after helpers.js (see _3_infrastructure.js).
 */
(function () {
  function optionRowHtml(opt, idx) {
    const inputId = 'linked-delete-opt-' + idx;
    const hint = opt.hint
      ? '<div style="font-size:12px;color:var(--text-secondary);margin-top:2px;">' + window.esc(opt.hint) + '</div>'
      : '';
    const selectHtml = opt.choices
      ? '<select data-target-select disabled style="margin-top:6px;width:100%;" class="input">' +
          opt.choices.map(function (c) {
            return '<option value="' + window.esc(c.value) + '">' + window.esc(c.label) + '</option>';
          }).join('') +
        '</select>'
      : '';
    return (
      '<label for="' + inputId + '" data-option-row data-value="' + window.esc(opt.value) + '" ' +
        'style="display:block;padding:10px 12px;border:1px solid var(--border);border-radius:8px;margin-bottom:8px;cursor:pointer;">' +
        '<div style="display:flex;gap:8px;align-items:flex-start;">' +
          '<input type="radio" name="linked-delete-strategy" id="' + inputId + '" value="' + window.esc(opt.value) + '" ' +
            'style="width:16px;height:16px;flex-shrink:0;margin-top:3px;accent-color:var(--accent);">' +
          '<div style="flex:1;">' +
            '<div style="font-weight:500;">' + window.esc(opt.label) + '</div>' +
            hint +
            selectHtml +
          '</div>' +
        '</div>' +
      '</label>'
    );
  }

  function linkedDeleteDialog(opts) {
    opts = opts || {};
    const visibleOptions = (opts.options || []).filter(function (o) { return !o.choices || o.choices.length > 0; });

    const bodyHtml =
      (opts.intro ? window.modalText(opts.intro) : '') +
      '<div data-region="strategy-options">' + visibleOptions.map(optionRowHtml).join('') + '</div>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const $confirm = window.btn({
      variant: 'secondary', size: 'md', label: 'Confirmar', disabled: true,
      attrs: 'data-act="linked-delete-confirm" type="button"',
    });

    const m = window.modal({
      title: opts.title || 'Excluir',
      body: bodyHtml,
      footer: window.modalFooter([$cancel, $confirm]),
    });
    m.open();

    function optionRow(value) { return m.$body.find('[data-option-row][data-value="' + value + '"]'); }
    function selectedOption() {
      const value = m.$body.find('input[name="linked-delete-strategy"]:checked').val();
      return visibleOptions.filter(function (o) { return o.value === value; })[0] || null;
    }

    function refreshConfirmState() {
      const opt = selectedOption();
      let valid = !!opt;
      if (opt && opt.choices) {
        valid = valid && !!optionRow(opt.value).find('[data-target-select]').val();
      }
      m.$el.find('[data-act=linked-delete-confirm]')
        .prop('disabled', !valid)
        .removeClass('btn-danger btn-secondary')
        .addClass(opt && opt.danger ? 'btn-danger' : 'btn-secondary');
    }

    m.$body.on('change', 'input[name="linked-delete-strategy"]', function () {
      m.$body.find('[data-target-select]').prop('disabled', true);
      const opt = selectedOption();
      if (opt && opt.choices) optionRow(opt.value).find('[data-target-select]').prop('disabled', false);
      refreshConfirmState();
    });
    m.$body.on('change', '[data-target-select]', refreshConfirmState);

    m.$el.on('click', '[data-act=linked-delete-confirm]', function () {
      const opt = selectedOption();
      if (!opt) return;
      let targetId = null;
      if (opt.choices) {
        targetId = optionRow(opt.value).find('[data-target-select]').val();
        if (!targetId) return;
      }
      const $b = $(this).prop('disabled', true);
      if (typeof opts.onConfirm === 'function') {
        opts.onConfirm({ strategy: opt.value, targetId: targetId }, m, function reEnable() { $b.prop('disabled', false); });
      }
    });

    return m;
  }

  window.linkedDeleteDialog = linkedDeleteDialog;
})();
