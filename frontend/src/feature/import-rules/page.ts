/** pages/import-rules — Regras de nomenclatura (nome + gatilhos → conta/categoria/centro de custo).
 *
 * Sem SSE (a fatia f010 não dispara evento nenhum): toda mutação recarrega a lista via
 * `service.list()` em vez de depender de `CacheStore`/`onChange`, mesmo padrão de `budget`.
 */
import $ from 'jquery';
import {categoryLabel, esc, sortByName} from '@/core/kernel/_0_domain/format.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import {
    bindRecordActions,
    byId,
    confirmModal,
    formModal,
    modalText,
    runMutation
} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import type {Page, PageState} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {createPage} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {
    accountOptionsHtml,
    categoryItemsFor,
    categoryPickerHtml
} from '@/core/kernel/_2_infrastructure/primary/pickers.ts';
import {btn, rowActionBtn} from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import {emptyState} from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import {pageHeader} from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import type {ImportRule} from '@/api/types.ts';
import type {ImportRuleService} from '@/feature/import-rules/service.ts';

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
  const parts: string[] = [];
  const acc = accountName(r.accountId);
  const cat = categoryName(r.categoryId);
  if (acc) parts.push('Conta: ' + acc);
  if (cat) parts.push('Categoria: ' + cat);
  if (r.planned !== null && r.planned !== undefined) parts.push('Planejado: ' + (r.planned ? 'Sim' : 'Não'));
  const triggerCount = (r.triggers || []).length;
  parts.push(triggerCount === 1 ? '1 gatilho' : triggerCount + ' gatilhos');
  const subtitle = parts.join(' · ');

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
          desc: 'Cadastre uma regra com um ou mais gatilhos (textos que podem aparecer na descrição de um lançamento, digitado ou importado de PDF). Ao bater qualquer gatilho, conta/categoria/centro de custo são preenchidos automaticamente — a descrição original nunca é alterada. Clique em "Nova Regra" para começar.',
        }),
      );
    } else {
      const sorted = state.rules.slice().sort(sortByName);
      $body = $('<div class="card card-list" style="overflow-y:auto;max-height:calc(100vh - 200px);min-height:200px;"></div>');
      sorted.forEach((r) => $body.append(renderRow(r, deps)));
    }

    $root.empty().append($header).append($body);
  }

  function triggerRowHtml(value: string): string {
    return (
      '<div class="trigger-row" style="display:flex;gap:6px;align-items:center;" data-trigger-row>' +
        '<input type="text" name="triggers[]" placeholder="Ex: Companhia de Saneamento" value="' + esc(value) + '" style="flex:1;" />' +
        '<button type="button" data-act="remove-trigger" aria-label="Remover gatilho" style="background:none;border:none;color:var(--muted);cursor:pointer;padding:4px;display:inline-flex;">' + icon('x', 14) + '</button>' +
      '</div>'
    );
  }

  function openFormModal(existing: ImportRule | null): void {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = { name: 'rule-name-' + uniq, account: 'rule-account-' + uniq, category: 'rule-category-' + uniq, planned: 'rule-planned-' + uniq };
    const initial = {
      name: isEdit ? existing?.name || '' : '',
      triggers: isEdit ? (existing?.triggers && existing.triggers.length ? existing.triggers.slice() : ['']) : [''],
      accountId: isEdit ? existing?.accountId || '' : '',
      categoryId: isEdit ? existing?.categoryId || '' : '',
      planned: isEdit && existing?.planned !== undefined ? existing.planned : null,
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

    const triggersHtml = initial.triggers.map(triggerRowHtml).join('');

    const bodyHtml =
      '<form data-form="rule" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Nome</label>' +
            '<input id="' + ids.name + '" name="name" type="text" minlength="3" placeholder="Ex: Companhia de Saneamento" value="' + esc(initial.name) + '" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label">Gatilhos</label>' +
            '<div data-region="triggers" style="display:flex;flex-direction:column;gap:6px;">' + triggersHtml + '</div>' +
            '<button type="button" data-act="add-trigger" style="background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;font-weight:600;padding:4px 0;display:inline-flex;align-items:center;gap:3px;">' + icon('plus', 12) + 'Adicionar gatilho</button>' +
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
            '<label class="form-label" for="' + ids.planned + '">Planejado (opcional)</label>' +
            '<select id="' + ids.planned + '" name="planned">' +
              '<option value="">— Não definido —</option>' +
              '<option value="true"' + (initial.planned === true ? ' selected' : '') + '>Sim</option>' +
              '<option value="false"' + (initial.planned === false ? ' selected' : '') + '>Não</option>' +
            '</select>' +
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
        const categoryIdForName = ($form.find('select[name=categoryId]').val() as string) || '';
        const categoryName = categoryIdForName ? categoryLabel(cache.categories(), { id: categoryIdForName }) : '';
        const name = (($name.val() as string) || '').trim() || categoryName;
        if (name.length < 3) {
          $name.trigger('focus');
          return null;
        }
        const triggers = $form
          .find('input[name="triggers[]"]')
          .map(function () { return (($(this).val() as string) || '').trim(); })
          .get()
          .filter((t) => t.length >= 3);
        if (!triggers.length) {
          $form.find('input[name="triggers[]"]').first().trigger('focus');
          return null;
        }
        const plannedVal = $form.find('select[name=planned]').val() as string;
        const payload = {
          name,
          triggers,
          accountId: ($form.find('select[name=accountId]').val() as string) || undefined,
          categoryId: ($form.find('select[name=categoryId]').val() as string) || undefined,
          planned: plannedVal === '' ? undefined : plannedVal === 'true',
        };
        return isEdit ? service.update((existing as ImportRule).id as string, payload) : service.create(payload);
      },
      success: () => (isEdit ? 'Regra atualizada' : 'Regra criada'),
      failure: 'Falha ao salvar regra',
      onDone: loadRules,
    });

    const $form = $('form[data-form=rule]');
    $form.on('click', '[data-act=add-trigger]', function (e) {
      e.preventDefault();
      const $container = $form.find('[data-region=triggers]');
      const $row = $(triggerRowHtml(''));
      $container.append($row);
      $row.find('input[name="triggers[]"]').trigger('focus');
    });

    $form.on('click', '[data-act=remove-trigger]', function (e) {
      e.preventDefault();
      const $row = $(this).closest('[data-trigger-row]');
      const $container = $row.closest('[data-region=triggers]');
      if ($container.find('[data-trigger-row]').length > 1) {
        $row.remove();
      } else {
        $row.find('input[name="triggers[]"]').val('');
      }
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
    bindRecordActions($root, '.import-rules', { find: findRule, onNew: () => openFormModal(null), onEdit: openFormModal, onDelete: openDeleteModal });
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
