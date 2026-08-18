/** pages/import-rules — Regras de nomenclatura (nome → conta/categoria/centro de custo).
 *
 * Sem SSE (a fatia f010 não dispara evento nenhum): toda mutação recarrega a lista via
 * `service.list()` em vez de depender de `CacheStore`/`onChange`, mesmo padrão de `budget`.
 */
import $ from 'jquery';
import { categoryLabel, esc, sortByName } from '../../core/kernel/_0_domain/format.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { byId, confirmModal, formModal, modalText, runMutation } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { createPage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { accountOptionsHtml, categoryItemsFor, categoryPickerHtml, optionsHtml } from '../../core/kernel/_2_infrastructure/primary/pickers.ts';
import { btn, rowActionBtn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import type { ImportRule } from '../../api/types.ts';
import type { ImportRuleService } from './service.ts';

interface ImportRulesPageState extends PageState {
  rules: ImportRule[];
}

export interface ImportRulesPageDeps {
  service: ImportRuleService;
  cache: CacheStore;
}

function renderRow(r: ImportRule, deps: ImportRulesPageDeps): JQuery {
  function accountName(id: string | null | undefined): string | null {
    if (!id) return null;
    const a = deps.cache.findById('accounts', id);
    return a ? a.name : null;
  }
  function categoryName(id: string | null | undefined): string | null {
    if (!id) return null;
    const c = deps.cache.findById('categories', id);
    return c ? categoryLabel(deps.cache.categories(), c) : null;
  }
  function costCenterName(id: string | null | undefined): string | null {
    if (!id) return null;
    const cc = deps.cache.findById('costCenters', id);
    return cc ? cc.description || '' : null;
  }

  const parts: string[] = [];
  const acc = accountName(r.accountId);
  const cat = categoryName(r.categoryId);
  const cc = costCenterName(r.costCenterId);
  if (acc) parts.push('Conta: ' + acc);
  if (cat) parts.push('Categoria: ' + cat);
  if (cc) parts.push('Centro de custo: ' + cc);
  const subtitle = parts.length ? parts.join(' · ') : 'Sem conta/categoria/centro de custo';

  const $row = $(
    '<div class="card-row" data-id="' + esc(r.id) + '">' +
      '<div class="card-row-main">' +
        '<div style="min-width:0;display:flex;flex-direction:column;gap:2px;">' +
          '<span class="row-title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(r.name) + '</span>' +
          '<span class="row-sub" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(subtitle) + '</span>' +
        '</div>' +
      '</div>' +
      '<div class="card-row-actions" data-region="row-actions"></div>' +
    '</div>',
  );

  $row.find('[data-region=row-actions]').append(rowActionBtn('edit', 'Editar', r.id as string)).append(rowActionBtn('trash', 'Excluir', r.id as string, true));

  return $row;
}

export function createImportRulesPage(deps: ImportRulesPageDeps): Page {
  let state: ImportRulesPageState | null = null;
  const { service, cache } = deps;

  function findRule(id: string): ImportRule | null {
    return state ? byId(state.rules, id) : null;
  }

  // No SSE for this slice — this page is the only place regras de nomenclatura get mutated, so
  // it's also the only place that can keep CacheStore.importRules fresh for the live matcher used
  // elsewhere (manual lançamento form, import previews).
  function loadRules(): Promise<void> {
    return service.list().then((list) => {
      if (!state) return;
      state.rules = Array.isArray(list) ? list : [];
      cache.setImportRules(state.rules);
      render();
    });
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $header = pageHeader({
      title: 'Regras de Nomenclatura',
      actions: btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Regra', attrs: 'data-act="new"' }),
    });

    let $body: JQuery;
    if (!state.rules.length) {
      $body = $(
        emptyState({
          icon: 'edit',
          title: 'Nenhuma regra cadastrada',
          desc: 'Ao aparecer o texto de uma regra na descrição de um lançamento (digitado ou importado de PDF), a descrição é substituída e conta/categoria/centro de custo podem ser preenchidos automaticamente. Clique em "Nova Regra" para começar.',
        }),
      );
    } else {
      const sorted = state.rules.slice().sort(sortByName);
      $body = $('<div class="card card-list" style="overflow-y:auto;max-height:calc(100vh - 200px);min-height:200px;"></div>');
      sorted.forEach((r) => $body.append(renderRow(r, deps)));
    }

    $root.empty().append($header).append($body);
  }

  function openFormModal(existing: ImportRule | null): void {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = { name: 'rule-name-' + uniq, account: 'rule-account-' + uniq, category: 'rule-category-' + uniq, costCenter: 'rule-cc-' + uniq };
    const initial = {
      name: isEdit ? existing?.name || '' : '',
      accountId: isEdit ? existing?.accountId || '' : '',
      categoryId: isEdit ? existing?.categoryId || '' : '',
      costCenterId: isEdit ? existing?.costCenterId || '' : '',
    };

    const accountOptions = accountOptionsHtml(cache.accounts(), initial.accountId, { includeEmpty: true, emptyLabel: '— Nenhuma —', activeOnly: false });
    const categoryFieldHtml = categoryPickerHtml({
      items: categoryItemsFor(cache.categories(), '', initial.categoryId),
      selectedId: initial.categoryId,
      selectId: ids.category,
      selectAttrs: ' name="categoryId"',
      placeholder: '— Nenhuma —',
      alwaysPlaceholder: true,
    });
    const costCenterOptions = optionsHtml(cache.costCenters(), initial.costCenterId, {
      labelOf: (c) => c.description || '',
    });

    const bodyHtml =
      '<form data-form="rule" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Nome</label>' +
            '<input id="' + ids.name + '" name="name" type="text" required minlength="3" placeholder="Ex: Companhia de Saneamento" value="' + esc(initial.name) + '" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.account + '">Conta (opcional)</label>' +
            '<select id="' + ids.account + '" name="accountId">' + accountOptions + '</select>' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.category + '">Categoria (opcional)</label>' +
            categoryFieldHtml +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.costCenter + '">Centro de custo (opcional)</label>' +
            '<select id="' + ids.costCenter + '" name="costCenterId">' + costCenterOptions + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    formModal({
      title: isEdit ? 'Editar Regra' : 'Nova Regra',
      formName: 'rule',
      body: bodyHtml,
      autofocus: 'input[name=name]',
      onSubmit: ($form) => {
        const $name = $form.find('input[name=name]');
        const name = (($name.val() as string) || '').trim();
        if (name.length < 3) {
          $name.trigger('focus');
          return null;
        }
        const payload = {
          name,
          accountId: ($form.find('select[name=accountId]').val() as string) || undefined,
          categoryId: ($form.find('select[name=categoryId]').val() as string) || undefined,
          costCenterId: ($form.find('select[name=costCenterId]').val() as string) || undefined,
        };
        return isEdit ? service.update((existing as ImportRule).id as string, payload) : service.create(payload);
      },
      success: () => (isEdit ? 'Regra atualizada' : 'Regra criada'),
      failure: 'Falha ao salvar regra',
      onDone: loadRules,
    });
  }

  function openDeleteModal(target: ImportRule): void {
    confirmModal({
      title: 'Excluir Regra',
      body: modalText('Tem certeza que deseja excluir a regra <strong>' + esc(target.name) + '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: (m, reEnable) => {
        runMutation(service.remove(target.id as string), {
          modal: m, success: 'Regra removida', failure: 'Falha ao excluir regra',
          onDone: loadRules, onError: reEnable,
        });
      },
    });
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.import-rules', '[data-act=new]', () => openFormModal(null));
    $root.on('click.import-rules', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const r = findRule(id);
      if (r) openFormModal(r);
    });
    $root.on('click.import-rules', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const r = findRule(id);
      if (r) openDeleteModal(r);
    });
  }

  return createPage<ImportRulesPageState>({
    ns: '.import-rules',
    state: () => {
      state = { rules: [] };
      return state;
    },
    render,
    bind: bindRoot,
    onMount: () => {
      loadRules();
    },
  });
}
