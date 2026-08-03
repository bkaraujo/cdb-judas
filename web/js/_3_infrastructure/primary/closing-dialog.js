/* _3_infrastructure/primary/closing-dialog.js — set/clear the accounting closing period.
 *
 * window.closingDialog({ current, onChange })
 *   current: "YYYY-MM" string of the active closing, or null.
 *   onChange(newPeriodOrNull) — called after a successful save/clear so the caller
 *     (sidebar) can update its cached state and re-render.
 * Opens immediately and returns the modal handle, mirroring confirmModal/linkedDeleteDialog.
 * Load order: after ui.js/helpers.js/icons.js (see _3_infrastructure.js).
 */
(function () {
  function monthYearLabel(p) {
    const d = new Date(p.year, p.month - 1, 1);
    const name = new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(d);
    return name.charAt(0).toUpperCase() + name.slice(1) + '/' + p.year;
  }

  function closingDialog(opts) {
    opts = opts || {};
    const current = opts.current || null;
    let sel = window.Domain.Period.fromYyyyDashMm(current) || window.Domain.Period.currentMonth();

    const bodyHtml =
      window.modalText('Lançamentos e transferências até o mês escolhido ficam bloqueados para criação, edição e exclusão.') +
      '<div data-region="period-nav"></div>' +
      '<p style="font-size:12px;color:var(--text-secondary);margin-top:10px;">' +
        (current ? 'Fechamento atual: ' + window.esc(monthYearLabel(window.Domain.Period.fromYyyyDashMm(current)))
                 : 'Nenhum fechamento definido') +
      '</p>';

    const $clear = window.btn({
      variant: 'danger', size: 'md', label: 'Limpar', disabled: !current,
      attrs: 'data-act="closing-clear" type="button"',
    });
    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="closing-save" type="button"',
    });

    const m = window.modal({
      title: 'Fechamento contábil',
      body: bodyHtml,
      footer: window.modalFooter([$clear, $cancel, $save], { align: 'end' }),
    });
    m.open();

    function renderNav() {
      const $region = m.$body.find('[data-region=period-nav]');
      $region.empty().append(window.periodNav({
        month: sel.month,
        year: sel.year,
        onPrev:   function () { sel = window.Domain.Period.shift(sel, -1); renderNav(); },
        onNext:   function () { sel = window.Domain.Period.shift(sel, 1); renderNav(); },
        onChange: function (mo, yr) { sel = window.Domain.Period.create(mo, yr); renderNav(); },
      }));
    }
    renderNav();

    m.$el.on('click', '[data-act=closing-save]', function () {
      const $b = $(this).prop('disabled', true);
      const period = window.Domain.Period.yyyyDashMm(sel);
      window.App.ClosingService.set(period)
        .then(function () {
          m.close();
          window.toast('Fechamento definido', 'success');
          if (opts.onChange) opts.onChange(period);
        })
        .catch(function (err) {
          $b.prop('disabled', false);
          window.toast(err && err.message ? err.message : 'Falha ao salvar fechamento', 'error');
        });
    });

    m.$el.on('click', '[data-act=closing-clear]', function () {
      const $b = $(this).prop('disabled', true);
      window.App.ClosingService.clear()
        .then(function () {
          m.close();
          window.toast('Fechamento removido', 'success');
          if (opts.onChange) opts.onChange(null);
        })
        .catch(function (err) {
          $b.prop('disabled', false);
          window.toast(err && err.message ? err.message : 'Falha ao salvar fechamento', 'error');
        });
    });

    return m;
  }

  window.closingDialog = closingDialog;
})();
