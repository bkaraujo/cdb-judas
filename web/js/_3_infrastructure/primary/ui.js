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

  /** opts.matchSelect: pele igual ao <select> de categoria (usado na tela de edição de transação) —
   *  painel abre em position:absolute (não empurra o layout) e ganha uma busca com filtro ao-vivo.
   *  Sem a opção, mantém o visual compacto original (usado na tabela de import), que cresce em fluxo
   *  normal de propósito — nunca é cortado por um ancestral com overflow:scroll, ao contrário de um
   *  painel position:absolute. */
  function tagsDropdownHtml(selectedIds, key, opts) {
    const tags = window.App.CacheStore.tags();
    if (!tags.length) return '<span style="font-size:11px;color:var(--text-muted);">Sem tags</span>';
    const sel = (selectedIds || []).map(String);
    const matchSelect = !!(opts && opts.matchSelect);
    const items = tags.map(function (t) {
      const checked = sel.indexOf(String(t.id)) !== -1 ? ' checked' : '';
      const color = t.color || 'var(--text-muted)';
      const labelAttrs = matchSelect
        ? ' class="search-dropdown-row"'
        : ' style="display:flex;align-items:center;gap:6px;padding:4px 6px;font-size:12px;' +
            'cursor:pointer;white-space:nowrap;"';
      return '<label' + labelAttrs + '>' +
        '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(t.id) + '"' +
          checked + ' style="cursor:pointer;" />' +
        '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
        esc(t.name) +
      '</label>';
    }).join('');

    if (matchSelect) {
      return (
        '<details class="search-dropdown" data-region="tags-dropdown" data-idx="' + esc(key) + '">' +
          '<summary class="search-dropdown-summary" data-region="tags-summary">' +
            '<span data-region="tags-summary-text">' + esc(tagsCountLabel(sel.length)) + '</span>' +
            '<span class="search-dropdown-chevron">' + window.icon('chevronDown', 14) + '</span>' +
          '</summary>' +
          '<div class="search-dropdown-panel">' +
            '<input type="text" class="search-dropdown-search" data-region="search-dropdown-search" ' +
              'placeholder="Buscar tag..." autocomplete="off" />' +
            '<div class="search-dropdown-items" data-region="search-dropdown-items">' + items + '</div>' +
            '<p class="search-dropdown-empty" data-region="search-dropdown-empty" style="display:none;">' +
              'Nenhuma tag encontrada' +
            '</p>' +
          '</div>' +
        '</details>'
      );
    }

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
    const $details = $checkbox.closest('details');
    const $summary = $details.find('> summary[data-region=tags-summary]');
    const count = $details.find('[data-tag-check]:checked').length;
    const label = tagsCountLabel(count);
    const $text = $summary.find('> [data-region=tags-summary-text]');
    if ($text.length) $text.text(label); else $summary.text(label);
  }

  /** Insere (ou marca, se já presente) uma tag recém-criada no dropdown `key` — contorna o lag
   *  do SSE até o CacheStore atualizar (mesma razão pela qual o quick-create de categoria também
   *  monta a option na mão em vez de esperar o cache). Só afeta o dropdown matchSelect (o único
   *  com `[data-region=search-dropdown-items]`); quem chama ainda precisa dar push no próprio
   *  array de ids selecionados, este helper só re-desenha. */
  function appendTagRow(key, tag) {
    const $items = $('[data-region=tags-dropdown][data-idx="' + esc(key) + '"] [data-region=search-dropdown-items]');
    if (!$items.length) return;
    const id = String(tag.id);
    const $existing = $items.find('[data-tag-id="' + esc(id) + '"]');
    if ($existing.length) {
      $existing.prop('checked', true);
      refreshTagsDropdownLabel($existing);
      return;
    }
    const color = tag.color || 'var(--text-muted)';
    const $row = $(
      '<label class="search-dropdown-row">' +
        '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(id) + '" checked ' +
          'style="cursor:pointer;" />' +
        '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
        esc(tag.name || '') +
      '</label>'
    );
    $items.append($row);
    refreshTagsDropdownLabel($row.find('[data-tag-check]'));
  }

  /* ---- Search dropdown: filtro ao-vivo compartilhado ----
   * Um único listener delegado cobre tanto o multi-select de tags (linhas com checkbox)
   * quanto o combobox de categoria (linhas de single-select) — ambos desenham suas linhas
   * com a classe `.search-dropdown-row` dentro de `.search-dropdown-panel`.
   */
  $(document).on('input', '.search-dropdown-search', function () {
    const q = $(this).val().trim().toLowerCase();
    const $panel = $(this).closest('.search-dropdown-panel');
    let visible = 0;
    $panel.find('.search-dropdown-row').each(function () {
      const match = !q || $(this).text().toLowerCase().indexOf(q) !== -1;
      $(this).toggleClass('search-dropdown-row-hidden', !match);
      if (match) visible++;
    });
    $panel.find('[data-region=search-dropdown-empty]').toggle(visible === 0);
  });

  /* `<details>` fires 'toggle' on itself without bubbling (unlike almost every other DOM event),
   * so `$(document).on('toggle', ...)` delegation never sees it — a capture-phase listener does,
   * since capture always walks document→target regardless of the event's bubbles flag. Used to
   * move keyboard focus into the filter box the moment a tag/category dropdown opens. */
  document.addEventListener('toggle', function (e) {
    const details = e.target;
    if (!details || !details.matches || !details.matches('details.search-dropdown') || !details.open) return;
    const input = details.querySelector('.search-dropdown-search');
    if (input) input.focus();
  }, true);

  /** Combobox single-select (categoria): desenha um <select> nativo (escondido, mantém toda a
   *  leitura/escrita existente por `.val()`) + este overlay com a mesma pele/filtro do dropdown
   *  de tags. `opts.pairedSelectId` liga o overlay ao id do <select> pra `refreshSearchSelect`
   *  re-sincronizar depois de uma mutação programática (ex.: "+ Nova categoria"). */
  function searchSelectHtml(items, selectedValue, key, opts) {
    opts = opts || {};
    const rows = items.map(function (it) {
      const isSel = it.value !== '' && String(it.value) === String(selectedValue);
      return '<div class="search-dropdown-row' + (isSel ? ' is-selected' : '') + '" ' +
          'data-dd-value="' + esc(it.value) + '" data-dd-label="' + esc(it.label) + '">' +
        esc(it.label) +
      '</div>';
    }).join('');
    const selected = items.filter(function (it) { return String(it.value) === String(selectedValue); })[0];
    const summaryText = selected ? selected.label : ((items[0] && items[0].label) || '');
    const forAttr = opts.pairedSelectId ? ' data-for="' + esc(opts.pairedSelectId) + '"' : '';
    return (
      '<details class="search-dropdown" data-region="search-dropdown" data-idx="' + esc(key) + '"' + forAttr + '>' +
        '<summary class="search-dropdown-summary" data-region="search-dropdown-summary">' +
          '<span data-region="search-dropdown-summary-text">' + esc(summaryText) + '</span>' +
          '<span class="search-dropdown-chevron">' + window.icon('chevronDown', 14) + '</span>' +
        '</summary>' +
        '<div class="search-dropdown-panel">' +
          '<input type="text" class="search-dropdown-search" data-region="search-dropdown-search" ' +
            'placeholder="Buscar..." autocomplete="off" />' +
          '<div class="search-dropdown-items" data-region="search-dropdown-items">' + rows + '</div>' +
          '<p class="search-dropdown-empty" data-region="search-dropdown-empty" style="display:none;">' +
            'Nenhum resultado' +
          '</p>' +
        '</div>' +
      '</details>'
    );
  }

  /** Re-desenha as linhas + rótulo do overlay a partir do <select> nativo pareado — chamar depois
   *  de qualquer mutação programática do <select> (append de <option>, `.val(...)`) que não passe
   *  por um re-render completo do HTML (que já sai sincronizado). */
  function refreshSearchSelect(selectId) {
    const $select = $('#' + selectId);
    const $details = $('.search-dropdown[data-for="' + selectId + '"]');
    if (!$select.length || !$details.length) return;
    const value = $select.val();
    const rows = $select.find('option').map(function () {
      const v = this.value || '';
      const label = $(this).text();
      const isSel = v !== '' && String(v) === String(value);
      return '<div class="search-dropdown-row' + (isSel ? ' is-selected' : '') + '" ' +
          'data-dd-value="' + esc(v) + '" data-dd-label="' + esc(label) + '">' +
        esc(label) +
      '</div>';
    }).get().join('');
    $details.find('[data-region=search-dropdown-items]').html(rows);
    $details.find('[data-region=search-dropdown-summary-text]').text($select.find('option:selected').text() || '');
  }

  /* Clique numa linha de single-select: sincroniza o <select> pareado, atualiza o rótulo/seleção
   * visual e fecha o painel. Linhas de multi-select (tags) não têm `data-dd-value` — o próprio
   * checkbox interno já cuida da própria mudança, então o guard abaixo as ignora. */
  $(document).on('click', '.search-dropdown-row[data-dd-value]', function () {
    const $row = $(this);
    const $details = $row.closest('.search-dropdown');
    const value = $row.attr('data-dd-value');
    const label = $row.attr('data-dd-label');
    $details.find('.search-dropdown-row').removeClass('is-selected');
    $row.addClass('is-selected');
    $details.find('[data-region=search-dropdown-summary-text]').text(label);
    const pairedId = $details.attr('data-for');
    if (pairedId) $('#' + pairedId).val(value).trigger('change');
    $details.removeAttr('open');
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
    tagsDropdownHtml: tagsDropdownHtml,
    refreshTagsDropdownLabel: refreshTagsDropdownLabel,
    appendTagRow: appendTagRow,
    searchSelectHtml: searchSelectHtml,
    refreshSearchSelect: refreshSearchSelect,
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
  window.appendTagRow = appendTagRow;
  window.searchSelectHtml = searchSelectHtml;
  window.refreshSearchSelect = refreshSearchSelect;
})();
