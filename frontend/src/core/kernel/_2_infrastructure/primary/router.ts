/**
 * Hash-based router. Mounts `registry[id]` into #page.
 *
 * A rota é `#/<id>` ou `#/<id>/<param>` — só `card-statement` usa o parâmetro hoje (o id do
 * cartão), repassado como 2º argumento de `mount()`. O que se persiste em `cdb-screen` é a rota
 * INTEIRA, com o parâmetro: guardar só o id faria o boot em `#/card-statement` cair numa tela sem
 * cartão.
 */
import $ from 'jquery';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import type { Page } from '@/core/kernel/_2_infrastructure/primary/page.ts';

export type PageId =
  | 'accounts'
  | 'accounts-payable'
  | 'budget'
  | 'card-statement'
  | 'categories'
  | 'cost-centers'
  | 'credit-cards'
  | 'dashboard'
  | 'import-rules'
  | 'installments'
  | 'reports'
  | 'settings'
  | 'statement'
  | 'tags'
  | 'transactions';

export type PageRegistry = Record<PageId, Page>;

const SCREENS: Record<PageId, string> = {
  dashboard: 'Visão Geral',
  transactions: 'Lançamentos',
  installments: 'Parcelamentos',
  'accounts-payable': 'A Pagar e Receber',
  statement: 'Extrato de Contas',
  'credit-cards': 'Cartões de Crédito',
  // Fora do sidebar (sidebar-nav.ts): só se chega por link da linha de fatura ou do botão
  // "Ver fatura" — precisa do id do cartão no path.
  'card-statement': 'Extrato do Cartão',
  budget: 'Metas / Orçamento',
  reports: 'Relatórios',
  categories: 'Categorias',
  'cost-centers': 'Centros de Custo',
  accounts: 'Contas Bancárias',
  tags: 'Tags',
  'import-rules': 'Regras de Nomenclatura',
  settings: 'Configurações',
};

const KEY_SCREEN = 'cdb-screen';

interface ParsedRoute {
  id: PageId;
  param: string | null;
}

/** Porta local — evita import estático de `sidebar.ts` (que referencia `Router.go` pro avatar), um
 * ciclo real no `window.*` original. composition-root (Fase 7) injeta os dois lados um no outro. */
export interface RouterSidebarPort {
  setCurrent(id: string): void;
}

export interface Router {
  SCREENS: Record<PageId, string>;
  start(): void;
  go(route: string): void;
  current(): PageId | null;
}

export interface RouterDeps {
  sidebar?: RouterSidebarPort;
}

export function createRouter(registry: PageRegistry, deps: RouterDeps = {}): Router {
  const state: { current: PageId | null; route: string | null } = { current: null, route: null };

  /* 'card-statement/abc' → { id: 'card-statement', param: 'abc' }; rota desconhecida → null. */
  function parseRoute(route: string | null | undefined): ParsedRoute | null {
    const clean = String(route || '').replace(/^#\/?/, '');
    if (!clean) return null;
    const slash = clean.indexOf('/');
    const id = slash < 0 ? clean : clean.slice(0, slash);
    if (!Object.prototype.hasOwnProperty.call(SCREENS, id)) return null;
    return { id: id as PageId, param: slash < 0 ? null : clean.slice(slash + 1) };
  }

  function routeOf(parsed: ParsedRoute): string {
    return parsed.param ? parsed.id + '/' + parsed.param : parsed.id;
  }

  function apply(parsed: ParsedRoute): void {
    const id = parsed.id;
    const route = routeOf(parsed);

    // Unmount previous
    if (state.current && registry[state.current]) {
      try {
        registry[state.current].unmount();
      } catch {
        /* swallow */
      }
    }

    state.current = id;
    state.route = route;
    localStorage.setItem(KEY_SCREEN, route);
    document.body.setAttribute('data-screen', id);

    if (deps.sidebar) deps.sidebar.setCurrent(id);

    const $page = $('#page').empty();
    const page = registry[id];
    if (page) page.mount($page, parsed.param);
    else $page.html(emptyState({ icon: 'alertCircle', title: 'Tela não encontrada', desc: id }));
  }

  function resolve(): ParsedRoute {
    return parseRoute(location.hash) || parseRoute(localStorage.getItem(KEY_SCREEN)) || { id: 'dashboard', param: null };
  }

  function onHashChange(): void {
    const parsed = resolve();
    if (routeOf(parsed) === state.route) return;
    apply(parsed);
  }

  return {
    SCREENS,
    start() {
      $(window).on('hashchange', onHashChange);
      const parsed = resolve();
      if (!location.hash) location.hash = '#/' + routeOf(parsed);
      else apply(parsed);
    },
    go(route) {
      const parsed = parseRoute(route) || { id: 'dashboard' as PageId, param: null };
      const hash = '#/' + routeOf(parsed);
      if (location.hash === hash) apply(parsed);
      else location.hash = hash;
    },
    current: () => state.current,
  };
}
