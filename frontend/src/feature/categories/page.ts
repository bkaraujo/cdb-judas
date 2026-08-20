/** pages/categories — Categorias (CRUD com árvore pai/filho por natureza). */
import $ from 'jquery';
import { esc, sortByName } from '@/core/kernel/_0_domain/format.ts';
import type { Category } from '@/core/kernel/_0_domain/category.ts';
import { bindRecordActions, byId, formModal, modalText, runMutation } from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import { deleteWithLinkedFallback, pluralTransactions } from '@/core/kernel/_2_infrastructure/primary/delete-dialog.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { cachePage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { badge } from '@/core/kernel/_2_infrastructure/primary/ui/badge.ts';
import type { Modal } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { btn, rowActionBtn } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import type { CategoryService } from '@/feature/categories/service.ts';

const NATURE_EXPENSE = 'EXPENSE';
const NATURE_REVENUE = 'INCOME';

interface CategoriesPageState extends PageState {
  tab: 'expense' | 'income';
  categories: Category[];
  expanded: Record<string, boolean>;
}

function reactivateBtn(id: string): JQuery {
  return rowActionBtn('eye', 'Reativar', id, { act: 'reactivate' });
}

function tabButton(id: 'expense' | 'income', label: string, iconName: string, count: number, active: boolean): string {
  const color = id === 'expense' ? 'expense' : 'income';
  const bg = active ? 'var(--' + color + '-light)' : 'transparent';
  const fg = active ? 'var(--' + color + ')' : 'var(--text-secondary)';
  return (
    '<button type="button" data-act="set-tab" data-tab="' + esc(id) + '" ' +
    'style="display:flex;align-items:center;justify-content:space-between;' +
    'padding:10px 12px;border-radius:var(--radius-sm);' +
    'background:' + bg + ';color:' + fg + ';' +
    'border:none;cursor:pointer;font-weight:600;font-size:13px;' +
    'transition:all var(--transition);">' +
    '<span style="display:flex;align-items:center;gap:8px;">' + icon(iconName, 15) + esc(label) + '</span>' +
    badge(String(count), color) +
    '</button>'
  );
}

export function createCategoriesPage(service: CategoryService): Page {
  let state: CategoriesPageState | null = null;

  function currentNature(): string {
    return state?.tab === 'expense' ? NATURE_EXPENSE : NATURE_REVENUE;
  }

  function natureRoots(nature: string): Category[] {
    return (state?.categories || [])
      .filter((c) => c.nature === nature && !c.parentId)
      .slice()
      .sort(sortByName);
  }

  function childrenOf(parentId: string): Category[] {
    return (state?.categories || [])
      .filter((c) => c.parentId === parentId)
      .slice()
      .sort(sortByName);
  }

  function findCategory(id: string): Category | null {
    return state ? byId(state.categories, id) : null;
  }

  function renderRootCard(root: Category, children: Category[]): JQuery {
    const hasChildren = children.length > 0;
    const isExpanded = !!state?.expanded[root.id];
    const showChildren = hasChildren && isExpanded;

    const $card = $('<div class="card" style="padding:0;overflow:hidden;"></div>');

    const chevronHtml = hasChildren ? icon(isExpanded ? 'chevronDown' : 'chevronRight', 14) : '<span style="display:inline-block;width:14px;"></span>';
    const rootAttrs = hasChildren ? ' data-act="toggle" data-id="' + esc(root.id) + '" style="cursor:pointer;' : ' style="cursor:default;';
    const rootActive = root.active !== false;
    const $rootRow = $(
      '<div data-row="root" data-id="' + esc(root.id) + '"' + rootAttrs +
        'display:flex;align-items:center;justify-content:space-between;' +
        'padding:13px 20px;border-bottom:' + (showChildren ? '1px solid var(--border-light)' : 'none') + ';' +
        (rootActive ? '' : 'opacity:0.55;') +
        'transition:background var(--transition);">' +
        '<div style="display:flex;align-items:center;gap:10px;min-width:0;">' +
          '<span style="display:inline-flex;align-items:center;color:var(--text-muted);">' + chevronHtml + '</span>' +
          '<span style="font-size:13px;font-weight:600;color:var(--text-primary);text-transform:uppercase;letter-spacing:0.02em;">' + esc(root.name) + '</span>' +
          (hasChildren ? badge(String(children.length), 'muted') : '') +
          (rootActive ? '' : badge('Inativa', 'muted')) +
        '</div>' +
        '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
      '</div>',
    );
    if (!root.isSystem) {
      const $rootActions = $rootRow.find('[data-region=row-actions]');
      if (rootActive) {
        $rootActions.append(rowActionBtn('edit', 'Editar', root.id)).append(rowActionBtn('trash', 'Excluir', root.id, true));
      } else {
        $rootActions.append(reactivateBtn(root.id));
      }
    }
    $rootRow.on('mouseenter', function () {
      $(this).css('background', 'var(--bg-hover)');
    });
    $rootRow.on('mouseleave', function () {
      $(this).css('background', 'transparent');
    });
    $card.append($rootRow);

    if (showChildren) {
      children.forEach((sub, idx) => {
        const isLast = idx === children.length - 1;
        const subActive = sub.active !== false;
        const $subRow = $(
          '<div data-row="sub" data-id="' + esc(sub.id) + '" ' +
            'style="display:flex;align-items:center;justify-content:space-between;' +
            'padding:10px 20px 10px 44px;' +
            (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
            (subActive ? '' : 'opacity:0.55;') +
            'transition:background var(--transition);">' +
            '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
              '<span style="color:var(--text-muted);font-family:monospace;font-size:12px;">└─</span>' +
              '<span style="font-size:13px;color:var(--text-secondary);">' + esc(sub.name) + '</span>' +
              (subActive ? '' : badge('Inativa', 'muted')) +
            '</div>' +
            '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
          '</div>',
        );
        if (!sub.isSystem) {
          const $subActions = $subRow.find('[data-region=row-actions]');
          if (subActive) {
            $subActions.append(rowActionBtn('edit', 'Editar', sub.id)).append(rowActionBtn('trash', 'Excluir', sub.id, true));
          } else {
            $subActions.append(reactivateBtn(sub.id));
          }
        }
        $subRow.on('mouseenter', function () {
          $(this).css('background', 'var(--bg-hover)');
        });
        $subRow.on('mouseleave', function () {
          $(this).css('background', 'transparent');
        });
        $card.append($subRow);
      });
    }

    return $card;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const expenseCount = state.categories.filter((c) => c.nature === NATURE_EXPENSE && !c.parentId).length;
    const incomeCount = state.categories.filter((c) => c.nature === NATURE_REVENUE && !c.parentId).length;

    const tab = state.tab;
    const tree = natureRoots(currentNature()).map((r) => ({ root: r, children: childrenOf(r.id) }));

    const $header = pageHeader({
      title: 'Categorias',
      actions: btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Categoria', attrs: 'data-act="new"' }),
    });

    const $typeNav = $(
      '<div class="card" style="padding:16px;align-self:start;">' +
        '<div style="display:flex;flex-direction:column;gap:4px;">' +
          tabButton('expense', 'Despesas', 'arrowDown', expenseCount, tab === 'expense') +
          tabButton('income', 'Receitas', 'arrowUp', incomeCount, tab === 'income') +
        '</div>' +
      '</div>',
    );

    let $list: JQuery;
    if (tree.length === 0) {
      $list = $('<div class="card" style="padding:40px 20px;text-align:center;color:var(--text-muted);font-size:13px;">Nenhuma categoria cadastrada.</div>');
    } else {
      $list = $('<div style="display:flex;flex-direction:column;gap:12px;"></div>');
      tree.forEach((node) => $list.append(renderRootCard(node.root, node.children)));
    }

    const $grid = $('<div style="display:grid;grid-template-columns:240px 1fr;gap:16px;align-items:start;"></div>');
    $grid.append($typeNav).append($list);

    $root.empty().append($header).append($grid);
  }

  function openFormModal(existing: Category | null): void {
    const isEdit = !!existing;
    const initialNature = isEdit ? (existing as Category).nature : currentNature();
    const name = isEdit ? (existing as Category).name : '';
    const parentId = isEdit ? (existing as Category).parentId || '' : '';

    const nameId = 'cat-name-' + Date.now();
    const parentSelectId = 'cat-parent-' + Date.now();
    const natureName = 'cat-nature-' + Date.now();

    const showParent = !isEdit || !!(existing as Category).parentId; // hide for root edits (matches nextjs)
    const showNature = !isEdit; // nature fixed on edit

    function parentOptionsHtml(nature: string): string {
      const parents = natureRoots(nature).filter((c) => !existing || c.id !== (existing as Category).id);
      const empty = isEdit ? '' : '<option value="">— Nenhum (raiz) —</option>';
      return empty + parents.map((p) => {
        const sel = p.id === parentId ? ' selected' : '';
        return '<option value="' + esc(p.id) + '"' + sel + '>' + esc(p.name) + '</option>';
      }).join('');
    }

    const natureHtml = showNature
      ? '<div class="form-group full">' +
          '<label class="form-label">Tipo</label>' +
          '<div style="display:flex;gap:16px;align-items:center;">' +
            '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">' +
              '<input type="radio" name="' + natureName + '" value="' + NATURE_EXPENSE + '"' + (initialNature === NATURE_EXPENSE ? ' checked' : '') + '> Despesa' +
            '</label>' +
            '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">' +
              '<input type="radio" name="' + natureName + '" value="' + NATURE_REVENUE + '"' + (initialNature === NATURE_REVENUE ? ' checked' : '') + '> Receita' +
            '</label>' +
          '</div>' +
        '</div>'
      : '';

    const bodyHtml =
      '<form data-form="cat" autocomplete="off">' +
        '<div class="form-grid">' +
          natureHtml +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<input id="' + nameId + '" name="name" type="text" required placeholder="Nome da categoria" value="' + esc(name) + '" />' +
          '</div>' +
          (showParent
            ? '<div class="form-group full">' +
                '<label class="form-label" for="' + parentSelectId + '">' + (isEdit ? 'Categoria Pai' : 'Categoria Pai (opcional)') + '</label>' +
                '<select id="' + parentSelectId + '" name="parentId">' + parentOptionsHtml(initialNature) + '</select>' +
              '</div>'
            : '') +
        '</div>' +
      '</form>';

    const m: Modal = formModal({
      title: isEdit ? 'Editar Categoria' : 'Nova Categoria',
      formName: 'cat',
      body: bodyHtml,
      onSubmit: ($form) => {
        const newName = (($form.find('input[name=name]').val() as string) || '').trim();
        if (!newName) {
          $form.find('input[name=name]').trigger('focus');
          return null;
        }
        const newParent = ($form.find('select[name=parentId]').val() as string) || undefined;
        const newNature = isEdit ? (existing as Category).nature : ((m.$el.find('input[name="' + natureName + '"]:checked').val() as string) || initialNature);

        return isEdit
          ? service.update((existing as Category).id, { name: newName, parentId: newParent })
          : service.create({ name: newName, nature: newNature, parentId: newParent });
      },
      success: () => (isEdit ? 'Categoria atualizada' : 'Categoria criada'),
      failure: 'Falha ao salvar categoria',
      // SSE UPSERT will refresh CacheStore → CategoryService.onChange → re-render.
    });

    const $form = m.$body.find('form[data-form=cat]');

    // Refresh parent options when nature radio changes (new mode only).
    if (showNature && showParent) {
      const $natureInputs = $form.find('input[name="' + natureName + '"]');
      const $parentSelect = $form.find('#' + parentSelectId);
      $natureInputs.on('change click', function () {
        if (!(this as HTMLInputElement).checked) return;
        $parentSelect.html(parentOptionsHtml((this as HTMLInputElement).value));
      });
    }
  }

  function subtreeIds(target: Category): string[] {
    const ids = [target.id];
    if (!target.parentId) childrenOf(target.id).forEach((c) => ids.push(c.id));
    return ids;
  }

  function openDeleteModal(target: Category): void {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    deleteWithLinkedFallback({
      title: 'Excluir Categoria',
      body: modalText('Tem certeza que deseja excluir a categoria ' + nameHtml + '? Esta ação não pode ser desfeita.'),
      remove: () => service.remove(target.id),
      success: 'Categoria excluída',
      failure: 'Falha ao excluir categoria',
      // SSE DELETE will refresh CacheStore → CategoryService.onChange → re-render.
      linked: {
        title: 'Categoria com transações vinculadas',
        intro: (count) => {
          const hasChildren = !target.parentId && childrenOf(target.id).length > 0;
          return 'A categoria <strong>' + esc(target.name) + '</strong>' + (hasChildren ? ' (e suas subcategorias)' : '') + ' tem ' + pluralTransactions(count) + '. Escolha o que fazer:';
        },
        options: (count) => {
          const excluded = subtreeIds(target);
          const hasChildren = !target.parentId && childrenOf(target.id).length > 0;
          const eligibleTargets = (state?.categories || []).filter(
            (c) => !!c.parentId && c.nature === target.nature && excluded.indexOf(c.id) === -1 && service.isEffectivelyActive(c.id),
          );
          return [
            {
              value: 'MOVE', label: 'Mover para outra subcategoria',
              hint: 'As transações' + (hasChildren ? ' desta categoria e das subcategorias' : '') + ' passam para a categoria escolhida.',
              choices: eligibleTargets.map((c) => ({ value: c.id, label: service.labelChain(c.id) })),
            },
            {
              value: 'DELETE', label: 'Excluir transações', danger: true,
              hint: 'Apaga a categoria' + (hasChildren ? ' (e subcategorias)' : '') + ' e ' + pluralTransactions(count) + '.',
            },
            { value: 'INATIVAR', label: 'Inativar categoria', hint: 'Some dos lançamentos novos; o histórico é mantido.' },
          ];
        },
        dispatch: (choice): Promise<unknown> => {
          if (choice.strategy === 'INATIVAR') {
            return service.update(target.id, { name: target.name, parentId: target.parentId || undefined, active: false });
          }
          return service.remove(target.id, { strategy: choice.strategy as 'MOVE' | 'DELETE', targetId: choice.targetId || undefined });
        },
        success: (choice) => (choice.strategy === 'INATIVAR' ? 'Categoria inativada' : 'Categoria excluída'),
        failure: 'Falha ao excluir categoria',
      },
    });
  }

  function reactivateCategory(cat: Category): void {
    runMutation(service.update(cat.id, { name: cat.name, parentId: cat.parentId || undefined, active: true }), {
      success: 'Categoria reativada',
      failure: 'Falha ao reativar categoria',
    });
  }

  function bindRoot($root: JQuery): void {
    bindRecordActions($root, '.cats', { find: findCategory, onNew: () => openFormModal(null), onEdit: openFormModal, onDelete: openDeleteModal });
    $root.on('click.cats', '[data-act=set-tab]', function () {
      const t = $(this).attr('data-tab') as 'expense' | 'income' | undefined;
      if (t && state && t !== state.tab) {
        state.tab = t;
        render();
      }
    });
    $root.on('click.cats', '[data-act=toggle]', function (e) {
      if ($(e.target as HTMLElement).closest('[data-region=row-actions]').length) return;
      const id = $(this).attr('data-id');
      if (!id || !state) return;
      state.expanded[id] = !state.expanded[id];
      render();
    });
    $root.on('click.cats', '[data-act=reactivate]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const cat = findCategory(id);
      if (cat) reactivateCategory(cat);
    });
  }

  return cachePage<CategoriesPageState>({
    ns: '.cats',
    state: () => {
      state = { tab: 'expense', categories: [], expanded: {} };
      return state;
    },
    sync: (s) => {
      s.categories = service.listCached();
    },
    subscribe: (cb) => service.onChange(cb),
    render,
    bind: bindRoot,
  });
}
