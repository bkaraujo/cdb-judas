/** Diálogo de criação/edição de lançamento.
 *
 * Modal único para criar ou editar um lançamento (despesa/receita/transferência), incluindo o
 * quick-create de categoria a partir do próprio formulário.
 */
import $ from 'jquery';
import type { TransactionRequest, TransactionResponse } from '../../api/overrides.ts';
import type { ImportRule } from '../../api/types.ts';
import * as AccountDomain from '../../core/kernel/_0_domain/account.ts';
import { esc, maskCurrency, parseCurrency } from '../../core/kernel/_0_domain/format.ts';
import * as Transaction from '../../core/kernel/_0_domain/transaction.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { bindCurrencyMask, byId } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { formModal } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { icon } from '../../core/kernel/_2_infrastructure/primary/icons.ts';
import { accountOptionsHtml, categoryItemsFor, categoryPickerHtml, costCenterOptionsHtml, openCategoryCreateModal, openTagCreateModal, quickCategoryLabel } from '../../core/kernel/_2_infrastructure/primary/pickers.ts';
import { refreshSearchSelect } from '../../core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import { appendTagRow, refreshTagsDropdownLabel, tagsDropdownHtml } from '../../core/kernel/_2_infrastructure/primary/ui/tags-dropdown.ts';
import { typeToggleHtml } from '../../core/kernel/_2_infrastructure/primary/ui/type-toggle.ts';
import { toast } from '../../core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type { Modal } from '../../core/kernel/_2_infrastructure/primary/ui/modal.ts';
import type { TransactionService } from './service.ts';

export interface ImportRulesPort {
  match(description: string | null | undefined, rules: readonly ImportRule[] | null | undefined): ImportRule | null;
  listCached(): ImportRule[];
}

export interface TransactionFormModalDeps {
  transactionService: TransactionService;
  cache: CacheStore;
  importRules: ImportRulesPort;
}

export interface TransactionFormModalOptions {
  existing?: TransactionResponse | null;
  isTransfer?: boolean;
  defaultDate?: string | null;
  onSaved?: () => void;
}

export function createTransactionFormModal(deps: TransactionFormModalDeps) {
  return function transactionFormModal(opts: TransactionFormModalOptions = {}): Modal {
    const existing = opts.existing || null;
    const onSaved = typeof opts.onSaved === 'function' ? opts.onSaved : () => null;

    const isEdit = !!existing;
    const isTransferEdit = isEdit && !!opts.isTransfer;
    const uniq = Date.now();
    const ids = {
      desc: 'tx-desc-' + uniq, amount: 'tx-amount-' + uniq, date: 'tx-date-' + uniq, category: 'tx-cat-' + uniq,
      account: 'tx-acc-' + uniq, card: 'tx-card-' + uniq, destAccount: 'tx-dest-' + uniq, costCenter: 'tx-cc-' + uniq,
      status: 'tx-status-' + uniq, notes: 'tx-notes-' + uniq,
    };

    const initial = {
      type: (isEdit ? existing?.type || 'expense' : 'expense') as string,
      description: isEdit ? existing?.description || '' : '',
      amount: isEdit ? maskCurrency(String(Math.round(Math.abs(Number(existing?.amount) || 0) * 100) / 100)) : '',
      date: isEdit ? (existing?.date || '').slice(0, 10) : opts.defaultDate || '',
      categoryId: isEdit ? existing?.categoryId || '' : '',
      accountId: isEdit ? String(existing?.accountId || '') : '',
      destAccountId: '',
      status: (isEdit ? (existing?.status === 'balance' ? 'confirmed' : existing?.status || 'confirmed') : 'confirmed') as string,
      costCenterId: isEdit ? String(existing?.costCenterId || '') : '',
      cardId: isEdit ? existing?.cardId || '' : '',
      isEstorno: isEdit ? (existing?.type === 'expense' && Number(existing?.amount) > 0) || (existing?.type === 'income' && Number(existing?.amount) < 0) : false,
      notes: isEdit ? existing?.notes || '' : '',
      tagIds: isEdit ? (existing?.tagIds || []).map(String) : ([] as string[]),
    };

    const accs = deps.cache.accounts().filter((a) => a.active !== false || (isEdit && String(a.id) === initial.accountId));
    if (!initial.accountId && accs.length) initial.accountId = String(accs[0]?.id);

    function natureForType(t: unknown): string | null {
      if (t === 'income') return 'INCOME';
      if (t === 'expense') return 'EXPENSE';
      return null;
    }

    // Perna de transferência: "Conta" é renomeada para orientar qual lado ela representa — EXPENSE
    // = saída (Origem), INCOME = entrada (Destino). Só depende da natureza da própria perna.
    function accountFieldLabel(): string {
      if (!isTransferEdit) return 'Conta';
      return initial.type === 'expense' ? 'Conta de Origem' : 'Conta de Destino';
    }

    function buildCatItems(type: string, selectedId: string) {
      return categoryItemsFor(deps.cache.categories(), natureForType(type) || '', selectedId);
    }

    function buildAccountOpts(selectedId: string, includeEmpty: boolean): string {
      return accountOptionsHtml(deps.cache.accounts(), selectedId, { keepId: isEdit ? initial.accountId : null, includeEmpty });
    }

    // Card select is optional and only relevant when the chosen account has cards attached;
    // options + visibility are recomputed whenever the account selection changes.
    function cardOptionsHtml(accountId: string, selectedCardId: string | null): string {
      const acc = byId(accs, accountId);
      if (!acc || !AccountDomain.hasCards(acc)) return '';
      const visible = acc.cards.filter((c) => c.active !== false || String(c.id) === String(selectedCardId));
      if (!visible.length) return '';
      return (
        '<option value="">— Nenhum —</option>' +
        visible.map((c) => '<option value="' + esc(c.id) + '"' + (String(c.id) === String(selectedCardId) ? ' selected' : '') + '>•••• ' + esc(c.last4) + '</option>').join('')
      );
    }

    function cardFieldHtml(accountId: string, selectedCardId: string): string {
      const options = cardOptionsHtml(accountId, selectedCardId);
      return (
        '<div class="form-group" data-region="card-field" style="display:' + (options ? 'block' : 'none') + ';">' +
        '<label class="form-label" for="' + ids.card + '">Cartão</label>' +
        '<select id="' + ids.card + '" name="cardId"' + (isTransferEdit ? ' disabled' : '') + '>' + options + '</select>' +
        '</div>'
      );
    }

    function buildCostCenterOpts(selectedId: string): string {
      return costCenterOptionsHtml(deps.cache.costCenters(), selectedId);
    }

    const accOpts = buildAccountOpts(initial.accountId, false);

    const statusOpts = ([['confirmed', 'Confirmado'], ['pending', 'Pendente'], ['scheduled', 'Agendado']] as const)
      .map((p) => '<option value="' + esc(p[0]) + '"' + (p[0] === initial.status ? ' selected' : '') + '>' + esc(p[1]) + '</option>')
      .join('');

    const TX_TYPE_OPTIONS = [
      { value: 'expense', label: '↓ Despesa', color: 'expense' },
      { value: 'income', label: '↑ Receita', color: 'income' },
      { value: 'transfer', label: '⇄ Transferência', color: 'accent' },
    ];
    const TX_TYPE_DISABLED_TITLE = 'Tipo não pode ser alterado ao editar uma perna de transferência';

    function buildGridHtml(type: string): string {
      if (type === 'transfer') {
        return (
          '<div class="form-group"><label class="form-label" for="' + ids.account + '">Origem</label><select id="' + ids.account + '" name="accountId">' + buildAccountOpts(initial.accountId, true) + '</select></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.destAccount + '">Destino</label><select id="' + ids.destAccount + '" name="destAccountId">' + buildAccountOpts(initial.destAccountId, true) + '</select></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.date + '">Data</label><input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" /></div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.amount + '">Valor (R$)</label><input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" placeholder="0,00" value="' + esc(initial.amount) + '" /></div>'
        );
      }
      return (
        '<div class="form-group full">' +
          '<label class="form-label" for="' + ids.desc + '">Descrição</label>' +
          '<input id="' + ids.desc + '" name="description" type="text" required ' + (isTransferEdit ? 'disabled ' : '') + 'style="text-transform:uppercase;" placeholder="Ex: Mercado, Salário..." value="' + esc(initial.description) + '" />' +
        '</div>' +
        '<div class="form-group"><label class="form-label" for="' + ids.date + '">Data</label><input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" /></div>' +
        '<div class="form-group"><label class="form-label" for="' + ids.account + '">' + esc(accountFieldLabel()) + '</label><select id="' + ids.account + '" name="accountId">' + accOpts + '</select></div>' +
        cardFieldHtml(initial.accountId, initial.cardId) +
        '<div class="form-group"><label class="form-label" for="' + ids.costCenter + '">Centro de Custo</label><select id="' + ids.costCenter + '" name="costCenterId"' + (isTransferEdit ? ' disabled' : '') + '>' + buildCostCenterOpts(initial.costCenterId) + '</select></div>' +
        '<div class="form-group">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">' +
            '<label class="form-label" for="' + ids.category + '" style="margin:0;">Categoria</label>' +
            '<button type="button" data-act="new-category" style="background:none;border:none;color:var(--accent);cursor:pointer;font-size:11px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' + icon('plus', 12) + 'Nova categoria</button>' +
          '</div>' +
          categoryPickerHtml({
            items: buildCatItems(type, initial.categoryId),
            selectedId: initial.categoryId,
            selectId: ids.category,
            selectAttrs: ' name="categoryId" data-region="category-select"',
            placeholder: 'Selecione',
          }) +
        '</div>' +
        '<div class="form-group">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">' +
            '<label class="form-label" style="margin:0;">Tags</label>' +
            '<button type="button" data-act="new-tag" style="background:none;border:none;color:var(--accent);cursor:pointer;font-size:11px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' + icon('plus', 12) + 'Nova tag</button>' +
          '</div>' +
          tagsDropdownHtml(deps.cache.tags(), initial.tagIds, 'tx') +
        '</div>' +
        '<div class="form-group"><label class="form-label" for="' + ids.status + '">Status</label><select id="' + ids.status + '" name="status">' + statusOpts + '</select></div>' +
        '<div class="form-group"><label class="form-label" for="' + ids.amount + '">Valor (R$)</label><input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" placeholder="0,00" value="' + esc(initial.amount) + '" /></div>' +
        '<div class="form-group full">' +
          '<label style="display:inline-flex;align-items:center;gap:8px;' + (isTransferEdit ? 'cursor:not-allowed;opacity:0.55;' : 'cursor:pointer;') + 'font-size:13px;font-weight:500;color:var(--text-secondary);">' +
            '<input type="checkbox" name="estorno"' + (initial.isEstorno ? ' checked' : '') + (isTransferEdit ? ' disabled' : '') + ' style="width:16px;height:16px;accent-color:var(--accent);" />Estorno (inverter sinal)' +
          '</label>' +
        '</div>' +
        '<div class="form-group full">' +
          '<label class="form-label" for="' + ids.notes + '">Anotações <span style="font-weight:400;color:var(--text-muted);">(opcional)</span></label>' +
          '<textarea id="' + ids.notes + '" name="notes" maxlength="250" rows="3" ' + (isTransferEdit ? 'disabled ' : '') + 'placeholder="Até 250 caracteres..." style="resize:vertical;min-height:60px;">' + esc(initial.notes) + '</textarea>' +
          '<p style="font-size:11px;color:var(--text-muted);margin-top:4px;text-align:right;" data-region="notes-counter">' + esc(initial.notes.length + '/250') + '</p>' +
        '</div>'
      );
    }

    const bodyHtml =
      '<form data-form="tx" autocomplete="off">' +
        (isTransferEdit
          ? '<div style="display:flex;gap:8px;align-items:flex-start;padding:10px 12px;margin-bottom:14px;border:1px solid var(--info);border-radius:var(--radius-sm);background:var(--info-light, transparent);">' +
            '<span style="color:var(--info);flex-shrink:0;display:flex;">' + icon('alertCircle', 16) + '</span>' +
            '<span style="font-size:12px;color:var(--text-secondary);line-height:1.4;">Esta transação faz parte de uma transferência. Você pode ajustar conta, data, valor, status e categoria desta perna — a perna oposta é sincronizada automaticamente. Tipo e os demais campos não podem ser alterados por aqui.</span>' +
            '</div>'
          : '') +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' + typeToggleHtml(TX_TYPE_OPTIONS, initial.type, { disabled: isTransferEdit, disabledTitle: TX_TYPE_DISABLED_TITLE }) + '</div>' +
        '<input type="hidden" name="type" value="' + esc(initial.type) + '" />' +
        '<div class="form-grid" data-region="grid">' + buildGridHtml(initial.type) + '</div>' +
      '</form>';

    let submittedType = initial.type;
    const m: Modal = formModal({
      title: isEdit ? 'Editar Lançamento' : 'Novo Lançamento',
      formName: 'tx',
      body: bodyHtml,
      onSubmit: ($form) => submit($form),
      success: () => {
        if (submittedType === 'transfer') return isEdit ? 'Lançamento convertido em transferência' : 'Transferência registrada';
        return isEdit ? 'Lançamento atualizado' : 'Lançamento criado';
      },
      failure: () => (submittedType === 'transfer' ? 'Falha ao registrar transferência' : 'Falha ao salvar lançamento'),
      onDone: onSaved,
    });

    const $form = m.$body.find('form[data-form=tx]');
    function bindAmountMask(): void {
      bindCurrencyMask($form.find('input[name=amount]'));
    }
    bindAmountMask();

    function bindNotesCounter(): void {
      const $notes = $form.find('textarea[name=notes]');
      if ($notes.length) {
        $notes.off('input.notesCnt').on('input.notesCnt', function () {
          const len = ((this as HTMLTextAreaElement).value || '').length;
          m.$body.find('[data-region=notes-counter]').text(len + '/250');
        });
      }
    }
    bindNotesCounter();

    // Live "regra de nomenclatura" match: while typing the description, if a rule's name appears
    // in the text, replace the description with the rule's own name and pre-fill whichever of
    // conta/categoria/centro de custo the rule sets.
    m.$body.on('input', 'input[name=description]', function () {
      const $desc = $(this);
      const value = ($desc.val() as string) || '';
      const rule = deps.importRules.match(value, deps.importRules.listCached());
      if (!rule || value === rule.name) return;

      $desc.val(rule.name || '');
      initial.description = rule.name || '';

      if (rule.accountId) {
        const $acc = m.$body.find('select[name=accountId]');
        if ($acc.find('option[value="' + esc(rule.accountId) + '"]').length) {
          $acc.val(String(rule.accountId)).trigger('change');
          initial.accountId = String(rule.accountId);
        }
      }
      if (rule.categoryId) {
        const $cat = m.$body.find('select[name=categoryId]');
        if ($cat.find('option[value="' + esc(rule.categoryId) + '"]').length) {
          $cat.val(String(rule.categoryId));
          refreshSearchSelect(ids.category);
          initial.categoryId = String(rule.categoryId);
        }
      }
      if (rule.costCenterId) {
        const $cc = m.$body.find('select[name=costCenterId]');
        if ($cc.find('option[value="' + esc(rule.costCenterId) + '"]').length) {
          $cc.val(String(rule.costCenterId));
          initial.costCenterId = String(rule.costCenterId);
        }
      }
    });

    // Tags: mesma dropdown compartilhada (ui/) — checkboxes fora de qualquer <select>, então o
    // estado mora em initial.tagIds (mutado direto a cada change), não lido do DOM no submit.
    m.$body.on('change', '[data-tag-check]', function () {
      const tagId = String($(this).attr('data-tag-id'));
      const current = initial.tagIds.map(String);
      initial.tagIds = (this as HTMLInputElement).checked ? current.concat([tagId]) : current.filter((id) => id !== tagId);
      refreshTagsDropdownLabel($(this));
    });

    // Account change refreshes which cards are offered (a card belongs to one account).
    m.$body.on('change', 'select[name=accountId]', function () {
      const $field = m.$body.find('[data-region=card-field]');
      if (!$field.length) return;
      const options = cardOptionsHtml($(this).val() as string, null);
      $field.find('select[name=cardId]').html(options);
      $field.css('display', options ? 'block' : 'none');
    });

    // Type buttons sync hidden input + rebuild grid layout for the chosen type.
    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      if (isTransferEdit) return; // tipo é imutável ao editar uma perna de transferência (par se desfaria)
      const t = $(this).attr('data-type') as string;
      initial.description = ($form.find('input[name=description]').val() as string) || initial.description;
      initial.amount = ($form.find('input[name=amount]').val() as string) || initial.amount;
      initial.date = ($form.find('input[name=date]').val() as string) || initial.date;
      initial.accountId = ($form.find('select[name=accountId]').val() as string) || initial.accountId;
      const $destSel = $form.find('select[name=destAccountId]');
      if ($destSel.length) initial.destAccountId = (($destSel.val() as string) || initial.destAccountId);
      const $catSel = $form.find('select[name=categoryId]');
      if ($catSel.length) initial.categoryId = (($catSel.val() as string) || initial.categoryId);
      const $stSel = $form.find('select[name=status]');
      if ($stSel.length) initial.status = (($stSel.val() as string) || initial.status);
      const $ccSel = $form.find('select[name=costCenterId]');
      if ($ccSel.length) initial.costCenterId = (($ccSel.val() as string) || initial.costCenterId);
      const $cardSel = $form.find('select[name=cardId]');
      if ($cardSel.length) initial.cardId = (($cardSel.val() as string) || initial.cardId);
      const $estornoChk = $form.find('input[name=estorno]');
      if ($estornoChk.length) initial.isEstorno = $estornoChk.is(':checked');
      const $notesTa = $form.find('textarea[name=notes]');
      if ($notesTa.length) initial.notes = (($notesTa.val() as string) || initial.notes);

      $form.find('input[name=type]').val(t);
      m.$body.find('[data-region=type-row]').html(typeToggleHtml(TX_TYPE_OPTIONS, t, { disabled: isTransferEdit, disabledTitle: TX_TYPE_DISABLED_TITLE }));
      m.$body.find('[data-region=grid]').html(buildGridHtml(t));
      bindAmountMask();
      bindNotesCounter();
    });

    // "+ Nova categoria": quick-create a category inline, then select it.
    m.$body.on('click', '[data-act=new-category]', (e) => {
      e.preventDefault();
      const nature = natureForType($form.find('input[name=type]').val());
      if (!nature) return; // transfer has no category
      openCategoryCreateModal(deps.cache.categories(), nature, (created) => {
        const c = created as { id: string; name?: string; parentId?: string | null };
        const $sel = $form.find('select[name=categoryId]');
        $sel.find('option').each(function () {
          if (!(this as HTMLOptionElement).value) $(this).remove();
        });
        if (!$sel.find('option[value="' + esc(c.id) + '"]').length) {
          $sel.append('<option value="' + esc(c.id) + '">' + esc(quickCategoryLabel(deps.cache.categories(), c)) + '</option>');
        }
        $sel.val(String(c.id));
        refreshSearchSelect(ids.category);
        initial.categoryId = String(c.id);
      });
    });

    // "+ Nova tag": quick-create a tag inline, then mark it selected.
    m.$body.on('click', '[data-act=new-tag]', (e) => {
      e.preventDefault();
      openTagCreateModal((created) => {
        const t = created as { id: string; name?: string; color?: string | null };
        const id = String(t.id);
        if (initial.tagIds.indexOf(id) === -1) initial.tagIds.push(id);
        appendTagRow('tx', { id: t.id, name: t.name || '', color: t.color || null });
      });
    });

    function submit($form: JQuery): Promise<unknown> | null {
      const type = $form.find('input[name=type]').val() as string;
      submittedType = type;
      const amtRaw = $form.find('input[name=amount]').val() as string;
      const amt = parseCurrency(amtRaw);
      const date = $form.find('input[name=date]').val() as string;
      const accountId = $form.find('select[name=accountId]').val() as string;

      if (type === 'transfer') {
        const destAccountId = $form.find('select[name=destAccountId]').val() as string;
        if (!accountId) {
          toast('Selecione a conta de origem', 'error');
          return null;
        }
        if (!destAccountId) {
          toast('Selecione a conta de destino', 'error');
          return null;
        }
        if (!Transaction.isValidTransfer(accountId, destAccountId)) {
          toast('Origem e destino devem ser contas diferentes', 'error');
          return null;
        }
        if (!date) {
          toast('Informe a data', 'error');
          return null;
        }
        if (!isFinite(amt) || amt <= 0) {
          $form.find('input[name=amount]').trigger('focus');
          return null;
        }

        return deps.transactionService
          .transfer({ fromAccountId: accountId, toAccountId: destAccountId, date, amount: Number(amt.toFixed(2)) })
          .then(() => {
            // Converting an existing lançamento into a transfer: drop the original so its amount
            // isn't counted twice alongside the new transfer pair.
            if (isEdit) return deps.transactionService.remove((existing as TransactionResponse).accountId as string, (existing as TransactionResponse).id as string);
          });
      }

      const description = (($form.find('input[name=description]').val() as string) || '').trim();
      if (!description) {
        $form.find('input[name=description]').trigger('focus');
        return null;
      }
      if (!isFinite(amt) || amt <= 0) {
        $form.find('input[name=amount]').trigger('focus');
        return null;
      }
      const categoryId = $form.find('select[name=categoryId]').val() as string;
      const costCenterId = $form.find('select[name=costCenterId]').val() as string;
      const status = $form.find('select[name=status]').val() as TransactionRequest['status'];
      const isEstorno = $form.find('input[name=estorno]').is(':checked');

      if (!accountId) {
        toast('Selecione uma conta', 'error');
        return null;
      }
      if (!categoryId) {
        toast('Selecione uma categoria', 'error');
        return null;
      }
      if (!costCenterId) {
        toast('Selecione o centro de custo', 'error');
        return null;
      }

      const signed = Transaction.signedAmount(type, amt) * (isEstorno ? -1 : 1);
      const notes = (($form.find('textarea[name=notes]').val() as string) || '').trim() || undefined;
      const payload: TransactionRequest = {
        description,
        amount: Number(signed.toFixed(2)),
        date,
        categoryId,
        accountId,
        costCenterId,
        status,
        type: type as TransactionRequest['type'],
        notes,
        cardId: ($form.find('select[name=cardId]').val() as string) || undefined,
        tagIds: initial.tagIds,
      };

      return isEdit ? deps.transactionService.update((existing as TransactionResponse).id as string, payload) : deps.transactionService.create(payload);
    }

    return m;
  };
}
