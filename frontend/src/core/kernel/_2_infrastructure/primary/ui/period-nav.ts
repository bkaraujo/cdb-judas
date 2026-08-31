import $ from 'jquery';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';

export interface PeriodNavOptions {
  month?: number;
  year?: number;
  onPrev?: (e: JQuery.ClickEvent) => void;
  onNext?: (e: JQuery.ClickEvent) => void;
  onChange?: (month: number, year: number) => void;
}

function monthNames(): string[] {
  const fmt = new Intl.DateTimeFormat('pt-BR', { month: 'long' });
  const out: string[] = [];
  for (let m = 0; m < 12; m++) {
    const s = fmt.format(new Date(2000, m, 1));
    out.push(s.charAt(0).toUpperCase() + s.slice(1));
  }
  return out;
}

/** `<`  [month dropdown]  [year field]  `>`
 * opts.month is 1-based (1-12); opts.year a full year. onPrev/onNext step by a month
 * (unchanged); onChange(month, year) fires on direct edit of the dropdown or the 4-digit year
 * field (month reported 1-based). Callers re-render after every callback, so the widgets reflect
 * fresh state on the next build — no live setter needed. */
export function periodNav(opts: PeriodNavOptions = {}): JQuery {
  const month = opts.month || 1;
  const year = opts.year || new Date().getFullYear();

  let optionsHtml = '';
  monthNames().forEach((name, i) => {
    const m = i + 1;
    optionsHtml += '<option value="' + m + '"' + (m === month ? ' selected' : '') + '>' + name + '</option>';
  });

  const $el = $(
    '<div class="period-nav">' +
      '<button class="icon-btn" data-act="prev">' + icon('chevronLeft', 14) + '</button>' +
      '<select class="period-nav-month">' + optionsHtml + '</select>' +
      '<input class="period-nav-year" type="text" inputmode="numeric" maxlength="4" value="' + year + '">' +
      '<button class="icon-btn" data-act="next">' + icon('chevronRight', 14) + '</button>' +
    '</div>',
  );

  if (opts.onPrev) $el.find('[data-act=prev]').on('click', opts.onPrev);
  if (opts.onNext) $el.find('[data-act=next]').on('click', opts.onNext);

  const $month = $el.find('.period-nav-month');
  const $year = $el.find('.period-nav-year');

  function fire(): void {
    if (!opts.onChange) return;
    opts.onChange(parseInt($month.val() as string, 10), parseInt($year.val() as string, 10));
  }

  $month.on('change', fire);
  $year.on('input', () => {
    $year.val(($year.val() as string).replace(/\D/g, '').slice(0, 4));
  });
  $year.on('change', () => {
    if (/^\d{4}$/.test($year.val() as string)) fire();
    else $year.val(year); // restore last valid year on junk input
  });
  $year.on('keydown', (e) => {
    if (e.key === 'Enter') $year.trigger('change');
  });

  return $el;
}
