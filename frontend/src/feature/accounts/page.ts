/** pages/accounts — Contas Bancárias (CRUD em grid de cards).
 *
 * Cartões de crédito não são mais um tipo de conta: são entidades filhas (last4 apenas), geridas
 * dentro do modal de edição da conta real a que pertencem. O limite de crédito/cheque especial e
 * o ciclo de fatura (fechamento/vencimento) são configuração da conta (seção "Limites e Fatura",
 * sempre visível).
 */
import $ from 'jquery';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import type { Card as CardWire } from '../../api/types.ts';
import * as AccountDomain from '../../core/kernel/_0_domain/account.ts';
import { esc, fmt, maskCurrency, parseCurrency, sortByName } from '../../core/kernel/_0_domain/format.ts';
import { bindCurrencyMask, bindSwatches, byId, formModal, runMutation } from '../../core/kernel/_2_infrastructure/primary/helpers.ts';
import { deleteWithLinkedFallback, linkedDeleteDialog, pluralTransactions } from '../../core/kernel/_2_infrastructure/primary/delete-dialog.ts';
import { icon } from '../../core/kernel/_2_infrastructure/primary/icons.ts';
import { cachePage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { PALETTE } from '../../core/kernel/_2_infrastructure/primary/pickers.ts';
import { rowActionBtn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { btn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { colorNameFieldHtml } from '../../core/kernel/_2_infrastructure/primary/ui/color-name-field.ts';
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { toast } from '../../core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type { AccountService } from './service.ts';

const TYPE_LABELS: Record<string, string> = {
  CHECKING: 'Conta Corrente',
  INVESTMENT: 'Investimento',
};

interface AccountsPageState extends PageState {
  accounts: Account[];
}

function colorOf(a: Account): string {
  return a.color || '#6366F1';
}

function iconForType(type: string): string {
  return type === 'INVESTMENT' ? 'trendingUp' : 'building';
}

// Optional numeric fields (Limites e Fatura): blank input → null payload.
function parseOptionalCurrency(raw: string | undefined): number | undefined {
  const n = parseCurrency(raw);
  return isFinite(n) ? +n.toFixed(2) : undefined;
}

function parseOptionalDay(raw: string | undefined): number | undefined {
  const n = parseInt(raw || '', 10);
  return isFinite(n) && n >= 1 && n <= 31 ? n : undefined;
}

function renderCard(a: Account): JQuery {
  const color = colorOf(a);
  const typeLabel = TYPE_LABELS[a.type] || 'Conta';
  const iconName = iconForType(a.type);

  const displayValue = AccountDomain.currentBalance(a);
  const valueColor = displayValue >= 0 ? 'var(--income)' : 'var(--expense)';
  const active = a.active !== false;

  const $card = $('<div class="card account-card" data-card="account" data-id="' + esc(a.id) + '"></div>');

  $card.append('<div class="account-card-band" style="background:' + esc(color) + ';"></div>');

  const $head = $('<div class="account-card-head"></div>');

  const $left = $('<div class="account-card-left"></div>');
  $left.append('<div class="account-card-icon" style="background:' + esc(color) + '26;color:' + esc(color) + ';">' + icon(iconName, 20) + '</div>');
  $left.append('<div class="account-card-title"><div class="account-card-name">' + esc(a.name) + '</div><div class="account-card-type">' + esc(typeLabel) + '</div></div>');
  $head.append($left);

  const $actions = $('<div class="account-card-actions"></div>');
  $actions.append(rowActionBtn('edit', 'Editar', a.id));
  $actions.append(rowActionBtn('trash', 'Excluir', a.id, true));
  $head.append($actions);

  $card.append($head);

  $card.append('<div class="account-card-value"><div class="account-card-value-label">Saldo atual</div><div class="account-card-value-amount" style="color:' + valueColor + ';">' + esc(fmt(displayValue)) + '</div></div>');

  if (AccountDomain.hasCards(a)) {
    const last4Line = a.cards.map((c) => '•••• ' + c.last4).join('  ·  ');
    $card.append('<div class="account-card-cards">' + icon('creditCard', 13) + '<span>' + esc(last4Line) + '</span></div>');
  }

  if (!active) {
    $card.append('<div class="account-card-footer">Inativa</div>');
  }

  return $card;
}

export function createAccountsPage(service: AccountService): Page {
  let state: AccountsPageState | null = null;

  function findAccount(id: string): Account | null {
    return state ? byId(state.accounts, id) : null;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $header = pageHeader({
      title: 'Contas Bancárias',
      actions: btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Conta', attrs: 'data-act="new"' }),
    });

    let $body: JQuery;
    if (!state.accounts.length) {
      $body = $(emptyState({ icon: 'building', title: 'Nenhuma conta cadastrada', desc: 'Clique em "Nova Conta" para começar.' }));
    } else {
      $body = $('<div data-region="accounts-grid" style="display:grid;gap:14px;grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));"></div>');
      state.accounts
        .slice()
        .sort(sortByName)
        .forEach((a) => $body.append(renderCard(a)));
    }

    $root.empty().append($header).append($body);
  }

  function openFormModal(existing: Account | null): void {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = {
      name: 'acc-name-' + uniq, type: 'acc-type-' + uniq, color: 'acc-color-' + uniq, active: 'acc-active-' + uniq,
      limit: 'acc-limit-' + uniq, overdraft: 'acc-overdraft-' + uniq, closingDay: 'acc-closing-' + uniq, dueDay: 'acc-due-' + uniq,
      cardLast4: 'acc-card-last4-' + uniq,
    };

    const initial = {
      name: isEdit ? existing?.name || '' : '',
      type: isEdit ? existing?.type || 'CHECKING' : 'CHECKING',
      color: isEdit ? existing?.color || PALETTE.swatches[0] : PALETTE.swatches[0],
      active: isEdit ? existing?.active !== false : true,
      creditLimit: isEdit && existing?.creditLimit ? maskCurrency(existing.creditLimit) : '',
      overdraftLimit: isEdit && existing?.overdraftLimit ? maskCurrency(existing.overdraftLimit) : '',
      closingDay: isEdit && existing?.closingDay ? String(existing.closingDay) : '',
      dueDay: isEdit && existing?.dueDay ? String(existing.dueDay) : '',
    };

    // Local, optimistic copy of this account's cards — kept in sync with the server on every
    // add/remove so the chip list reflects reality without waiting for the SSE round-trip.
    let cards: CardWire[] = isEdit ? (existing?.cards || []).slice() : [];

    const typeOptions = ([['CHECKING', 'Corrente'], ['INVESTMENT', 'Investimento']] as const)
      .map((p) => {
        const sel = p[0] === initial.type ? ' selected' : '';
        return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
      })
      .join('');

    function cardsRegionHtml(): string {
      if (!isEdit) {
        return '<p style="font-size:12px;color:var(--text-muted);">Salve a conta antes de adicionar cartões.</p>';
      }
      const chips = cards
        .map((c) => {
          const active = c.active !== false;
          const actionHtml = active
            ? '<button type="button" data-act="remove-card" data-id="' + esc(c.id) + '" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;display:inline-flex;" title="Remover cartão">' + icon('x', 12) + '</button>'
            : '<button type="button" data-act="reactivate-card" data-id="' + esc(c.id) + '" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;display:inline-flex;" title="Reativar cartão">' + icon('eye', 12) + '</button>';
          return '<span class="chip" data-card-id="' + esc(c.id) + '" style="' + (active ? '' : 'opacity:0.55;') + '"><span>•••• ' + esc(c.last4) + (active ? '' : ' · Inativo') + '</span><span class="chip-actions">' + actionHtml + '</span></span>';
        })
        .join('');
      const chipsHtml = chips ? '<div class="chip-group" style="margin-bottom:10px;">' + chips + '</div>' : '<p style="font-size:12px;color:var(--text-muted);margin-bottom:10px;">Nenhum cartão nesta conta.</p>';
      return (
        chipsHtml +
        '<div style="display:flex;gap:8px;align-items:center;">' +
        '<input id="' + ids.cardLast4 + '" data-field="new-card-last4" type="text" maxlength="4" inputmode="numeric" placeholder="0000" style="width:90px;" />' +
        '<button type="button" data-act="add-card" class="btn btn-secondary btn-sm">' + icon('plus', 13) + '<span>Adicionar</span></button>' +
        '</div>'
      );
    }

    const bodyHtml =
      '<form data-form="acc" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' + colorNameFieldHtml({ colorId: ids.color, nameId: ids.name, color: initial.color, nameValue: initial.name, placeholder: 'Ex: Itaú, Nubank...', swatchMarginTop: '6px', swatches: PALETTE.swatches }) + '</div>' +
          '<div class="form-group"><label class="form-label" for="' + ids.type + '">Tipo</label><select id="' + ids.type + '" name="type">' + typeOptions + '</select></div>' +
          '<div class="form-group full">' +
            '<label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;font-size:13px;font-weight:500;color:var(--text-primary);">' +
              '<input id="' + ids.active + '" name="active" type="checkbox"' + (initial.active ? ' checked' : '') + ' style="width:16px;height:16px;accent-color:var(--accent);" />Conta ativa' +
            '</label>' +
          '</div>' +
        '</div>' +
        '<div style="margin-top:14px;padding-top:14px;border-top:1px solid var(--border);">' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:10px;">Limites e Fatura</p>' +
          '<div class="form-grid">' +
            '<div class="form-group"><label class="form-label" for="' + ids.limit + '">Limite de crédito</label><input id="' + ids.limit + '" name="creditLimit" type="text" inputmode="numeric" placeholder="0,00" value="' + esc(initial.creditLimit) + '" /></div>' +
            '<div class="form-group"><label class="form-label" for="' + ids.overdraft + '">Cheque especial</label><input id="' + ids.overdraft + '" name="overdraftLimit" type="text" inputmode="numeric" placeholder="0,00" value="' + esc(initial.overdraftLimit) + '" /></div>' +
            '<div class="form-group"><label class="form-label" for="' + ids.closingDay + '">Fechamento (dia)</label><input id="' + ids.closingDay + '" name="closingDay" type="number" min="1" max="31" value="' + esc(initial.closingDay) + '" /></div>' +
            '<div class="form-group"><label class="form-label" for="' + ids.dueDay + '">Vencimento (dia)</label><input id="' + ids.dueDay + '" name="dueDay" type="number" min="1" max="31" value="' + esc(initial.dueDay) + '" /></div>' +
          '</div>' +
        '</div>' +
        '<div data-region="cards-section" style="margin-top:14px;padding-top:14px;border-top:1px solid var(--border);">' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:10px;">Cartões</p>' +
          '<div data-region="cards-body">' + cardsRegionHtml() + '</div>' +
        '</div>' +
      '</form>';

    const m = formModal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      formName: 'acc',
      body: bodyHtml,
      onSubmit: ($form) => {
        const name = (($form.find('input[name=name]').val() as string) || '').trim();
        if (!name) {
          $form.find('input[name=name]').trigger('focus');
          return null;
        }
        const payload = {
          name,
          type: $form.find('select[name=type]').val() as string,
          color: ($form.find('input[name=color]').val() as string) || PALETTE.swatches[0],
          active: $form.find('input[name=active]').is(':checked'),
          creditLimit: parseOptionalCurrency($form.find('input[name=creditLimit]').val() as string),
          overdraftLimit: parseOptionalCurrency($form.find('input[name=overdraftLimit]').val() as string),
          closingDay: parseOptionalDay($form.find('input[name=closingDay]').val() as string),
          dueDay: parseOptionalDay($form.find('input[name=dueDay]').val() as string),
        };
        return isEdit ? service.update((existing as Account).id, payload) : service.create(payload);
      },
      success: () => (isEdit ? 'Conta atualizada' : 'Conta criada'),
      failure: 'Falha ao salvar conta',
      // SSE UPSERT will refresh CacheStore → AccountService.onChange → re-render.
    });

    const $form = m.$body.find('form[data-form=acc]');
    const $color = $form.find('input[name=color]');
    const $limit = $form.find('input[name=creditLimit]');
    const $overdraft = $form.find('input[name=overdraftLimit]');

    bindSwatches(m, $color);
    bindCurrencyMask($limit);
    bindCurrencyMask($overdraft);

    function renderCardsBody(): void {
      m.$body.find('[data-region=cards-body]').html(cardsRegionHtml());
    }

    m.$el.on('click', '[data-act=add-card]', function () {
      if (!isEdit) return;
      const $input = m.$body.find('#' + ids.cardLast4);
      const last4 = ((($input.val() as string) || '')).trim();
      if (!/^\d{4}$/.test(last4)) {
        toast('Informe os 4 dígitos do cartão', 'error');
        return;
      }
      const $btn = $(this);
      runMutation(service.addCard((existing as Account).id, { last4 }), {
        $btn,
        success: 'Cartão adicionado',
        failure: 'Falha ao adicionar cartão',
        onDone: (card) => {
          if (card) cards.push(card);
          renderCardsBody();
        },
      });
    });

    function doRemoveCard(cardId: string, opts?: { strategy: 'MOVE' | 'DELETE'; targetId?: string }, dialogModal?: import('../../core/kernel/_2_infrastructure/primary/ui/modal.ts').Modal, dialogReEnable?: () => void): void {
      const $chip = m.$body.find('[data-card-id="' + cardId + '"]');
      $chip.css('opacity', '0.5');
      runMutation(service.removeCard((existing as Account).id, cardId, opts), {
        modal: dialogModal,
        success: 'Cartão removido',
        failure: 'Falha ao remover cartão',
        onDone: () => {
          cards = cards.filter((c) => String(c.id) !== String(cardId));
          renderCardsBody();
        },
        onError: (err) => {
          $chip.css('opacity', '1');
          const apiErr = err as { status?: number; code?: string; count?: number };
          if (!opts && apiErr && apiErr.status === 409 && apiErr.code === 'LINKED_TRANSACTIONS') {
            openLinkedCardDialog(cardId, apiErr.count || 0);
            return true;
          }
          if (dialogReEnable) dialogReEnable();
        },
      });
    }

    function openLinkedCardDialog(cardId: string, count: number): void {
      const card = cards.filter((c) => String(c.id) === String(cardId))[0];
      if (!card) return;
      const otherActive = cards.filter((c) => String(c.id) !== String(cardId) && c.active !== false);
      const options = [
        {
          value: 'MOVE', label: 'Mover para outro cartão',
          hint: 'As transações deste cartão passam para o cartão escolhido (mesma conta).',
          choices: otherActive.map((c) => ({ value: c.id || '', label: '•••• ' + c.last4 })),
        },
        { value: 'DELETE', label: 'Excluir transações', danger: true, hint: 'Apaga o cartão e ' + pluralTransactions(count) + '.' },
        { value: 'INATIVAR', label: 'Inativar cartão', hint: 'Some de novos lançamentos; o histórico é mantido.' },
      ];

      linkedDeleteDialog({
        title: 'Cartão com transações vinculadas',
        intro: 'O cartão <strong>•••• ' + esc(card.last4) + '</strong> tem ' + pluralTransactions(count) + '. Escolha o que fazer:',
        options,
        onConfirm: (choice, m2, reEnable) => {
          if (choice.strategy === 'INATIVAR') {
            runMutation(service.setCardActive((existing as Account).id, cardId, false), {
              modal: m2,
              success: 'Cartão inativado',
              failure: 'Falha ao inativar cartão',
              onDone: (updated) => {
                if (updated) cards = cards.map((c) => (String(c.id) === String(updated.id) ? updated : c));
                renderCardsBody();
              },
              onError: () => {
                reEnable();
              },
            });
            return;
          }
          doRemoveCard(cardId, { strategy: choice.strategy as 'MOVE' | 'DELETE', targetId: choice.targetId || undefined }, m2, reEnable);
        },
      });
    }

    m.$el.on('click', '[data-act=remove-card]', function () {
      if (!isEdit) return;
      const cardId = $(this).attr('data-id') as string;
      const card = cards.filter((c) => String(c.id) === String(cardId))[0];
      if (!card) return;
      doRemoveCard(cardId);
    });

    m.$el.on('click', '[data-act=reactivate-card]', function () {
      if (!isEdit) return;
      const cardId = $(this).attr('data-id') as string;
      runMutation(service.setCardActive((existing as Account).id, cardId, true), {
        success: 'Cartão reativado',
        failure: 'Falha ao reativar cartão',
        onDone: (updated) => {
          if (updated) cards = cards.map((c) => (String(c.id) === String(updated.id) ? updated : c));
          renderCardsBody();
        },
      });
    });

    m.$body.on('keydown', '[data-field=new-card-last4]', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        m.$el.find('[data-act=add-card]').trigger('click');
      }
    });
  }

  function openDeleteModal(target: Account): void {
    deleteWithLinkedFallback({
      title: 'Excluir Conta',
      body: '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">Tem certeza que deseja excluir a conta <strong>' + esc(target.name) + '</strong>? Esta ação não pode ser desfeita.</p>',
      remove: () => service.remove(target.id),
      success: 'Conta excluída',
      failure: 'Falha ao excluir conta',
      // SSE DELETE will refresh CacheStore → AccountService.onChange → re-render.
      linked: {
        title: 'Conta com transações vinculadas',
        intro: (count) => 'A conta <strong>' + esc(target.name) + '</strong> tem ' + pluralTransactions(count) + '. Escolha o que fazer:',
        options: (count) => {
          const otherActive = (state?.accounts || []).filter((a) => String(a.id) !== String(target.id) && a.active !== false);
          return [
            {
              value: 'MOVE', label: 'Mover para outra conta',
              hint: 'Transações e cartões desta conta passam para a conta escolhida.',
              choices: otherActive.map((a) => ({ value: a.id, label: a.name })),
            },
            { value: 'DELETE', label: 'Excluir transações', danger: true, hint: 'Apaga a conta e ' + pluralTransactions(count) + '.' },
            { value: 'INATIVAR', label: 'Inativar conta', hint: 'Some do lançamento de novas transações; o histórico é mantido.' },
          ];
        },
        dispatch: (choice): Promise<unknown> => {
          if (choice.strategy === 'INATIVAR') {
            return service.update(target.id, {
              name: target.name, type: target.type, color: target.color || '', active: false,
              creditLimit: target.creditLimit || undefined,
              overdraftLimit: target.overdraftLimit || undefined,
              closingDay: target.closingDay || undefined,
              dueDay: target.dueDay || undefined,
            });
          }
          return service.remove(target.id, { strategy: choice.strategy as 'MOVE' | 'DELETE', targetId: choice.targetId || undefined });
        },
        success: (choice) => (choice.strategy === 'INATIVAR' ? 'Conta inativada' : 'Conta excluída'),
        failure: 'Falha ao excluir conta',
      },
    });
  }

  function bindRoot($root: JQuery): void {
    $root.on('click.accs', '[data-act=new]', () => openFormModal(null));
    $root.on('click.accs', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const acc = findAccount(id);
      if (acc) openFormModal(acc);
    });
    $root.on('click.accs', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id') as string;
      const acc = findAccount(id);
      if (acc) openDeleteModal(acc);
    });
  }

  return cachePage<AccountsPageState>({
    ns: '.accs',
    state: () => {
      state = { accounts: [] };
      return state;
    },
    sync: (s) => {
      s.accounts = service.listCached();
    },
    subscribe: (cb) => service.onChange(cb),
    render,
    bind: bindRoot,
  });
}
