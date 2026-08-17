/**
 * Search dropdown: combobox single-select (categoria) + o painel flutuante/filtro compartilhado
 * com o multi-select de tags (`tags-dropdown.ts`) — ambos desenham linhas `.search-dropdown-row`
 * dentro de `.search-dropdown-panel`, e os listeners globais abaixo cobrem os dois.
 */
import $ from 'jquery';
import { esc } from '../../../_0_domain/format.ts';
import { icon } from '../icons.ts';

export interface SelectorButtonOptions {
  id?: string;
  active?: boolean;
  title?: string;
  value?: string;
  valueColor?: string;
  cls?: string;
  act?: string;
}

/** Extrato de contas / extrato do cartão, coluna esquerda. `cls`/`act` differ per screen
 * (statement.js data-act="select-account" vs card-statement.js data-act="select-card") — same
 * visual skin, different dispatch. */
export function selectorButtonHtml(opts: SelectorButtonOptions = {}): string {
  const active = !!opts.active;
  const cls = 'selector-btn' + (active ? ' is-active' : '') + (opts.cls ? ' ' + esc(opts.cls) : '');
  return (
    '<button type="button" class="' + cls + '" ' +
    'data-act="' + esc(opts.act || 'select-account') + '" data-id="' + esc(opts.id) + '">' +
    '<span>' + esc(opts.title) + '</span>' +
    '<span class="selector-btn-value" style="color:' + (opts.valueColor || 'var(--text-muted)') + ';">' + esc(opts.value) + '</span>' +
    '</button>'
  );
}

/** Opções de pele compartilhadas pelos dois dropdowns (tags e single-select): `floating` marca o
 * <details> pra o painel ser ancorado em position:fixed ao abrir; `compact` encolhe o <summary>
 * pra densidade de linha de tabela. */
export function floatingAttr(opts?: { floating?: boolean }): string {
  return opts && opts.floating ? ' data-floating="1"' : '';
}

export function summaryClass(opts?: { compact?: boolean }): string {
  return 'search-dropdown-summary' + (opts && opts.compact ? ' search-dropdown-summary-sm' : '');
}

function searchSelectRowHtml(value: string, label: string, selectedValue: string | null | undefined): string {
  const isSel = value !== '' && String(value) === String(selectedValue);
  return (
    '<div class="search-dropdown-row' + (isSel ? ' is-selected' : '') + '" ' +
    'data-dd-value="' + esc(value) + '" data-dd-label="' + esc(label) + '">' +
    esc(label) +
    '</div>'
  );
}

export interface SearchSelectItem {
  value: string;
  label: string;
}

export interface SearchSelectOptions {
  pairedSelectId?: string;
  lazy?: boolean;
  floating?: boolean;
  compact?: boolean;
  disabled?: boolean;
  title?: string;
}

/** Pele igual ao <select> de categoria (padrão 007): painel abre fora do fluxo (não empurra o
 * layout) e ganha uma busca com filtro ao-vivo.
 *  - `pairedSelectId`: id do <select> nativo — liga o overlay pra `refreshSearchSelect`
 *    re-sincronizar depois de uma mutação programática (ex.: "+ Nova categoria");
 *  - `lazy`: não emite as linhas agora; são materializadas do <select> pareado no primeiro
 *    `open` e devolvidas ao fechar. Exige `pairedSelectId`;
 *  - `floating`: painel ancorado em position:fixed (obrigatório dentro de um ancestral com
 *    overflow:scroll, senão o painel absoluto é recortado);
 *  - `compact`: <summary> na densidade de linha de tabela;
 *  - `disabled`: <summary> inerte (parcela travada, catálogo vazio);
 *  - `title`: tooltip do <summary>. */
export function searchSelectHtml(items: readonly SearchSelectItem[], selectedValue: string | null | undefined, key: string, opts: SearchSelectOptions = {}): string {
  const rows = opts.lazy ? '' : items.map((it) => searchSelectRowHtml(it.value, it.label, selectedValue)).join('');
  const selected = items.filter((it) => String(it.value) === String(selectedValue))[0];
  const summaryText = selected ? selected.label : (items[0] && items[0].label) || '';
  const forAttr = opts.pairedSelectId ? ' data-for="' + esc(opts.pairedSelectId) + '"' : '';
  const lazyAttr = opts.lazy ? ' data-lazy="1"' : '';
  const titleAttr = opts.title ? ' title="' + esc(opts.title) + '"' : '';
  const disabledAttr = opts.disabled ? ' data-disabled="1"' : '';
  return (
    '<details class="search-dropdown" data-region="search-dropdown" data-idx="' + esc(key) + '"' +
      forAttr + lazyAttr + floatingAttr(opts) + '>' +
      '<summary class="' + summaryClass(opts) + '" data-region="search-dropdown-summary"' + titleAttr + disabledAttr + '>' +
        '<span data-region="search-dropdown-summary-text" title="' + esc(summaryText) + '">' + esc(summaryText) + '</span>' +
        '<span class="search-dropdown-chevron">' + icon('chevronDown', 14) + '</span>' +
      '</summary>' +
      '<div class="search-dropdown-panel">' +
        '<input type="text" class="search-dropdown-search" data-region="search-dropdown-search" ' +
          'placeholder="Buscar..." autocomplete="off" />' +
        '<div class="search-dropdown-items" data-region="search-dropdown-items">' + rows + '</div>' +
        '<p class="search-dropdown-empty" data-region="search-dropdown-empty" style="display:none;">Nenhum resultado</p>' +
      '</div>' +
    '</details>'
  );
}

/** Re-desenha as linhas + rótulo do overlay a partir do <select> nativo pareado — chamar depois
 * de qualquer mutação programática do <select> que não passe por um re-render completo do HTML. */
export function refreshSearchSelect(selectId: string): void {
  const $select = $('#' + selectId);
  const $details = $('.search-dropdown[data-for="' + selectId + '"]');
  if (!$select.length || !$details.length) return;
  const label = $select.find('option:selected').text() || '';
  $details.find('[data-region=search-dropdown-summary-text]').text(label).attr('title', label);
  if ($details.attr('data-lazy') && !($details[0] as HTMLDetailsElement).open) return;
  const value = $select.val();
  const rows = $select
    .find('option')
    .map(function () {
      return searchSelectRowHtml((this as HTMLOptionElement).value || '', $(this).text(), value as string);
    })
    .get()
    .join('');
  $details.find('[data-region=search-dropdown-items]').html(rows);
}

const FLOATING_MIN_WIDTH = 240;
const FLOATING_GAP = 4;
/* Abrir o dropdown pode fazer o container rolar sozinho (o browser traz pra vista a linha
 * clicada / o campo focado) — sem esta janela de carência o listener de scroll fecharia o painel
 * no mesmo gesto que o abriu. Ela é armada no clique do <summary>, não no 'toggle': o evento de
 * scroll é despachado antes do 'toggle' (que o browser enfileira), então armar só lá chegaria
 * tarde — o painel já teria sido fechado. */
const SCROLL_GRACE_MS = 200;
let scrollGraceUntil = 0;

function armScrollGrace(): void {
  scrollGraceUntil = Date.now() + SCROLL_GRACE_MS;
}

function positionFloatingPanel(details: HTMLElement): void {
  const summary = details.querySelector('summary');
  const panel = details.querySelector<HTMLElement>('.search-dropdown-panel');
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

function resetSearchDropdown(details: HTMLElement): void {
  const input = details.querySelector<HTMLInputElement>('.search-dropdown-search');
  if (input) input.value = '';
  const panel = details.querySelector<HTMLElement>('.search-dropdown-panel');
  if (!panel) return;
  panel.classList.remove('is-floating');
  panel.style.left = panel.style.top = panel.style.width = '';
  const items = panel.querySelector('[data-region=search-dropdown-items]');
  // Lazy: devolve os nós das linhas — numa tabela de centenas de linhas, só o dropdown aberto
  // paga o custo do catálogo inteiro.
  if (items && details.getAttribute('data-lazy')) items.innerHTML = '';
  else if (items) {
    const rows = items.querySelectorAll('.search-dropdown-row-hidden');
    rows.forEach((row) => row.classList.remove('search-dropdown-row-hidden'));
  }
  const empty = panel.querySelector<HTMLElement>('[data-region=search-dropdown-empty]');
  if (empty) empty.style.display = 'none';
}

function closeOpenSearchDropdowns(except: HTMLElement | null): void {
  const open = document.querySelectorAll<HTMLDetailsElement>('details.search-dropdown[open]');
  open.forEach((d) => {
    if (d !== except) d.open = false;
  });
}

/** Assina os listeners globais (delegados no document) do search-dropdown: filtro ao-vivo,
 * posicionamento do painel flutuante, fechar ao clicar fora/ESC/rolar, e o clique de linha de
 * single-select. Chamar uma vez (via `initUi()`, Fase 7). */
export function installSearchDropdownHandlers(): void {
  $(document).on('input', '.search-dropdown-search', function () {
    const q = ($(this).val() as string).trim().toLowerCase();
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
   * since capture always walks document→target regardless of the event's bubbles flag. */
  document.addEventListener(
    'toggle',
    (e) => {
      const details = e.target as HTMLDetailsElement;
      if (!details || !details.matches || !details.matches('details.search-dropdown')) return;
      if (!details.open) {
        resetSearchDropdown(details);
        return;
      }

      closeOpenSearchDropdowns(details); // com painel flutuante, dois abertos ao mesmo tempo se sobrepõem
      const pairedId = details.getAttribute('data-for');
      if (details.getAttribute('data-lazy') && pairedId) refreshSearchSelect(pairedId);
      if (details.getAttribute('data-floating')) positionFloatingPanel(details);
      armScrollGrace();
      const input = details.querySelector<HTMLInputElement>('.search-dropdown-search');
      if (input) input.focus({ preventScroll: true }); // teclado cai direto na busca ao abrir
    },
    true,
  );

  /* <summary> inerte: <details> não tem atributo disabled (parcela travada, catálogo vazio). */
  $(document).on('click', '.search-dropdown-summary[data-disabled]', (e) => e.preventDefault());

  $(document).on('click', '.search-dropdown-summary', armScrollGrace);

  $(document).on('click', (e) => {
    if ($(e.target).closest('details.search-dropdown').length) return;
    closeOpenSearchDropdowns(null);
  });

  /* Captura: o handler de ESC do modal está registrado antes deste (bolha), então só interceptando
   * antes dá pra fechar o dropdown sem levar o modal junto. */
  document.addEventListener(
    'keydown',
    (e) => {
      if (e.key !== 'Escape') return;
      if (!document.querySelector('details.search-dropdown[open]')) return;
      closeOpenSearchDropdowns(null);
      e.stopPropagation();
    },
    true,
  );

  /* Captura: a rolagem que importa é a do container interno (tabela/modal), que não borbulha até o
   * document. Rolar dentro do próprio painel não conta — é a lista de opções. */
  document.addEventListener(
    'scroll',
    (e) => {
      if (Date.now() < scrollGraceUntil) return;
      const target = e.target as HTMLElement | null;
      if (target && target.closest && target.closest('.search-dropdown-panel')) return;
      closeOpenSearchDropdowns(null);
    },
    true,
  );

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
    $details.find('[data-region=search-dropdown-summary-text]').text(label || '').attr('title', label || '');
    const pairedId = $details.attr('data-for');
    if (pairedId) $('#' + pairedId).val(value || '').trigger('change');
    $details.removeAttr('open');
  });
}
