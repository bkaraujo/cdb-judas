/** pages/settings — Configurações: shell com abas (Perfil, Aparência).
 * Acessada pelo avatar da sidebar (#/settings); não faz parte do menu lateral. */
import $ from 'jquery';
import { esc } from '../../core/kernel/_0_domain/format.ts';
import { createPage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { btn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { tabs } from '../../core/kernel/_2_infrastructure/primary/ui/tabs.ts';
import { toast } from '../../core/kernel/_2_infrastructure/primary/ui/toast.ts';

export interface SettingsAuthPort {
  decodeUser(): string;
  name(): string | null;
  setName(n: string): void;
  setUser(u: string): void;
}
export interface SettingsSelfPort {
  updateName(name: string | null): Promise<{ name?: string; username?: string } | null>;
}
export interface SettingsThemePort {
  get(): string;
  set(t: string): void;
}
export interface SettingsSidebarPort {
  refreshUser(): void;
}

export interface SettingsPageDeps {
  authStore: SettingsAuthPort;
  selfService: SettingsSelfPort;
  theme: SettingsThemePort;
  sidebar?: SettingsSidebarPort;
}

interface SettingsPageState extends PageState {
  tab: 'perfil' | 'aparencia';
}

export function createSettingsPage(deps: SettingsPageDeps): Page {
  let state: SettingsPageState | null = null;

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $header = pageHeader({ title: 'Configurações' });
    const $tabs = tabs(
      [{ id: 'perfil', label: 'Perfil' }, { id: 'aparencia', label: 'Aparência' }],
      state.tab,
      (id) => {
        if (state) state.tab = id as SettingsPageState['tab'];
        renderBody();
      },
    );
    const $body = $('<div data-region="tab-body" style="margin-top:18px;"></div>');

    $root.empty().append($header).append($tabs).append($body);
    renderBody();
  }

  function renderBody(): void {
    if (!state?.$root) return;
    const $body = state.$root.find('[data-region=tab-body]');
    $body.empty();
    $body.append(state.tab === 'aparencia' ? renderAparencia() : renderPerfil());
  }

  function renderPerfil(): JQuery {
    const username = deps.authStore.decodeUser();
    const name = deps.authStore.name() || '';
    const nameId = 'profile-name-' + Date.now();

    const $card = $('<div class="card" style="max-width:520px;"></div>');
    const $form = $(
      '<form data-form="profile" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label">Usuário (login)</label>' +
            '<input type="text" value="' + esc(username) + '" disabled readonly />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome de exibição</label>' +
            '<input id="' + nameId + '" name="name" type="text" maxlength="80" ' +
              'placeholder="Como você quer ser chamado" value="' + esc(name) + '" />' +
            '<p style="font-size:12px;color:var(--text-muted);margin-top:6px;">Deixe em branco para usar seu usuário como exibição.</p>' +
          '</div>' +
        '</div>' +
      '</form>',
    );
    const $save = btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="save-profile" type="submit"',
    });
    const $footer = $('<div style="display:flex;gap:10px;margin-top:6px;"></div>').append($save);
    return $card.append($form).append($footer);
  }

  function saveProfile(): void {
    if (!state?.$root) return;
    const $input = state.$root.find('form[data-form=profile] input[name=name]');
    const value = ((($input.val() as string) || '')).trim();
    const $btn = state.$root.find('[data-act=save-profile]').prop('disabled', true);

    deps.selfService
      .updateName(value)
      .then((me) => {
        deps.authStore.setName((me && me.name) || '');
        if (me && me.username) deps.authStore.setUser(me.username);
        if (deps.sidebar) deps.sidebar.refreshUser();
        toast('Perfil atualizado', 'success');
        $btn.prop('disabled', false);
      })
      .catch((err: { message?: string }) => {
        $btn.prop('disabled', false);
        toast(err && err.message ? err.message : 'Falha ao salvar perfil', 'error');
      });
  }

  function renderAparencia(): JQuery {
    const current = deps.theme.get();
    const $card = $('<div class="card" style="max-width:520px;"></div>');
    $card.append('<label class="form-label">Tema</label>');
    const $opts = $('<div data-region="theme-opts" style="display:flex;gap:10px;margin-top:8px;"></div>');
    ([{ id: 'light', label: 'Claro', icon: 'sun' }, { id: 'dark', label: 'Escuro', icon: 'moon' }] as const).forEach((opt) => {
      $opts.append(
        btn({
          variant: current === opt.id ? 'primary' : 'secondary', size: 'md',
          icon: opt.icon, label: opt.label,
          attrs: 'data-theme-opt="' + opt.id + '" type="button"',
        }),
      );
    });
    return $card.append($opts);
  }

  function bindRoot($root: JQuery): void {
    $root.on('submit.settings', 'form[data-form=profile]', (e) => {
      e.preventDefault();
      saveProfile();
    });
    $root.on('click.settings', '[data-act=save-profile]', (e) => {
      e.preventDefault();
      saveProfile();
    });
    $root.on('click.settings', '[data-theme-opt]', function () {
      deps.theme.set($(this).attr('data-theme-opt') as string);
      renderBody();
    });
  }

  return createPage<SettingsPageState>({
    ns: '.settings',
    state: () => {
      state = { tab: 'perfil' };
      return state;
    },
    bind: bindRoot,
    render,
  });
}
