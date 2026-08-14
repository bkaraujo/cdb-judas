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

  /* ---- Page header ---- */
  function pageHeader(opts) {
    opts = opts || {};
    const $titleBlock = opts.subtitle
      ? $('<div></div>').append(
          $('<h1></h1>').text(opts.title || ''),
          $('<p style="font-size:12px;color:var(--text-muted);margin-top:2px;"></p>').text(opts.subtitle)
        )
      : $('<h1></h1>').text(opts.title || '');

    // grid fixo title(esquerda) / actions(centro) / nav(direita) — ver .page-header em app.css.
    const $el = $('<div class="page-header"></div>').append($titleBlock);
    if (opts.actions) $('<div class="page-header-actions"></div>').append(opts.actions).appendTo($el);
    if (opts.nav)     $('<div class="page-header-nav"></div>').append(opts.nav).appendTo($el);
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

  /* Opções de pele compartilhadas pelos dois dropdowns (tags e single-select): `floating` marca o
   * <details> pra o painel ser ancorado em position:fixed ao abrir (ver o listener de 'toggle');
   * `compact` encolhe o <summary> pra densidade de linha de tabela. */
  function floatingAttr(opts) {
    return (opts && opts.floating) ? ' data-floating="1"' : '';
  }

  function summaryClass(opts) {
    return 'search-dropdown-summary' + ((opts && opts.compact) ? ' search-dropdown-summary-sm' : '');
  }

  /** Pele igual ao <select> de categoria (padrão único desde 007 — ver docs/frontend/category-tag-pickers.md):
   *  painel abre fora do fluxo (não empurra o layout) e ganha uma busca com filtro ao-vivo.
   *  opts.floating/opts.compact: ver searchSelectHtml — dentro de um ancestral com overflow:scroll
   *  (a tabela de import), `floating` é obrigatório, senão o painel absoluto é recortado. */
  function tagsDropdownHtml(selectedIds, key, opts) {
    const tags = window.App.CacheStore.tags();
    if (!tags.length) return '<span style="font-size:11px;color:var(--text-muted);">Sem tags</span>';
    const sel = (selectedIds || []).map(String);
    const locked = !!(opts && opts.disabled);
    const disabledAttr = locked ? ' disabled' : '';
    const items = tags.map(function (t) {
      const color = t.color || 'var(--text-muted)';
      const checked = sel.indexOf(String(t.id)) !== -1 ? ' checked' : '';
      return '<label class="search-dropdown-row">' +
        '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(t.id) + '"' +
          checked + disabledAttr + ' style="cursor:pointer;" />' +
        '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
        esc(t.name) +
      '</label>';
    }).join('');
    const titleAttr = (opts && opts.title) ? ' title="' + esc(opts.title) + '"' : '';
    const summaryDisabledAttr = locked ? ' data-disabled="1"' : '';

    return (
      '<details class="search-dropdown" data-region="tags-dropdown" data-idx="' + esc(key) + '"' +
          floatingAttr(opts) + '>' +
        '<summary class="' + summaryClass(opts) + '" data-region="tags-summary"' + titleAttr + summaryDisabledAttr + '>' +
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
   *  monta a option na mão em vez de esperar o cache). Quem chama ainda precisa dar push no próprio
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

  /* ---- Indicador de tags da linha (extrato, fatura, lançamentos) ----
   * Célula de largura fixa: com tags vira um ícone que abre o painel no hover; sem tags fica
   * vazia, mas ocupando o mesmo espaço — senão as colunas dançam entre linhas com e sem tag.
   * Resolve contra o catálogo em cache (nenhuma requisição): id órfão não conta, então uma tag
   * apagada com DETACH não deixa um ícone que abre um painel vazio.
   */
  const TAG_FLAG_WIDTH = 16;

  function resolvedTags(tagIds) {
    return window.Domain.Tag.resolve(tagIds, window.App.CacheStore.tags());
  }

  function tagFlagHtml(tagIds) {
    const cell = 'display:inline-flex;align-items:center;justify-content:center;' +
      'width:' + TAG_FLAG_WIDTH + 'px;flex-shrink:0;';
    const tags = resolvedTags(tagIds);
    if (!tags.length) return '<span style="' + cell + '"></span>';
    const ids = tags.map(function (t) { return String(t.id); }).join(',');
    return '<span class="tag-flag" style="' + cell + '" data-tag-ids="' + esc(ids) + '" ' +
      'aria-label="' + esc(tagsCountLabel(tags.length)) + '">' +
      window.icon('tag', 13) +
    '</span>';
  }

  /* Painel do indicador: position:fixed ancorado no ícone, e não um filho position:absolute, porque
     toda linha de lançamento mora dentro de um card com overflow:auto — um painel absoluto seria
     recortado na primeira e na última linha. O preço é que ele não acompanha rolagem: por isso o
     listener de scroll em captura o fecha. */
  let $tagPopover = null;

  function hideTagPopover() {
    if (!$tagPopover) return; // guard: o listener de scroll dispara o tempo todo
    $tagPopover.remove();
    $tagPopover = null;
  }

  function showTagPopover($flag) {
    hideTagPopover();
    const ids = String($flag.attr('data-tag-ids') || '').split(',').filter(Boolean);
    const tags = resolvedTags(ids);
    if (!tags.length) return;

    const rows = tags.map(function (t) {
      const color = t.color || 'var(--text-muted)';
      return '<tr>' +
        '<td><span class="tag-pop-dot" style="background:' + esc(color) + ';"></span></td>' +
        '<td>' + esc('#' + (t.name || '')) + '</td>' +
      '</tr>';
    }).join('');
    const $pop = $tagPopover = $('<div id="tag-popover"><table>' + rows + '</table></div>').appendTo('body');

    // Preferência à esquerda do ícone (a direita da linha é ocupada por valor/saldo/ações);
    // sem espaço, vai pra direita. Clamp vertical pra não vazar da viewport.
    const r = $flag[0].getBoundingClientRect();
    const w = $pop.outerWidth();
    const h = $pop.outerHeight();
    const gap = 8;
    const left = (r.left - gap - w >= 4) ? (r.left - gap - w) : Math.min(r.right + gap, window.innerWidth - w - 4);
    const top = Math.max(4, Math.min(r.top + r.height / 2 - h / 2, window.innerHeight - h - 4));
    $pop.css({ left: Math.max(4, left) + 'px', top: top + 'px' });
  }

  $(document)
    .on('mouseenter.tagflag', '.tag-flag', function () { showTagPopover($(this)); })
    .on('mouseleave.tagflag', '.tag-flag', hideTagPopover);
  // Captura: a rolagem que importa é a do card interno, que não borbulha até o document.
  document.addEventListener('scroll', hideTagPopover, true);

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

  /* Painel flutuante: dentro de um ancestral com overflow:scroll (a tabela de import), um painel
   * position:absolute é recortado. Ancorar em position:fixed contorna — mesma técnica do popover
   * do indicador de tags acima. Preço: não acompanha rolagem, por isso o listener de scroll em
   * captura fecha o dropdown. Mede depois de aberto (o <details> já renderizou o painel). */
  const FLOATING_MIN_WIDTH = 240;
  const FLOATING_GAP = 4;
  /* Abrir o dropdown pode fazer o container rolar sozinho (o browser traz pra vista a linha
   * clicada / o campo focado) — sem esta janela de carência o listener de scroll fecharia o painel
   * no mesmo gesto que o abriu. Ela é armada no clique do <summary>, não no 'toggle': o evento de
   * scroll é despachado antes do 'toggle' (que o browser enfileira), então armar só lá chegaria
   * tarde — o painel já teria sido fechado. */
  const SCROLL_GRACE_MS = 200;
  let scrollGraceUntil = 0;

  function armScrollGrace() {
    scrollGraceUntil = Date.now() + SCROLL_GRACE_MS;
  }

  function positionFloatingPanel(details) {
    const summary = details.querySelector('summary');
    const panel = details.querySelector('.search-dropdown-panel');
    if (!summary || !panel) return;
    panel.classList.add('is-floating');
    const r = summary.getBoundingClientRect();
    const width = Math.min(Math.max(r.width, FLOATING_MIN_WIDTH), window.innerWidth - 8);
    panel.style.width = width + 'px';
    const h = panel.offsetHeight;
    // Abaixo do campo; sem espaço, joga pra cima; sem espaço em lugar nenhum, cola na viewport.
    let top = r.bottom + FLOATING_GAP;
    if (top + h > window.innerHeight - 4) {
      const above = r.top - FLOATING_GAP - h;
      top = above >= 4 ? above : Math.max(4, window.innerHeight - h - 4);
    }
    panel.style.left = Math.max(4, Math.min(r.left, window.innerWidth - width - 4)) + 'px';
    panel.style.top = top + 'px';
  }

  function resetSearchDropdown(details) {
    const input = details.querySelector('.search-dropdown-search');
    if (input) input.value = '';
    const panel = details.querySelector('.search-dropdown-panel');
    if (!panel) return;
    panel.classList.remove('is-floating');
    panel.style.left = panel.style.top = panel.style.width = '';
    const items = panel.querySelector('[data-region=search-dropdown-items]');
    // Lazy: devolve os nós das linhas — numa tabela de centenas de linhas, só o dropdown aberto
    // paga o custo do catálogo inteiro.
    if (items && details.getAttribute('data-lazy')) items.innerHTML = '';
    else if (items) {
      const rows = items.querySelectorAll('.search-dropdown-row-hidden');
      Array.prototype.forEach.call(rows, function (row) { row.classList.remove('search-dropdown-row-hidden'); });
    }
    const empty = panel.querySelector('[data-region=search-dropdown-empty]');
    if (empty) empty.style.display = 'none';
  }

  function closeOpenSearchDropdowns(except) {
    const open = document.querySelectorAll('details.search-dropdown[open]');
    Array.prototype.forEach.call(open, function (d) { if (d !== except) d.open = false; });
  }

  /* `<details>` fires 'toggle' on itself without bubbling (unlike almost every other DOM event),
   * so `$(document).on('toggle', ...)` delegation never sees it — a capture-phase listener does,
   * since capture always walks document→target regardless of the event's bubbles flag. */
  document.addEventListener('toggle', function (e) {
    const details = e.target;
    if (!details || !details.matches || !details.matches('details.search-dropdown')) return;
    if (!details.open) { resetSearchDropdown(details); return; }

    closeOpenSearchDropdowns(details); // com painel flutuante, dois abertos ao mesmo tempo se sobrepõem
    const pairedId = details.getAttribute('data-for');
    if (details.getAttribute('data-lazy') && pairedId) refreshSearchSelect(pairedId);
    if (details.getAttribute('data-floating')) positionFloatingPanel(details);
    armScrollGrace();
    const input = details.querySelector('.search-dropdown-search');
    if (input) input.focus({ preventScroll: true }); // teclado cai direto na busca ao abrir
  }, true);

  /* <summary> inerte: <details> não tem atributo disabled (parcela travada, catálogo vazio). */
  $(document).on('click', '.search-dropdown-summary[data-disabled]', function (e) { e.preventDefault(); });

  $(document).on('click', '.search-dropdown-summary', armScrollGrace);

  $(document).on('click', function (e) {
    if ($(e.target).closest('details.search-dropdown').length) return;
    closeOpenSearchDropdowns(null);
  });

  /* Captura: o handler de ESC do modal está registrado antes deste (bolha), então só interceptando
   * antes dá pra fechar o dropdown sem levar o modal junto. */
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    if (!document.querySelector('details.search-dropdown[open]')) return;
    closeOpenSearchDropdowns(null);
    e.stopPropagation();
  }, true);

  /* Captura: a rolagem que importa é a do container interno (tabela/modal), que não borbulha até o
   * document. Rolar dentro do próprio painel não conta — é a lista de opções. */
  document.addEventListener('scroll', function (e) {
    if (Date.now() < scrollGraceUntil) return;
    const target = e.target;
    if (target && target.closest && target.closest('.search-dropdown-panel')) return;
    closeOpenSearchDropdowns(null);
  }, true);

  function searchSelectRowHtml(value, label, selectedValue) {
    const isSel = value !== '' && String(value) === String(selectedValue);
    return '<div class="search-dropdown-row' + (isSel ? ' is-selected' : '') + '" ' +
        'data-dd-value="' + esc(value) + '" data-dd-label="' + esc(label) + '">' +
      esc(label) +
    '</div>';
  }

  /** Combobox single-select (categoria): desenha um <select> nativo (escondido, mantém toda a
   *  leitura/escrita existente por `.val()`) + este overlay com a mesma pele/filtro do dropdown
   *  de tags. Opções:
   *   - `pairedSelectId`: id do <select> nativo — liga o overlay pra `refreshSearchSelect`
   *     re-sincronizar depois de uma mutação programática (ex.: "+ Nova categoria");
   *   - `lazy`: não emite as linhas agora; são materializadas do <select> pareado no primeiro
   *     `open` e devolvidas ao fechar. Numa tabela de centenas de linhas (import), emitir as
   *     linhas de todo mundo dobraria o DOM de opções à toa. Exige `pairedSelectId`;
   *   - `floating`: painel ancorado em position:fixed (obrigatório dentro de um ancestral com
   *     overflow:scroll, senão o painel absoluto é recortado);
   *   - `compact`: <summary> na densidade de linha de tabela;
   *   - `disabled`: <summary> inerte (parcela travada, catálogo vazio);
   *   - `title`: tooltip do <summary> (o rótulo selecionado já vai no title do span interno, pra
   *     continuar legível quando `compact` o trunca com reticências). */
  function searchSelectHtml(items, selectedValue, key, opts) {
    opts = opts || {};
    const rows = opts.lazy ? '' : items.map(function (it) {
      return searchSelectRowHtml(it.value, it.label, selectedValue);
    }).join('');
    const selected = items.filter(function (it) { return String(it.value) === String(selectedValue); })[0];
    const summaryText = selected ? selected.label : ((items[0] && items[0].label) || '');
    const forAttr = opts.pairedSelectId ? ' data-for="' + esc(opts.pairedSelectId) + '"' : '';
    const lazyAttr = opts.lazy ? ' data-lazy="1"' : '';
    const titleAttr = opts.title ? ' title="' + esc(opts.title) + '"' : '';
    const disabledAttr = opts.disabled ? ' data-disabled="1"' : '';
    return (
      '<details class="search-dropdown" data-region="search-dropdown" data-idx="' + esc(key) + '"' +
          forAttr + lazyAttr + floatingAttr(opts) + '>' +
        '<summary class="' + summaryClass(opts) + '" data-region="search-dropdown-summary"' +
            titleAttr + disabledAttr + '>' +
          '<span data-region="search-dropdown-summary-text" title="' + esc(summaryText) + '">' +
            esc(summaryText) +
          '</span>' +
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
   *  por um re-render completo do HTML (que já sai sincronizado). O rótulo é sempre atualizado; as
   *  linhas, só quando estão materializadas (overlay não-lazy, ou lazy já aberto) — é este mesmo
   *  caminho que materializa o painel lazy na abertura. */
  function refreshSearchSelect(selectId) {
    const $select = $('#' + selectId);
    const $details = $('.search-dropdown[data-for="' + selectId + '"]');
    if (!$select.length || !$details.length) return;
    const label = $select.find('option:selected').text() || '';
    $details.find('[data-region=search-dropdown-summary-text]').text(label).attr('title', label);
    if ($details.attr('data-lazy') && !$details[0].open) return;
    const value = $select.val();
    const rows = $select.find('option').map(function () {
      return searchSelectRowHtml(this.value || '', $(this).text(), value);
    }).get().join('');
    $details.find('[data-region=search-dropdown-items]').html(rows);
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
    $details.find('[data-region=search-dropdown-summary-text]').text(label).attr('title', label);
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
    pageHeader: pageHeader,
    tabs: tabs,
    modal: modal,
    toast: toast,
    tagsDropdownHtml: tagsDropdownHtml,
    refreshTagsDropdownLabel: refreshTagsDropdownLabel,
    appendTagRow: appendTagRow,
    tagFlagHtml: tagFlagHtml,
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
  window.pageHeader = pageHeader;
  window.tabs       = tabs;
  window.modal      = modal;
  window.toast      = toast;
  window.tagsDropdownHtml = tagsDropdownHtml;
  window.refreshTagsDropdownLabel = refreshTagsDropdownLabel;
  window.appendTagRow = appendTagRow;
  window.tagFlagHtml = tagFlagHtml;
  window.searchSelectHtml = searchSelectHtml;
  window.refreshSearchSelect = refreshSearchSelect;
})();
