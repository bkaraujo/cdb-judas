/** Set/clear the accounting closing period.
 *
 * `closingDialog({ current, onChange })`
 *   current: "YYYY-MM" string of the active closing, or null.
 *   onChange(newPeriodOrNull) — called after a successful save/clear so the caller (sidebar) can
 *     update its cached state and re-render.
 * Opens immediately and returns the modal handle, mirroring confirmModal/linkedDeleteDialog.
 */
import $ from 'jquery';
import { esc } from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import { modalFooter, modalText, runMutation } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { btn } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import type { Modal } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { modal } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { periodNav } from '@/core/kernel/_2_infrastructure/primary/ui/period-nav.ts';
import type { ClosingService } from '@/feature/accounts/service.ts';

export interface ClosingDialogOptions {
  current?: string | null;
  onChange?: (newPeriod: string | null) => void;
}

function monthYearLabel(p: Period.Period): string {
  const d = new Date(p.year, p.month - 1, 1);
  const name = new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(d);
  return name.charAt(0).toUpperCase() + name.slice(1) + '/' + p.year;
}

export function createClosingDialog(closingService: ClosingService) {
  return function closingDialog(opts: ClosingDialogOptions = {}): Modal {
    const current = opts.current || null;
    let sel = (current && Period.fromYyyyDashMm(current)) || Period.currentMonth();

    const bodyHtml =
      modalText('Lançamentos e transferências até o mês escolhido ficam bloqueados para criação, edição e exclusão.') +
      '<div data-region="period-nav"></div>' +
      '<p style="font-size:12px;color:var(--text-secondary);margin-top:10px;">' +
      (current ? 'Fechamento atual: ' + esc(monthYearLabel(Period.fromYyyyDashMm(current) as Period.Period)) : 'Nenhum fechamento definido') +
      '</p>';

    const $clear = btn({ variant: 'danger', size: 'md', label: 'Limpar', disabled: !current, attrs: 'data-act="closing-clear" type="button"' });
    const $cancel = btn({ variant: 'secondary', size: 'md', label: 'Cancelar', attrs: 'data-modal-close="1" type="button"' });
    const $save = btn({ variant: 'primary', size: 'md', label: 'Salvar', attrs: 'data-act="closing-save" type="button"' });

    const m = modal({
      title: 'Fechamento contábil',
      body: bodyHtml,
      footer: modalFooter([$clear, $cancel, $save], { align: 'end' }),
    });
    m.open();

    function renderNav(): void {
      const $region = m.$body.find('[data-region=period-nav]');
      $region.empty().append(
        periodNav({
          month: sel.month,
          year: sel.year,
          onPrev: () => {
            sel = Period.shift(sel, -1);
            renderNav();
          },
          onNext: () => {
            sel = Period.shift(sel, 1);
            renderNav();
          },
          onChange: (mo, yr) => {
            sel = Period.create(mo, yr);
            renderNav();
          },
        }),
      );
    }
    renderNav();

    m.$el.on('click', '[data-act=closing-save]', function () {
      const $b = $(this);
      const period = Period.yyyyDashMm(sel);
      runMutation(closingService.set(period), {
        $btn: $b,
        modal: m,
        success: 'Fechamento definido',
        failure: 'Falha ao salvar fechamento',
        onDone: () => {
          if (opts.onChange) opts.onChange(period);
        },
      });
    });

    m.$el.on('click', '[data-act=closing-clear]', function () {
      const $b = $(this);
      runMutation(closingService.clear(), {
        $btn: $b,
        modal: m,
        success: 'Fechamento removido',
        failure: 'Falha ao salvar fechamento',
        onDone: () => {
          if (opts.onChange) opts.onChange(null);
        },
      });
    });

    return m;
  };
}
