/** Adapter primário de autenticação. Modal persistente (não pode ser fechado sem logar). */
import type {Modal} from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import {modal} from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import type {Router} from '@/core/kernel/_2_infrastructure/primary/router.ts';
import type {Sidebar} from '@/core/kernel/_2_infrastructure/primary/sidebar.ts';
import type {SessionService} from '@/core/kernel/_1_application/session-service.ts';

export interface LoginModalDeps {
  session: SessionService;
  sidebar: Sidebar;
  router: Router;
}

export function createLoginModal(deps: LoginModalDeps) {
  let current: Modal | null = null;

  return function promptLogin(): Modal {
    if (current) return current;

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

    const footer = '<button class="btn btn-primary btn-md" data-act="login-submit">Entrar</button>';

    const m = modal({
      title: 'Entrar',
      body: formHtml,
      footer,
      persistent: true,
      locked: true,
      onClose: () => {
        current = null;
      },
    });
    current = m;
    m.open();

    function submit(): void {
      const u = (m.$el.find('#login-user').val() as string) || '';
      const p = (m.$el.find('#login-pass').val() as string) || '';
      if (!u.trim()) {
        m.$el.find('#login-error').text('O ID do usuário deve ser informado').show();
        return;
      }
      if (!p) {
        m.$el.find('#login-error').text('A senha deve ser informada').show();
        return;
      }
      m.$el.find('[data-act=login-submit]').attr('disabled', 'true');
      deps.session
        .login(u, p)
        .then(() => {
          current = null;
          m.close();
          deps.session.startSSE();
          deps.sidebar.refreshVersion();
          location.hash = '#/dashboard';
          deps.router.start();
        })
        .catch((e: unknown) => {
          const err = e as { status?: number } | undefined;
          const msg = err?.status === 401 ? 'Usuário/Senha inválido' : !err?.status ? 'Sistema indisponível' : 'Falha ao entrar';
          m.$el.find('#login-error').text(msg).show();
          m.$el.find('[data-act=login-submit]').attr('disabled', 'false');
        });
    }
    m.$el.on('click', '[data-act=login-submit]', submit);
    m.$el.on('keydown', 'input', (e) => {
      if (e.key === 'Enter') submit();
    });
    return m;
  };
}
