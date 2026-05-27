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

  /* ---- Period nav ---- */
  function periodNav(opts) {
    opts = opts || {};
    const $el = $(
      '<div class="period-nav">' +
        '<button class="icon-btn" data-act="prev">' + window.icon('chevronLeft', 14) + '</button>' +
        '<span class="period-nav-label"></span>' +
        '<button class="icon-btn" data-act="next">' + window.icon('chevronRight', 14) + '</button>' +
      '</div>'
    );
    $el.find('.period-nav-label').text(opts.label || '');
    if (opts.onPrev) $el.find('[data-act=prev]').on('click', opts.onPrev);
    if (opts.onNext) $el.find('[data-act=next]').on('click', opts.onNext);
    $el.setLabel = function (lbl) { $el.find('.period-nav-label').text(lbl || ''); };
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
   * Closes on overlay click and ESC (delegated on document), unless
   * `opts.persistent === true` — used for the login modal so users can't
   * dismiss it accidentally (e.g. typing Ctrl+Shift+I for DevTools).
   */
  function modal(opts) {
    opts = opts || {};
    const id = nextId();
    const persistent = !!opts.persistent;
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
    const overlayClass = persistent ? 'modal-overlay modal-persistent' : 'modal-overlay';
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

    return { open: open, close: close, $el: $overlay, $body: $body };
  }

  /* ESC closes the top-most non-persistent modal. */
  $(document).on('keydown', function (e) {
    if (e.key !== 'Escape') return;
    const $top = $('.modal-overlay').not('.modal-persistent').last();
    if ($top.length) $top.find('[data-modal-close]').trigger('click');
  });

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
})();
