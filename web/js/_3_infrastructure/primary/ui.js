/* _3_infrastructure/primary/ui.js — DOM-building helpers returning HTML strings or jQuery elements. */

(function () {
  let uid = 0;
  function nextId() { return 'ui-' + (++uid); }

  /* ---- Buttons ---- */
  function btn(opts) {
    opts = opts || {};
    const variant = opts.variant || 'primary';
    const size    = opts.size    || 'md';
    const iconHtml = opts.icon
      ? window.icon(opts.icon, size === 'sm' ? 13 : 15)
      : '';
    const label = opts.label != null ? esc(opts.label) : '';
    const attrs = opts.attrs || '';
    const disabled = opts.disabled ? ' disabled' : '';
    const html = '<button class="btn btn-' + variant + ' btn-' + size + '"' + disabled + ' ' + attrs + '>' +
      iconHtml + (label ? '<span>' + label + '</span>' : '') +
      '</button>';
    const $el = $(html);
    if (opts.onClick) $el.on('click', opts.onClick);
    return $el;
  }

  function iconBtn(name, opts) {
    opts = opts || {};
    const size = opts.size || 18;
    const $el = $('<button class="icon-btn" type="button">' + window.icon(name, size) + '</button>');
    if (opts.title) $el.attr('title', opts.title);
    if (opts.onClick) $el.on('click', opts.onClick);
    return $el;
  }

  /* ---- Row Action Button ---- */
  function rowActionBtn(iconName, title, id, danger) {
    const color = danger ? 'var(--expense)' : 'var(--text-secondary)';
    return $(
      '<button type="button" class="icon-btn" title="' + esc(title) + '" ' +
        'data-act="' + esc(iconName) + '" data-id="' + esc(id) + '" ' +
        'style="width:28px;height:28px;color:' + color + ';">' +
        window.icon(iconName, 14) +
      '</button>'
    );
  }

  /* ---- Badge ---- */
  function badge(text, color) {
    color = color || 'accent';
    return '<span class="badge badge-' + esc(color) + '">' + esc(text) + '</span>';
  }

  /* ---- Card ---- */
  function card(content, opts) {
    opts = opts || {};
    const $el = $('<div class="card"></div>');
    if (opts.style) $el.attr('style', opts.style);
    if (typeof content === 'string') $el.html(content);
    else if (content) $el.append(content);
    return $el;
  }

  /* ---- Empty state ---- */
  function emptyState(opts) {
    opts = opts || {};
    const iconName = opts.icon  || 'activity';
    const title    = opts.title || '';
    const desc     = opts.desc  || '';
    return '<div class="empty-state">' +
      '<div class="empty-state-icon">' + window.icon(iconName, 24) + '</div>' +
      '<div>' +
        '<p class="empty-state-title">' + esc(title) + '</p>' +
        (desc ? '<p class="empty-state-desc">' + esc(desc) + '</p>' : '') +
      '</div>' +
    '</div>';
  }

  /* ---- Period nav ----
   * `<`  [month dropdown]  [year field]  `>`
   * opts.month is 1-based (1-12); opts.year a full year. onPrev/onNext step by
   * a month (unchanged); onChange(month, year) fires on direct edit of the
   * dropdown or the 4-digit year field (month reported 1-based). Callers
   * re-render after every callback, so the widgets reflect fresh state on the
   * next build — no live setter needed. */
  function monthNames() {
    const fmt = new Intl.DateTimeFormat('pt-BR', { month: 'long' });
    const out = [];
    for (let m = 0; m < 12; m++) {
      const s = fmt.format(new Date(2000, m, 1));
      out.push(s.charAt(0).toUpperCase() + s.slice(1));
    }
    return out;
  }

  function periodNav(opts) {
    opts = opts || {};
    const month = opts.month || 1;
    const year  = opts.year  || new Date().getFullYear();

    let optionsHtml = '';
    monthNames().forEach(function (name, i) {
      const m = i + 1;
      optionsHtml += '<option value="' + m + '"' + (m === month ? ' selected' : '') + '>' + esc(name) + '</option>';
    });

    const $el = $(
      '<div class="period-nav">' +
        '<button class="icon-btn" data-act="prev">' + window.icon('chevronLeft', 14) + '</button>' +
        '<select class="period-nav-month">' + optionsHtml + '</select>' +
        '<input class="period-nav-year" type="text" inputmode="numeric" maxlength="4" value="' + year + '">' +
        '<button class="icon-btn" data-act="next">' + window.icon('chevronRight', 14) + '</button>' +
      '</div>'
    );

    if (opts.onPrev) $el.find('[data-act=prev]').on('click', opts.onPrev);
    if (opts.onNext) $el.find('[data-act=next]').on('click', opts.onNext);

    const $month = $el.find('.period-nav-month');
    const $year  = $el.find('.period-nav-year');

    function fire() {
      if (!opts.onChange) return;
      opts.onChange(parseInt($month.val(), 10), parseInt($year.val(), 10));
    }

    $month.on('change', fire);
    $year.on('input', function () { $year.val($year.val().replace(/\D/g, '').slice(0, 4)); });
    $year.on('change', function () {
      if (/^\d{4}$/.test($year.val())) fire();
      else $year.val(year);                 // restore last valid year on junk input
    });
    $year.on('keydown', function (e) { if (e.key === 'Enter') $year.trigger('change'); });

    return $el;
  }

  /* ---- Tabs ---- */
  function tabs(items, active, onChange) {
    const $el = $('<div class="tabs"></div>');
    items.forEach(function (it) {
      const isActive = it.id === active;
      const $t = $('<button class="tab' + (isActive ? ' active' : '') + '"></button>')
        .text(it.label).attr('data-id', it.id);
      $el.append($t);
    });
    $el.on('click', '.tab', function () {
      const id = $(this).attr('data-id');
      $el.find('.tab').removeClass('active');
      $(this).addClass('active');
      if (onChange) onChange(id);
    });
    return $el;
  }

  /* ---- Modal ----
   * Returns { open, close, $el }. Stacks: each modal has its own overlay.
   * Persistent by default: no overlay-click close and no header X. Pass
   * `opts.persistent === false` to re-enable overlay click + header X.
   * ESC always cancels the operation and closes the top-most modal,
   * regardless of `persistent`. Pass `opts.locked === true` to opt out of
   * ESC too (e.g. the auth/login gate that must not be dismissable).
   */
  function modal(opts) {
    opts = opts || {};
    const id = nextId();
    const persistent = opts.persistent !== false;
    const locked = opts.locked === true;
    const titleHtml = '<h3>' + esc(opts.title || '') + '</h3>';
    const bodyHtml  = opts.body || '';
    const footerContent = opts.footer;
    let footerStr = '';
    if (footerContent) {
      if (typeof footerContent === 'string') footerStr = footerContent;
      else if (footerContent.jquery) footerStr = ''; // injected below
      else if (footerContent.outerHTML) footerStr = footerContent.outerHTML;
    }
    const footerHtml = opts.footer ? '<div class="modal-footer"></div>' : '';
    const closeBtnHtml = persistent
      ? ''
      : '<button class="icon-btn" data-modal-close="1">' + window.icon('x', 18) + '</button>';
    let overlayClass = persistent ? 'modal-overlay modal-persistent' : 'modal-overlay';
    if (locked) overlayClass += ' modal-locked';
    const $overlay = $(
      '<div class="' + overlayClass + '" data-modal="' + id + '">' +
        '<div class="modal-box fade-in">' +
          '<div class="modal-header">' +
            titleHtml +
            closeBtnHtml +
          '</div>' +
          '<div class="modal-body"></div>' +
          footerHtml +
        '</div>' +
      '</div>'
    );
    const $body = $overlay.find('.modal-body');
    if (typeof bodyHtml === 'string') $body.html(bodyHtml);
    else if (bodyHtml) $body.append(bodyHtml);
    const $footer = $overlay.find('.modal-footer');
    if ($footer.length) {
      if (typeof footerContent === 'string') $footer.html(footerContent);
      else if (footerContent && footerContent.jquery) $footer.append(footerContent);
      else if (footerContent) $footer.html(footerStr);
    }

    function close() {
      $overlay.remove();
      if (opts.onClose) opts.onClose();
    }
    function open() {
      $('body').append($overlay);
    }
    if (!persistent) {
      $overlay.on('click', function (e) { if (e.target === $overlay[0]) close(); });
    }
    $overlay.on('click', '[data-modal-close]', close);
    $overlay.on('modal:dismiss', close);

    return { open: open, close: close, $el: $overlay, $body: $body };
  }

  /* ESC cancels the operation and closes the top-most modal (skips locked). */
  $(document).on('keydown', function (e) {
    if (e.key !== 'Escape') return;
    const $top = $('.modal-overlay').not('.modal-locked').last();
    if ($top.length) $top.trigger('modal:dismiss');
  });

  /* ---- Tags multi-select dropdown ----
   * <details>/<summary> nativo: abre/fecha sem JS e cresce em fluxo normal (nunca é cortado por um
   * ancestral com overflow:scroll, ao contrário de um painel position:absolute). O rótulo do
   * <summary> mostra só a contagem ("2 tags"/"Nenhuma tag"); cada consumidor liga seu próprio
   * handler delegado em '[data-tag-check]' pra atualizar o próprio estado (a lista de tags
   * marcadas por linha/transação é dona de quem chama, este helper só desenha).
   */
  function tagsCountLabel(count) {
    if (!count) return 'Nenhuma tag';
    return count + (count === 1 ? ' tag' : ' tags');
  }

  function tagsDropdownHtml(selectedIds, key) {
    const tags = window.App.CacheStore.tags();
    if (!tags.length) return '<span style="font-size:11px;color:var(--text-muted);">Sem tags</span>';
    const sel = (selectedIds || []).map(String);
    const items = tags.map(function (t) {
      const checked = sel.indexOf(String(t.id)) !== -1 ? ' checked' : '';
      const color = t.color || 'var(--text-muted)';
      return '<label style="display:flex;align-items:center;gap:6px;padding:4px 6px;font-size:12px;' +
          'cursor:pointer;white-space:nowrap;">' +
        '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(t.id) + '"' +
          checked + ' style="cursor:pointer;" />' +
        '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
        esc(t.name) +
      '</label>';
    }).join('');
    return (
      '<details data-region="tags-dropdown" data-idx="' + esc(key) + '" style="font-size:12px;">' +
        '<summary data-region="tags-summary" style="cursor:pointer;padding:4px 8px;border:1px solid var(--border);' +
          'border-radius:6px;display:inline-block;color:var(--text-secondary);user-select:none;">' +
          esc(tagsCountLabel(sel.length)) +
        '</summary>' +
        '<div style="margin-top:6px;padding:6px;border:1px solid var(--border);border-radius:6px;' +
          'background:var(--bg-hover);display:flex;flex-direction:column;gap:2px;min-width:140px;">' +
          items +
        '</div>' +
      '</details>'
    );
  }

  /** Atualiza o rótulo de contagem do <summary> mais próximo de um checkbox de tag alterado —
   *  chamar depois de mutar o array de tagIds do chamador, dentro do handler de 'change'. */
  function refreshTagsDropdownLabel($checkbox) {
    const $summary = $checkbox.closest('details').find('> summary[data-region=tags-summary]');
    const count = $checkbox.closest('details').find('[data-tag-check]:checked').length;
    $summary.text(tagsCountLabel(count));
  }

  /* ---- Toast ---- */
  function toast(msg, variant) {
    variant = variant || 'info'; // info | success | error | warning
    const $t = $('<div class="toast toast-' + esc(variant) + '"></div>').text(msg);
    let $stack = $('#toast-stack');
    if (!$stack.length) $stack = $('<div id="toast-stack"></div>').appendTo('body');
    $stack.append($t);
    setTimeout(function () { $t.addClass('toast-fade'); }, 2400);
    setTimeout(function () { $t.remove(); }, 2900);
  }

  window.UI = {
    btn: btn,
    iconBtn: iconBtn,
    rowActionBtn: rowActionBtn,
    badge: badge,
    card: card,
    emptyState: emptyState,
    periodNav: periodNav,
    tabs: tabs,
    modal: modal,
    toast: toast,
    tagsDropdownHtml: tagsDropdownHtml,
    refreshTagsDropdownLabel: refreshTagsDropdownLabel,
  };

  // Convenience globals (used inline by pages).
  window.btn          = btn;
  window.iconBtn      = iconBtn;
  window.rowActionBtn = rowActionBtn;
  window.badge        = badge;
  window.card       = card;
  window.emptyState = emptyState;
  window.periodNav  = periodNav;
  window.tabs       = tabs;
  window.modal      = modal;
  window.toast      = toast;
  window.tagsDropdownHtml = tagsDropdownHtml;
  window.refreshTagsDropdownLabel = refreshTagsDropdownLabel;
})();
