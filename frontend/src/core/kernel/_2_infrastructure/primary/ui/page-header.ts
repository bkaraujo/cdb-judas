import $ from 'jquery';

export interface PageHeaderOptions {
  title?: string;
  subtitle?: string;
  actions?: JQuery | JQuery[];
  nav?: JQuery | JQuery[];
}

export function pageHeader(opts: PageHeaderOptions = {}): JQuery {
  const $titleBlock = opts.subtitle
    ? $('<div></div>').append(
        $('<h1></h1>').text(opts.title || ''),
        $('<p style="font-size:12px;color:var(--text-muted);margin-top:2px;"></p>').text(opts.subtitle),
      )
    : $('<h1></h1>').text(opts.title || '');

  // grid fixo title(esquerda) / actions(centro) / nav(direita) — ver .page-header em app.css.
  const $el = $('<div class="page-header"></div>').append($titleBlock);
  if (opts.actions) $('<div class="page-header-actions"></div>').append(opts.actions).appendTo($el);
  if (opts.nav) $('<div class="page-header-nav"></div>').append(opts.nav).appendTo($el);
  return $el;
}
