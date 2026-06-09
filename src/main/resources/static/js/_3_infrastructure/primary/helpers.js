/* _3_infrastructure/primary/helpers.js — page-level helpers shared across the
 * CRUD page modules. Builds on ui.js (btn/modal/toast) + format.js (esc) to
 * factor out patterns that were copy-pasted in nearly every page:
 *   - byId            : linear lookup of a record in a state array
 *   - modalFooter     : flex row of footer buttons
 *   - saveCancelFooter: the Cancelar + Salvar footer used by every form modal
 *   - modalText       : the standard muted <p> used in modal bodies
 *   - confirmModal    : Cancelar + (danger) confirm modal with wired action
 *   - swatchesHtml    : color-swatch buttons (round) for color pickers
 *   - bindSwatches    : wire swatch clicks + <input type=color> sync
 *   - shiftMonth      : advance/rewind {month,year} state by N months
 * Load order: after ui.js / format.js, before any pages/*.js (see _3_infrastructure.js).
 */
(function () {

  /* ---- Record lookup ----
   * Replaces the per-page findAccount/findItem/findCard/findTag/findTx helpers.
   * String-compares ids so a numeric record id matches a data-id attribute. */
  function byId(arr, id) {
    if (!arr) return null;
    for (let i = 0; i < arr.length; i++) {
      if (String(arr[i].id) === String(id)) return arr[i];
    }
    return null;
  }

  /* ---- Footer builders ---- */

  // A horizontal row of footer buttons. `buttons` is a jQuery element or array
  // of them. opts.align === 'end' right-aligns and wraps (grouped-delete style).
  function modalFooter(buttons, opts) {
    opts = opts || {};
    const style = opts.align === 'end'
      ? 'display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;'
      : 'display:flex;gap:10px;';
    const $footer = $('<div style="' + style + '"></div>');
    [].concat(buttons).forEach(function ($b) { if ($b) $footer.append($b); });
    return $footer;
  }

  // The Cancelar + Salvar footer shared by every create/edit form modal.
  // opts.saveLabel (default 'Salvar'), opts.saveAttrs (default submit + data-act=save).
  function saveCancelFooter(opts) {
    opts = opts || {};
    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: opts.cancelLabel || 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: opts.saveLabel || 'Salvar',
      attrs: opts.saveAttrs || 'data-act="save" type="submit"',
    });
    return modalFooter([$cancel, $save]);
  }

  /* ---- Modal body text ----
   * The muted paragraph used in confirm/info modal bodies. `html` may contain
   * inline markup (e.g. <strong>name</strong>) — callers must esc() user data. */
  function modalText(html) {
    return '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
      html +
    '</p>';
  }

  /* ---- Confirm modal ----
   * Cancelar + a confirm button (danger/trash by default). Opens immediately and
   * wires the confirm click to disable the button and invoke opts.onConfirm(m, reEnable).
   * The callback owns closing on success (m.close()) and re-enabling on failure
   * (reEnable()), matching the existing delete flows.
   *   opts: { title, body, confirmLabel, confirmIcon, danger=true, cancelLabel, onConfirm }
   * Returns the modal handle. */
  function confirmModal(opts) {
    opts = opts || {};
    const $cancel = window.btn({
      variant: opts.danger === false ? 'ghost' : 'secondary',
      size: 'md', label: opts.cancelLabel || 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const confirmBtn = {
      variant: opts.danger === false ? 'primary' : 'danger',
      size: 'md', label: opts.confirmLabel || 'Excluir',
      attrs: 'data-act="modal-confirm" type="button"',
    };
    if (opts.confirmIcon !== null) confirmBtn.icon = opts.confirmIcon || 'trash';
    const $confirm = window.btn(confirmBtn);

    const m = window.modal({
      title: opts.title || 'Confirmar',
      body: opts.body || '',
      footer: modalFooter([$cancel, $confirm]),
    });
    m.open();

    m.$el.on('click', '[data-act=modal-confirm]', function () {
      const $b = $(this).prop('disabled', true);
      if (typeof opts.onConfirm === 'function') {
        opts.onConfirm(m, function reEnable() { $b.prop('disabled', false); });
      }
    });
    return m;
  }

  /* ---- Color swatches ----
   * Round color-swatch buttons for a <input type=color> picker. Used by the
   * accounts and tags form modals.
   * swatchesHtml(colors, selectedHex) → HTML string of [data-swatch] buttons. */
  function swatchesHtml(colors, selectedHex) {
    return colors.map(function (c) {
      const isSel = c.toLowerCase() === String(selectedHex || '').toLowerCase();
      return '<button type="button" data-swatch="' + window.esc(c) + '" ' +
        'style="width:22px;height:22px;border-radius:50%;cursor:pointer;' +
        'border:' + (isSel ? '2px solid var(--text-primary)' : '1px solid var(--border)') + ';' +
        'background:' + window.esc(c) + ';padding:0;flex-shrink:0;" title="' + window.esc(c) + '"></button>';
    }).join('');
  }

  // Wire the swatch grid inside a modal to a color <input>: clicking a swatch
  // sets the input value and re-paints borders; editing the color input repaints
  // too. Swatch clicks are ignored while the color input is disabled (accounts'
  // credit-card mode). Returns a paint(hex) fn so callers can repaint after
  // programmatic color changes.
  function bindSwatches(m, $colorInput) {
    function paint(activeHex) {
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
      $colorInput.val(c);
      paint(c);
    });
    $colorInput.on('input change', function () { paint($colorInput.val()); });
    return paint;
  }

  /* ---- Period navigation ----
   * Advance/rewind a { month, year } state object by `delta` months, handling
   * year roll-over. `oneBased` true → month is 1-12 (credit-cards, statement);
   * false → month is 0-11 (transactions, accounts-payable, budget). */
  function shiftMonth(state, delta, oneBased) {
    const base = oneBased ? 1 : 0;
    let m = state.month - base + delta;     // normalize to 0-based
    const y = state.year + Math.floor(m / 12);
    m = ((m % 12) + 12) % 12;
    state.month = m + base;
    state.year = y;
  }

  // Expose as convenience globals (pages call window.* like window.btn/window.esc).
  window.byId            = byId;
  window.modalFooter     = modalFooter;
  window.saveCancelFooter = saveCancelFooter;
  window.modalText       = modalText;
  window.confirmModal    = confirmModal;
  window.swatchesHtml    = swatchesHtml;
  window.bindSwatches    = bindSwatches;
  window.shiftMonth      = shiftMonth;
})();
