/* pages/settings.js — Configurações: shell com abas (Perfil, Aparência).
 * Acessada pelo avatar da sidebar (#/settings); não faz parte do menu lateral. */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    state = { $root: null, tab: 'perfil' };
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $header = window.pageHeader({ title: 'Configurações' });
    const $tabs = window.tabs(
      [{ id: 'perfil', label: 'Perfil' }, { id: 'aparencia', label: 'Aparência' }],
      state.tab,
      function (id) { state.tab = id; renderBody(); }
    );
    const $body = $('<div data-region="tab-body" style="margin-top:18px;"></div>');

    $root.empty().append($header).append($tabs).append($body);
    renderBody();
  }

  function renderBody() {
    const $body = state.$root.find('[data-region=tab-body]');
    $body.empty();
    $body.append(state.tab === 'aparencia' ? renderAparencia() : renderPerfil());
  }

  // ── Aba Perfil ────────────────────────────────────────────
  function renderPerfil() {
    const store = window.Infra.AuthStore;
    const username = store.decodeUser();
    const name = store.name() || '';
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
            '<p style="font-size:12px;color:var(--text-muted);margin-top:6px;">' +
              'Deixe em branco para usar seu usuário como exibição.' +
            '</p>' +
          '</div>' +
        '</div>' +
      '</form>'
    );
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="save-profile" type="submit"'
    });
    const $footer = $('<div style="display:flex;gap:10px;margin-top:6px;"></div>').append($save);
    return $card.append($form).append($footer);
  }

  function saveProfile() {
    const $input = state.$root.find('form[data-form=profile] input[name=name]');
    const value = ($input.val() || '').trim();
    const $btn = state.$root.find('[data-act=save-profile]').prop('disabled', true);

    window.App.SelfService.updateName(value)
      .then(function (me) {
        window.Infra.AuthStore.setName((me && me.name) || '');
        if (me && me.username) window.Infra.AuthStore.setUser(me.username);
        if (window.Sidebar && window.Sidebar.refreshUser) window.Sidebar.refreshUser();
        window.toast('Perfil atualizado', 'success');
        $btn.prop('disabled', false);
      })
      .catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar perfil', 'error');
      });
  }

  // ── Aba Aparência ─────────────────────────────────────────
  function renderAparencia() {
    const current = window.Theme.get();
    const $card = $('<div class="card" style="max-width:520px;"></div>');
    $card.append('<label class="form-label">Tema</label>');
    const $opts = $('<div data-region="theme-opts" style="display:flex;gap:10px;margin-top:8px;"></div>');
    [{ id: 'light', label: 'Claro', icon: 'sun' }, { id: 'dark', label: 'Escuro', icon: 'moon' }].forEach(function (opt) {
      $opts.append(window.btn({
        variant: current === opt.id ? 'primary' : 'secondary', size: 'md',
        icon: opt.icon, label: opt.label,
        attrs: 'data-theme-opt="' + opt.id + '" type="button"'
      }));
    });
    return $card.append($opts);
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('submit.settings', 'form[data-form=profile]', function (e) { e.preventDefault(); saveProfile(); });
    $root.on('click.settings', '[data-act=save-profile]', function (e) { e.preventDefault(); saveProfile(); });
    $root.on('click.settings', '[data-theme-opt]', function () {
      window.Theme.set($(this).attr('data-theme-opt'));
      renderBody();
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['settings'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
    },
    unmount: function () {
      if (state && state.$root) state.$root.off('.settings');
      state = null;
    }
  };
})();
