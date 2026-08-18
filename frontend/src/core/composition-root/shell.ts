/** Scaffold de DOM de topo (`#app` → sidebar + área principal) + montagem do sidebar. */
import $ from 'jquery';
import type { Router } from '../kernel/_2_infrastructure/primary/router.ts';
import type { Sidebar } from '../kernel/_2_infrastructure/primary/sidebar.ts';

export interface Shell {
  buildShell(): void;
  mountChrome(initialScreenId: string): void;
  navTo(id: string): void;
}

export function createShell(sidebar: Sidebar, router: Router): Shell {
  function buildShell(): void {
    const $app = $('#app');
    $app.html(
      '<div class="app-layout">' +
        '<aside id="sidebar"></aside>' +
        '<div class="main-area">' +
          '<main class="page-content" id="page"></main>' +
        '</div>' +
      '</div>',
    );
  }

  function navTo(id: string): void {
    router.go(id);
  }

  function mountChrome(initialScreenId: string): void {
    sidebar.mount($('#sidebar'), { current: initialScreenId, onNav: navTo });
  }

  return { buildShell, mountChrome, navTo };
}
