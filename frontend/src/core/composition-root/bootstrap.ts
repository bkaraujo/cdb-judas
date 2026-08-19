/** DOM-ready entry point. Monta app, chrome (shell + sidebar) e decide entre login e boot direto
 * da sessão já autenticada — equivalente a `web/core/composition-root/bootstrap.js`. */
import $ from 'jquery';
import { createApp } from '@/core/composition-root/composition-root.ts';
import { createLoginModal } from '@/core/composition-root/login-modal.ts';
import { createShell } from '@/core/composition-root/shell.ts';

export function bootstrap(): void {
  const app = createApp();
  const shell = createShell(app.sidebar, app.router);
  const promptLogin = createLoginModal({ session: app.session, sidebar: app.sidebar, router: app.router });

  app.session.onUnauthorized(() => promptLogin());

  $(() => {
    app.theme.restore();
    shell.buildShell();

    // Só o id da tela: a rota pode carregar parâmetro (`card-statement/<cardId>`), que o sidebar
    // não conhece — quem interpreta o path inteiro é o Router.
    const initial = ((location.hash || '').replace(/^#\/?/, '') || app.preferences.getLastScreen() || 'dashboard').split('/')[0] as string;

    shell.mountChrome(initial);

    if (!app.session.isAuthenticated()) {
      promptLogin();
    } else {
      app.session
        .bootstrap()
        .then(() => {
          app.session.startSSE();
          app.router.start();
        })
        .catch(() => {
          app.authStore.clear();
          promptLogin();
        });
    }
  });
}
