/* app/bootstrap.js — DOM-ready entry point. Assumes composition-root, shell,
 * and login-modal have already initialized their exports on window.App. */
(function () {
  window.App.SessionService.onUnauthorized(function () { window.App.promptLogin(); });

  $(function () {
    window.Theme.restore();
    window.App.Shell.buildShell();

    // Só o id da tela: a rota pode carregar parâmetro (`card-statement/<cardId>`), que o sidebar
    // não conhece — quem interpreta o path inteiro é o Router.
    const initial = ((location.hash || '').replace(/^#\/?/, '') ||
                  window.App.PreferencesService.getLastScreen() || 'dashboard').split('/')[0];

    window.App.Shell.mountChrome(initial);

    if (!window.App.SessionService.isAuthenticated()) {
      window.App.promptLogin();
    } else {
      window.App.SessionService.bootstrap()
        .then(function () {
          window.App.SessionService.startSSE();
          window.Router.start();
        })
        .catch(function () {
          window.Infra.AuthStore.clear();
          window.App.promptLogin();
        });
    }
  });
})();
