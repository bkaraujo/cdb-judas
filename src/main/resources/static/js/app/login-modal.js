/* app/login-modal.js — primary adapter for authentication UI.
 * Persistent modal so users can't dismiss it accidentally. */
(function () {
  let _loginModal = null;

  function promptLogin() {
    if (_loginModal) return _loginModal;
    const formHtml =
      '<div class="form-grid">' +
        '<div class="form-group full">' +
          '<label class="form-label">Usuário</label>' +
          '<input type="text" id="login-user" autocomplete="username">' +
        '</div>' +
        '<div class="form-group full">' +
          '<label class="form-label">Senha</label>' +
          '<input type="password" id="login-pass" autocomplete="current-password">' +
        '</div>' +
        '<div class="form-group full">' +
          '<p id="login-error" style="color:var(--expense);font-size:12px;display:none;"></p>' +
        '</div>' +
      '</div>';

    const footer =
      '<button class="btn btn-primary btn-md" data-act="login-submit">Entrar</button>';

    const m = window.modal({
      title: 'Entrar',
      body: formHtml,
      footer: footer,
      persistent: true,
      onClose: function () { _loginModal = null; },
    });
    _loginModal = m;
    m.open();

    function submit() {
      const u = m.$el.find('#login-user').val();
      const p = m.$el.find('#login-pass').val();
      if (!u || !u.trim()) {
        m.$el.find('#login-error').text('O ID do usuário deve ser informado').show();
        return;
      }
      if (!p) {
        m.$el.find('#login-error').text('A senha deve ser informada').show();
        return;
      }
      m.$el.find('[data-act=login-submit]').attr('disabled', true);
      window.App.SessionService.login(u, p)
        .then(function () {
          _loginModal = null;
          m.close();
          window.App.SessionService.startSSE();
          window.Router.start();
        })
        .catch(function (e) {
          const msg = e.status === 401
            ? 'Usuário/Senha inválido'
            : !e.status
              ? 'Sistema indisponível'
              : 'Falha ao entrar';
          m.$el.find('#login-error').text(msg).show();
          m.$el.find('[data-act=login-submit]').attr('disabled', false);
        });
    }
    m.$el.on('click', '[data-act=login-submit]', submit);
    m.$el.on('keydown', 'input', function (e) { if (e.key === 'Enter') submit(); });
    return m;
  }

  window.App = window.App || {};
  window.App.promptLogin = promptLogin;
})();
