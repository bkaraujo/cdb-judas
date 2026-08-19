import $ from 'jquery';
import type { Tag } from '@/core/kernel/_0_domain/tag.ts';
import * as TagDomain from '@/core/kernel/_0_domain/tag.ts';
import { esc } from '@/core/kernel/_0_domain/format.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { floatingAttr, summaryClass } from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';

function tagsCountLabel(count: number): string {
  if (!count) return 'Nenhuma tag';
  return count + (count === 1 ? ' tag' : ' tags');
}

export interface TagsDropdownOptions {
  disabled?: boolean;
  title?: string;
  floating?: boolean;
  compact?: boolean;
}

/** Pele igual ao <select> de categoria (padrão único desde 007). `tags`: catálogo completo (ex.:
 * `cacheStore.tags()`) — thread explícito em vez de ler um cache global. opts.floating/compact:
 * ver `search-select.ts` — dentro de um ancestral com overflow:scroll (a tabela de import),
 * `floating` é obrigatório, senão o painel absoluto é recortado. */
export function tagsDropdownHtml(tags: readonly Tag[], selectedIds: readonly string[] | null | undefined, key: string, opts: TagsDropdownOptions = {}): string {
  if (!tags.length) return '<span style="font-size:11px;color:var(--text-muted);">Sem tags</span>';
  const sel = (selectedIds || []).map(String);
  const locked = !!opts.disabled;
  const disabledAttr = locked ? ' disabled' : '';
  const items = tags
    .map((t) => {
      const color = t.color || 'var(--text-muted)';
      const checked = sel.indexOf(String(t.id)) !== -1 ? ' checked' : '';
      return (
        '<label class="search-dropdown-row">' +
        '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(t.id) + '"' + checked + disabledAttr + ' style="cursor:pointer;" />' +
        '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
        esc(t.name) +
        '</label>'
      );
    })
    .join('');
  const titleAttr = opts.title ? ' title="' + esc(opts.title) + '"' : '';
  const summaryDisabledAttr = locked ? ' data-disabled="1"' : '';

  return (
    '<details class="search-dropdown" data-region="tags-dropdown" data-idx="' + esc(key) + '"' + floatingAttr(opts) + '>' +
      '<summary class="' + summaryClass(opts) + '" data-region="tags-summary"' + titleAttr + summaryDisabledAttr + '>' +
        '<span data-region="tags-summary-text">' + esc(tagsCountLabel(sel.length)) + '</span>' +
        '<span class="search-dropdown-chevron">' + icon('chevronDown', 14) + '</span>' +
      '</summary>' +
      '<div class="search-dropdown-panel">' +
        '<input type="text" class="search-dropdown-search" data-region="search-dropdown-search" placeholder="Buscar tag..." autocomplete="off" />' +
        '<div class="search-dropdown-items" data-region="search-dropdown-items">' + items + '</div>' +
        '<p class="search-dropdown-empty" data-region="search-dropdown-empty" style="display:none;">Nenhuma tag encontrada</p>' +
      '</div>' +
    '</details>'
  );
}

/** Atualiza o rótulo de contagem do <summary> mais próximo de um checkbox de tag alterado —
 * chamar depois de mutar o array de tagIds do chamador, dentro do handler de 'change'. */
export function refreshTagsDropdownLabel($checkbox: JQuery): void {
  const $details = $checkbox.closest('details');
  const $summary = $details.find('> summary[data-region=tags-summary]');
  const count = $details.find('[data-tag-check]:checked').length;
  const label = tagsCountLabel(count);
  const $text = $summary.find('> [data-region=tags-summary-text]');
  if ($text.length) $text.text(label);
  else $summary.text(label);
}

/** Insere (ou marca, se já presente) uma tag recém-criada no dropdown `key` — contorna o lag do
 * SSE até o CacheStore atualizar. Quem chama ainda precisa dar push no próprio array de ids
 * selecionados, este helper só re-desenha. */
export function appendTagRow(key: string, tag: Tag): void {
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
      '<input type="checkbox" data-tag-check data-idx="' + esc(key) + '" data-tag-id="' + esc(id) + '" checked style="cursor:pointer;" />' +
      '<span style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>' +
      esc(tag.name || '') +
    '</label>',
  );
  $items.append($row);
  refreshTagsDropdownLabel($row.find('[data-tag-check]'));
}

/** Indicador de tags da linha (extrato, fatura, lançamentos): célula de largura fixa, com tags
 * vira um ícone que abre o painel no hover; sem tags fica vazia, mas ocupando o mesmo espaço —
 * senão as colunas dançam entre linhas com e sem tag. Resolve contra o catálogo em cache (nenhuma
 * requisição): id órfão não conta. */
const TAG_FLAG_WIDTH = 16;

export function tagFlagHtml(tags: readonly Tag[], tagIds: readonly string[] | null | undefined): string {
  const cell = 'display:inline-flex;align-items:center;justify-content:center;width:' + TAG_FLAG_WIDTH + 'px;flex-shrink:0;';
  const resolved = TagDomain.resolve(tagIds, tags);
  if (!resolved.length) return '<span style="' + cell + '"></span>';
  const ids = resolved.map((t) => String(t.id)).join(',');
  return (
    '<span class="tag-flag" style="' + cell + '" data-tag-ids="' + esc(ids) + '" aria-label="' + esc(tagsCountLabel(resolved.length)) + '">' +
    icon('tag', 13) +
    '</span>'
  );
}

/* Painel do indicador: position:fixed ancorado no ícone, e não um filho position:absolute, porque
 * toda linha de lançamento mora dentro de um card com overflow:auto — um painel absoluto seria
 * recortado na primeira e na última linha. O preço é que ele não acompanha rolagem: por isso o
 * listener de scroll em captura o fecha. */
let $tagPopover: JQuery | null = null;

function hideTagPopover(): void {
  if (!$tagPopover) return; // guard: o listener de scroll dispara o tempo todo
  $tagPopover.remove();
  $tagPopover = null;
}

function showTagPopover($flag: JQuery, tags: readonly Tag[]): void {
  hideTagPopover();
  const ids = String($flag.attr('data-tag-ids') || '').split(',').filter(Boolean);
  const resolved = TagDomain.resolve(ids, tags);
  if (!resolved.length) return;

  const rows = resolved
    .map((t) => {
      const color = t.color || 'var(--text-muted)';
      return '<tr><td><span class="tag-pop-dot" style="background:' + esc(color) + ';"></span></td><td>' + esc('#' + (t.name || '')) + '</td></tr>';
    })
    .join('');
  const $pop = ($tagPopover = $('<div id="tag-popover"><table>' + rows + '</table></div>').appendTo('body'));

  // Preferência à esquerda do ícone (a direita da linha é ocupada por valor/saldo/ações);
  // sem espaço, vai pra direita. Clamp vertical pra não vazar da viewport.
  const r = ($flag[0] as HTMLElement).getBoundingClientRect();
  const w = $pop.outerWidth() || 0;
  const h = $pop.outerHeight() || 0;
  const gap = 8;
  const left = r.left - gap - w >= 4 ? r.left - gap - w : Math.min(r.right + gap, window.innerWidth - w - 4);
  const top = Math.max(4, Math.min(r.top + r.height / 2 - h / 2, window.innerHeight - h - 4));
  $pop.css({ left: Math.max(4, left) + 'px', top: top + 'px' });
}

/** Assina os listeners globais do indicador de tags (hover mostra popover, scroll fecha). Chamar
 * uma vez (via `initUi()`, Fase 7) com um acessor `tags` fechado sobre o CacheStore corrente. */
export function installTagFlagHandlers(getTags: () => readonly Tag[]): void {
  $(document)
    .on('mouseenter.tagflag', '.tag-flag', function () {
      showTagPopover($(this), getTags());
    })
    .on('mouseleave.tagflag', '.tag-flag', hideTagPopover);
  // Captura: a rolagem que importa é a do card interno, que não borbulha até o document.
  document.addEventListener('scroll', hideTagPopover, true);
}
