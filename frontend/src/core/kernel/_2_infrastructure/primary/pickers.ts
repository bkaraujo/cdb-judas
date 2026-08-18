/** Shared picker UI components and related modals. */
import type { Account } from '../../_0_domain/account.ts';
import { accountsList, categoryById, esc, flatCategories, sortByName } from '../../_0_domain/format.ts';
import type { Category } from '../../_0_domain/category.ts';
import type { CostCenter } from '../../../../api/types.ts';
import { formModal, bindSwatches } from './helpers.ts';
import type { Modal } from './ui/modal.ts';
import { colorNameFieldHtml } from './ui/color-name-field.ts';
import { searchSelectHtml } from './ui/search-select.ts';
import type { SearchSelectItem } from './ui/search-select.ts';

export const PALETTE = {
  swatches: ['#6366F1', '#10B981', '#F43F5E', '#F59E0B', '#38BDF8', '#A78BFA', '#820AD1', '#1C2951'],
  series: ['#6366F1', '#38BDF8', '#F59E0B', '#10B981', '#F43F5E', '#A78BFA', '#820AD1', '#1C2951'],
} as const;

// Quick-create (category/tag) precisa criar via o serviço da fatia dona — mas kernel não pode
// referenciar App.CategoryService/App.TagService por nome (violaria a regra de fatia). Mesmo
// padrão do Sidebar.configureClosing: kernel expõe um slot vazio, composition-root injeta o
// provider real depois que a fatia carregou.
export interface QuickCreateProvider<TCreate, TCreated> {
  create(data: TCreate): Promise<TCreated>;
}

interface CategoryCreateData {
  name: string;
  nature: string;
  parentId: string | null;
}
interface TagCreateData {
  name: string;
  color: string;
}

const quickCreateProviders: {
  category: QuickCreateProvider<CategoryCreateData, unknown> | null;
  tag: QuickCreateProvider<TagCreateData, unknown> | null;
} = { category: null, tag: null };

export function configureQuickCreate(kind: 'category', provider: QuickCreateProvider<CategoryCreateData, unknown>): void;
export function configureQuickCreate(kind: 'tag', provider: QuickCreateProvider<TagCreateData, unknown>): void;
export function configureQuickCreate(kind: 'category' | 'tag', provider: QuickCreateProvider<CategoryCreateData, unknown> | QuickCreateProvider<TagCreateData, unknown>): void {
  if (kind === 'category') quickCreateProviders.category = provider as QuickCreateProvider<CategoryCreateData, unknown>;
  else quickCreateProviders.tag = provider as QuickCreateProvider<TagCreateData, unknown>;
}

// Label for a freshly-created category not yet in the cache (SSE lag).
export function quickCategoryLabel(categories: readonly Category[], cat: { name?: string; parentId?: string | null }): string {
  const name = cat.name || '';
  if (cat.parentId) {
    const parent = categoryById(categories)[cat.parentId];
    if (parent) return parent.name + ' / ' + name;
  }
  return name;
}

// Nature is normally fixed by the originating transaction type. When `nature` is null
// (quick-create not tied to a single row, e.g. import preview header) the modal exposes Tipo as
// an editable dropdown instead of the disabled label, defaulting to Despesa.
export function openCategoryCreateModal(categories: readonly Category[], nature: string | null, onCreated?: (created: unknown) => void): void {
  const uniq = Date.now();
  const nameId = 'qcat-name-' + uniq;
  const parentSelId = 'qcat-parent-' + uniq;
  const natureSelId = 'qcat-nature-' + uniq;
  const fixedNature = nature === 'INCOME' || nature === 'EXPENSE' ? nature : null;
  const natureLabel = (n: string) => (n === 'INCOME' ? 'Receita' : 'Despesa');

  function rootsFor(n: string): Category[] {
    return categories
      .filter((c) => String(c.nature || '').toUpperCase() === n && !c.parentId)
      .slice()
      .sort(sortByName);
  }

  function parentOptsFor(n: string): string {
    return '<option value="">— Nenhuma (categoria raiz) —</option>' + rootsFor(n).map((p) => '<option value="' + esc(p.id) + '">' + esc(p.name) + '</option>').join('');
  }

  const tipoFieldHtml = fixedNature
    ? '<input type="text" value="' + esc(natureLabel(fixedNature)) + '" disabled />'
    : '<select id="' + natureSelId + '" name="nature"><option value="EXPENSE">Despesa</option><option value="INCOME">Receita</option></select>';

  const bodyHtml =
    '<form data-form="qcat" autocomplete="off">' +
      '<div class="form-grid">' +
        '<div class="form-group full"><label class="form-label">Tipo</label>' + tipoFieldHtml + '</div>' +
        '<div class="form-group full">' +
          '<label class="form-label" for="' + nameId + '">Nome</label>' +
          '<input id="' + nameId + '" name="name" type="text" required placeholder="Nome da categoria" />' +
        '</div>' +
        '<div class="form-group full">' +
          '<label class="form-label" for="' + parentSelId + '">Categoria Pai (opcional)</label>' +
          '<select id="' + parentSelId + '" name="parentId">' + parentOptsFor(fixedNature || 'EXPENSE') + '</select>' +
        '</div>' +
      '</div>' +
    '</form>';

  const m = formModal({
    title: 'Nova Categoria',
    formName: 'qcat',
    body: bodyHtml,
    autofocus: 'input[name=name]',
    onSubmit: ($form) => {
      const name = (($form.find('input[name=name]').val() as string) || '').trim();
      if (!name) {
        $form.find('input[name=name]').trigger('focus');
        return null;
      }
      const parentId = ($form.find('select[name=parentId]').val() as string) || null;
      const effectiveNature = fixedNature || ($form.find('select[name=nature]').val() as string) || 'EXPENSE';
      return quickCreateProviders.category?.create({ name, nature: effectiveNature, parentId });
    },
    success: 'Categoria criada',
    failure: 'Falha ao criar categoria',
    onDone: (created) => {
      if (onCreated) onCreated(created);
    },
  });

  const $form = m.$body.find('form[data-form=qcat]');

  if (!fixedNature) {
    $form.find('select[name=nature]').on('change', function () {
      $form.find('select[name=parentId]').html(parentOptsFor((this as HTMLSelectElement).value));
    });
  }
}

// On success the created tag (with id) is handed to `onCreated` so the caller can select it.
export function openTagCreateModal(onCreated?: (created: unknown) => void): void {
  const uniq = Date.now();
  const nameId = 'qtag-name-' + uniq;
  const colorId = 'qtag-color-' + uniq;
  const defaultColor = PALETTE.swatches[0];

  const bodyHtml =
    '<form data-form="qtag" autocomplete="off">' +
      '<div class="form-grid">' +
        '<div class="form-group full">' +
          colorNameFieldHtml({ colorId, nameId, color: defaultColor, placeholder: 'Ex: mensal, fixo...', swatches: PALETTE.swatches }) +
        '</div>' +
      '</div>' +
    '</form>';

  const m: Modal = formModal({
    title: 'Nova Tag',
    formName: 'qtag',
    body: bodyHtml,
    autofocus: 'input[name=name]',
    onSubmit: ($form) => {
      const $name = $form.find('input[name=name]');
      const name = ((($name.val() as string) || '')).trim();
      if (!name) {
        $name.trigger('focus');
        return null;
      }
      return quickCreateProviders.tag?.create({ name, color: (($form.find('input[name=color]').val() as string) || defaultColor) });
    },
    success: 'Tag criada',
    failure: 'Falha ao criar tag',
    onDone: (created) => {
      if (onCreated) onCreated(created);
    },
  });

  bindSwatches(m, m.$body.find('input[name=color]'));
}

export interface CategoryPickerItem {
  id?: string;
  value?: string;
  label: string;
}

export interface CategoryPickerOptions {
  items?: CategoryPickerItem[];
  selectedId?: string | null;
  selectId?: string;
  selectAttrs?: string;
  placeholder?: string;
  /** campo opcional onde "sem categoria" é uma escolha legítima, não só o estado de "nada
   * selecionado ainda" — o placeholder some da lista quando true, senão o usuário não consegue
   * voltar a limpar a seleção pelo próprio picker (ex.: regra de nomenclatura, filtro). */
  alwaysPlaceholder?: boolean;
  lazy?: boolean;
  floating?: boolean;
  compact?: boolean;
  disabled?: boolean;
  title?: string;
}

export function categoryPickerHtml(opts: CategoryPickerOptions = {}): string {
  const items = opts.items || [];
  const selectedId = opts.selectedId != null ? String(opts.selectedId) : '';
  const selectId = opts.selectId || '';
  const selectAttrs = opts.selectAttrs || '';
  const placeholder = opts.placeholder || 'Selecione';
  const alwaysPlaceholder = !!opts.alwaysPlaceholder;

  const hasSel = items.some((it) => String(it.value || it.id) === selectedId && selectedId !== '');
  let ddItems: SearchSelectItem[];
  if (!items.length) {
    ddItems = [{ value: '', label: 'Sem categorias' }];
  } else if (hasSel && !alwaysPlaceholder) {
    ddItems = items.map((it) => ({ value: String(it.value || it.id), label: it.label }));
  } else {
    ddItems = [{ value: '', label: placeholder }, ...items.map((it) => ({ value: String(it.value || it.id), label: it.label }))];
  }

  const optionsHtml = ddItems.map((it) => {
    const sel = String(it.value) === selectedId ? ' selected' : '';
    return '<option value="' + esc(it.value) + '"' + sel + '>' + esc(it.label) + '</option>';
  }).join('');

  return (
    '<select id="' + esc(selectId) + '"' + selectAttrs + ' style="display:none;">' + optionsHtml + '</select>' +
    searchSelectHtml(ddItems, selectedId, selectId + '-dd', {
      pairedSelectId: selectId,
      lazy: opts.lazy !== false,
      floating: !!opts.floating,
      compact: !!opts.compact,
      disabled: !!opts.disabled,
      title: opts.title || '',
    })
  );
}

/* ---- <option> builders ----
 * Shared across the form modals that dropdown-select an account/category/cost-center. */

export interface OptionsHtmlOptions<T> {
  labelOf?: (item: T) => string;
  placeholder?: string;
}

// Generic "— placeholder — + items" <option> list, keyed by `.id` (labelOf defaults to
// name/description). Used where a field is optional (no valid default to fall back to).
export function optionsHtml<T extends { id?: string; name?: string; description?: string }>(items: readonly T[] | null | undefined, selectedId: string | null | undefined, opts: OptionsHtmlOptions<T> = {}): string {
  const labelOf = opts.labelOf || ((it: T) => it.name || it.description || '');
  const out = ['<option value="">' + esc(opts.placeholder || '— Nenhuma —') + '</option>'];
  (items || []).forEach((it) => {
    const sel = String(it.id) === String(selectedId) ? ' selected' : '';
    out.push('<option value="' + esc(it.id) + '"' + sel + '>' + esc(labelOf(it)) + '</option>');
  });
  return out.join('');
}

export interface AccountOptionsHtmlOptions {
  keepId?: string | null;
  items?: Account[];
  includeEmpty?: boolean;
  emptyLabel?: string;
  activeOnly?: boolean;
}

// Account <option> list. Defaults to active-only (kept accounts: `opts.keepId`, typically the
// account already saved on the record being edited, so it survives even if inactivated since).
// `opts.items` overrides the source list entirely (e.g. import preview's server-picked candidates).
// `opts.includeEmpty` prepends a placeholder (`opts.emptyLabel`, default '— Selecione —').
export function accountOptionsHtml(accounts: readonly Account[], selectedId: string | null | undefined, opts: AccountOptionsHtmlOptions = {}): string {
  const keepId = opts.keepId != null ? String(opts.keepId) : null;
  const items = opts.items || accountsList(accounts).filter((a) => opts.activeOnly === false || a.active !== false || String(a.id) === keepId || String(a.id) === String(selectedId));
  const empty = opts.includeEmpty ? '<option value="">' + esc(opts.emptyLabel || '— Selecione —') + '</option>' : '';
  if (!items.length) return empty || '<option value="">Nenhuma conta disponível</option>';
  return empty + items.map((a) => {
    const sel = String(a.id) === String(selectedId) ? ' selected' : '';
    return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
  }).join('');
}

// Cost-center <option> list with the "Variável" fallback: when nothing is selected yet, the cost
// center whose description/name matches /vari/i is pre-selected (mirrors the backend's own
// fallback when a transaction/import row arrives without a costCenterId).
export function costCenterOptionsHtml(costCenters: readonly CostCenter[], selectedId: string | null | undefined): string {
  if (!costCenters.length) return '<option value="">Nenhum centro de custo</option>';
  const variavel = costCenters.filter((c) => /vari/i.test(c.description || '')).at(0);
  const target = selectedId || (variavel && variavel.id) || '';
  return costCenters.map((c) => {
    const label = c.description || '';
    const sel = String(c.id) === String(target) ? ' selected' : '';
    return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(label) + '</option>';
  }).join('');
}

// The `flatCategories → [{value,label}]` shape shared by every category dropdown that isn't
// built through categoryPickerHtml directly: falls back to a single disabled-looking placeholder
// item when the nature has no categories at all.
export function categoryItemsFor(categories: readonly Category[], nature: string, keepId: string | null | undefined): SearchSelectItem[] {
  const cats = flatCategories(categories, nature, true, keepId);
  if (!cats.length) return [{ value: '', label: 'Nenhuma categoria disponível' }];
  return cats.map((c) => ({ value: String(c.id), label: c.label }));
}
