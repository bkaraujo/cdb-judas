import $ from 'jquery';
import { esc, sortByName } from '../../core/kernel/_0_domain/format.ts';
import type { Tag } from '../../core/kernel/_0_domain/tag.ts';
import { bindSwatches, byId, formModal, modalText } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { cachePage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { deleteWithLinkedFallback, pluralTransactions } from '../../core/kernel/_2_infrastructure/primary/delete-dialog.ts';
import { PALETTE } from '../../core/kernel/_2_infrastructure/primary/pickers.ts';
import { btn, rowActionBtn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { colorNameFieldHtml } from '../../core/kernel/_2_infrastructure/primary/ui/color-name-field.ts';
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import type { TagService } from './service.ts';

interface TagsPageState extends PageState {
  tags: Tag[];
}

function renderChip(t: Tag): JQuery {
  const hasColor = !!t.color;
  const color = hasColor ? (t.color as string) : '';

  const $chip = $(
    '<div class="tag-chip' + (hasColor ? ' has-color' : '') + '" data-id="' + esc(t.id) + '" ' +
      (hasColor ? 'data-color="' + esc(color) + '" ' : '') + '>' +
      '<span class="tag-chip-dot"' + (hasColor ? ' style="background:' + esc(color) + ';"' : '') + '></span>' +
      '<span class="tag-chip-name">#' + esc(t.name) + '</span>' +
      '<span class="tag-chip-actions" data-region="row-actions"></span>' +
    '</div>',
  );

  $chip
    .find('[data-region=row-actions]')
    .append(rowActionBtn('edit', 'Editar', t.id, { size: 22, iconSize: 12 }))
    .append(rowActionBtn('trash', 'Excluir', t.id, { size: 22, iconSize: 12, danger: true }));

  // Hover tints border/background for colored chips (uncolored chips are handled by CSS :hover
  // alone); leaving values empty on mouseleave restores the class-driven default.
  if (hasColor) {
    $chip.on('mouseenter', () => {
      $chip.css('border-color', color);
      $chip.css('background', color + '18');
    });
    $chip.on('mouseleave', () => {
      $chip.css('border-color', '');
      $chip.css('background', '');
    });
  }

  return $chip;
}

export function createTagsPage(service: TagService): Page {
  let state: TagsPageState | null = null;

  function findTag(id: string): Tag | null {
    return state ? byId(state.tags, id) : null;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $header = pageHeader({
      title: 'Tags',
      actions: btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Tag', attrs: 'data-act="new"' }),
    });

    let $body: JQuery;
    if (!state.tags.length) {
      $body = $(emptyState({ icon: 'tag', title: 'Nenhuma tag cadastrada', desc: 'Clique em "Nova Tag" para começar.' }));
    } else {
      const sorted = state.tags.slice().sort(sortByName);
      const $card = $('<div class="card"></div>');
      const $chips = $('<div data-region="chips" style="display:flex;flex-wrap:wrap;gap:10px;"></div>');
      sorted.forEach((t) => $chips.append(renderChip(t)));
      $card.append($chips);
      $body = $card;
    }

    $root.empty().append($header).append($body);
  }

  function openFormModal(existing: Tag | null): void {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = { name: 'tag-name-' + uniq, color: 'tag-color-' + uniq };
    const initial = {
      name: isEdit ? existing?.name || '' : '',
      color: isEdit ? existing?.color || PALETTE.swatches[0] : PALETTE.swatches[0],
    };

    const bodyHtml =
      '<form data-form="tag" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            colorNameFieldHtml({ colorId: ids.color, nameId: ids.name, color: initial.color, nameValue: initial.name, placeholder: 'Ex: mensal, fixo...', swatches: PALETTE.swatches }) +
          '</div>' +
        '</div>' +
      '</form>';

    const m = formModal({
      title: isEdit ? 'Editar Tag' : 'Nova Tag',
      formName: 'tag',
      body: bodyHtml,
      autofocus: 'input[name=name]',
      onSubmit: ($form) => {
        const $name = $form.find('input[name=name]');
        const name = (($name.val() as string) || '').trim();
        if (!name) {
          $name.trigger('focus');
          return null;
        }
        const payload = { name, color: (($form.find('input[name=color]').val() as string) || PALETTE.swatches[0]) };
        return isEdit ? service.update((existing as Tag).id, payload) : service.create(payload);
      },
      success: () => (isEdit ? 'Tag atualizada' : 'Tag criada'),
      failure: 'Falha ao salvar tag',
      // SSE UPSERT will refresh CacheStore → TagService.onChange → re-render.
    });

    bindSwatches(m, m.$body.find('input[name=color]'));
  }

  function openDeleteModal(target: Tag): void {
    const nameHtml = '<strong>#' + esc(target.name) + '</strong>';
    deleteWithLinkedFallback({
      title: 'Excluir Tag',
      body: modalText('Tem certeza que deseja excluir a tag ' + nameHtml + '? As transações vinculadas permanecem — apenas perdem a marcação. Esta ação não pode ser desfeita.'),
      remove: () => service.remove(target.id),
      success: 'Tag removida',
      failure: 'Falha ao excluir tag',
      // SSE DELETE will refresh CacheStore → TagService.onChange → re-render.
      linked: {
        title: 'Tag com transações vinculadas',
        intro: (count) => 'A tag <strong>#' + esc(target.name) + '</strong> tem ' + pluralTransactions(count) + '. Escolha o que fazer:',
        options: () => {
          const otherTags = (state?.tags || []).filter((t) => String(t.id) !== String(target.id));
          return [
            {
              value: 'MOVE', label: 'Mover para outra tag',
              hint: 'As transações desta tag passam a usar a tag escolhida.',
              choices: otherTags.map((t) => ({ value: t.id, label: '#' + t.name })),
            },
            { value: 'DETACH', label: 'Apenas desvincular', hint: 'Remove só a marcação; as transações permanecem intactas.' },
          ];
        },
        dispatch: (choice) => service.remove(target.id, { strategy: choice.strategy as 'MOVE' | 'DETACH', targetId: choice.targetId || undefined }),
        success: 'Tag removida',
        failure: 'Falha ao excluir tag',
      },
    });
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.tags', '[data-act=new]', () => openFormModal(null));
    $root.on('click.tags', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const t = findTag(id);
      if (t) openFormModal(t);
    });
    $root.on('click.tags', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const t = findTag(id);
      if (t) openDeleteModal(t);
    });
  }

  return cachePage<TagsPageState>({
    ns: '.tags',
    state: () => {
      state = { tags: [] };
      return state;
    },
    sync: (s) => {
      s.tags = service.listCached();
    },
    subscribe: (cb) => service.onChange(cb),
    render,
    bind: bindRoot,
  });
}
