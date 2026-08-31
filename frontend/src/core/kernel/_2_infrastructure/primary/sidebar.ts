/** Collapsible sidebar with grouped nav + tooltips + theme toggle. */
import $ from 'jquery';
import {esc} from '@/core/kernel/_0_domain/format.ts';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import type {NavItem} from '@/core/kernel/_2_infrastructure/primary/sidebar-nav.ts';
import {SIDEBAR_NAV} from '@/core/kernel/_2_infrastructure/primary/sidebar-nav.ts';

export interface SidebarPreferencesPort {
  getSidebarCollapsed(): boolean;
  setSidebarCollapsed(b: boolean): void;
  getSidebarGroups(): Record<string, boolean>;
  setSidebarGroups(obj: Record<string, boolean>): void;
}

export interface SidebarAuthPort {
  name(): string | null;
  decodeUser(): string;
}

export interface SidebarThemePort {
  get(): string;
  toggle(): void;
}

export interface SidebarSystemPort {
  getVersion(): Promise<{ version?: string } | null>;
}

/** Porta local — evita import estático de `router.ts` (que por sua vez referencia `Sidebar` pra
 * `setCurrent`, um ciclo real no `window.*` original). composition-root (Fase 7) injeta os dois
 * lados um no outro em runtime. */
export interface SidebarRouterPort {
  go(route: string): void;
}

export interface ClosingPeriod {
  period: string | null;
}

/** Injetado por composition-root depois que a fatia `accounts` carrega — kernel nunca referencia
 * App.ClosingService/closingDialog por nome. */
export interface ClosingProvider {
  get(): Promise<ClosingPeriod | null>;
  openDialog(opts: { current: string | null; onChange: (p: string | null) => void }): void;
}

export interface SidebarMountOptions {
  current?: string;
  onNav?: (id: string) => void;
}

export interface Sidebar {
  mount($root: JQuery, opts?: SidebarMountOptions): void;
  refreshVersion(): void;
  setCurrent(id: string): void;
  refreshUser(): void;
  refreshState(): void;
  refreshClosing(): void;
  configureClosing(provider: ClosingProvider): void;
}

export interface SidebarDeps {
  preferences: SidebarPreferencesPort;
  authStore: SidebarAuthPort;
  theme: SidebarThemePort;
  systemService?: SidebarSystemPort;
  router?: SidebarRouterPort;
  nav?: NavItem[];
}

const DEFAULT_GROUPS: Record<string, boolean> = { movements: true, registries: false, reports: false };

export function createSidebar(deps: SidebarDeps): Sidebar {
  const NAV = deps.nav || SIDEBAR_NAV;
  let closingProvider: ClosingProvider | null = null;

  const state = {
    current: 'dashboard',
    collapsed: false,
    groups: { ...DEFAULT_GROUPS },
    version: '0.0.0',
    closing: null as string | null,
    onNav: null as ((id: string) => void) | null,
    $root: null as JQuery | null,
  };

  function loadState(): void {
    state.collapsed = deps.preferences.getSidebarCollapsed();
    const groups = deps.preferences.getSidebarGroups();
    state.groups = groups && Object.keys(groups).length ? groups : { ...DEFAULT_GROUPS };
  }

  function persist(): void {
    deps.preferences.setSidebarCollapsed(state.collapsed); // write-through: mirror + debounced PATCH /api/me
    deps.preferences.setSidebarGroups(state.groups); // local-only
  }

  // Initial letter of the display name (name ?? username); falls back to 'C' when unknown.
  function avatarInitial(): string {
    let who = deps.authStore.name() || '';
    if (!who) {
      const u = deps.authStore.decodeUser();
      if (u && u !== 'anonymous') who = u;
    }
    who = who.trim();
    return who ? who.charAt(0).toUpperCase() : 'C';
  }

  // "2026-06" -> "06/2026" for the closing tooltip.
  function closingLabel(period: string): string {
    const parts = period.split('-');
    return parts[1] + '/' + parts[0];
  }

  function isActive(item: NavItem): boolean {
    if (item.id === state.current) return true;
    if (item.children) return item.children.some((c) => c.id === state.current);
    return false;
  }

  function render(): void {
    const $s = state.$root as JQuery;
    $s.toggleClass('collapsed', state.collapsed);
    $s.empty();

    // Head
    const $head = $('<div class="sidebar-head"></div>');
    const $brand = $('<div class="sidebar-brand"></div>');
    $brand.append(
      '<div class="sidebar-logo">C</div>' +
        '<div class="sidebar-brand-text">' +
        '<span class="sidebar-brand-name">CBD Finance</span>' +
        '<span class="sidebar-version">v' + state.version + '</span>' +
        '</div>',
    );
    $head.append($brand);
    const $collapseBtn = $('<button class="icon-btn sidebar-collapse-btn">' + icon('chevronLeft', 16) + '</button>');
    $collapseBtn.on('click', () => {
      state.collapsed = true;
      persist();
      render();
    });
    $head.append($collapseBtn);
    $s.append($head);

    // Nav
    const $nav = $('<nav class="sidebar-nav"></nav>');
    NAV.forEach((item) => {
      const active = isActive(item);
      const hasChild = !!item.children;
      const open = !!state.groups[item.id];

      let classes = 'sidebar-item';
      if (active && !hasChild) classes += ' active';
      else if (active && hasChild) classes += ' group-active';

      const chevron = hasChild ? '<span class="sidebar-item-chevron">' + icon(open ? 'chevronUp' : 'chevronDown', 13) + '</span>' : '';

      const $wrap = $('<div class="sidebar-item-wrap tooltip-wrap"></div>');
      const $btn = $(
        '<button class="' + classes + '" data-id="' + esc(item.id) + '">' + icon(item.icon, 17) + '<span class="sidebar-item-label">' + esc(item.label) + '</span>' + chevron + '</button>',
      );
      $btn.on('click', () => {
        if (hasChild) {
          if (state.collapsed) {
            state.collapsed = false;
            state.groups[item.id] = true;
          } else state.groups[item.id] = !state.groups[item.id];
          persist();
          render();
        } else {
          if (state.onNav) state.onNav(item.id);
          if (state.collapsed) {
            state.collapsed = false;
            persist();
            render();
          }
        }
      });
      $wrap.append($btn);
      $wrap.append('<span class="tooltip">' + esc(item.label) + '</span>');
      $nav.append($wrap);

      if (hasChild && open && !state.collapsed) {
        const $kids = $('<div class="sidebar-children"></div>');
        (item.children as NavItem[]).forEach((child) => {
          const childActive = child.id === state.current;
          const $kw = $('<div class="tooltip-wrap"></div>');
          const $kb = $('<button class="sidebar-child' + (childActive ? ' active' : '') + '" data-id="' + esc(child.id) + '">' + icon(child.icon, 14) + '<span>' + esc(child.label) + '</span></button>');
          $kb.on('click', () => {
            if (state.onNav) state.onNav(child.id);
          });
          $kw.append($kb);
          $kids.append($kw);
        });
        $nav.append($kids);
      }
    });
    $s.append($nav);

    // Foot
    const $foot = $('<div class="sidebar-foot"></div>');
    const themeName = deps.theme.get() === 'dark' ? 'sun' : 'moon';
    const themeLbl = deps.theme.get() === 'dark' ? 'Modo Claro' : 'Modo Escuro';

    // User avatar (left) — initial of the display name (name ?? username); opens Configurações.
    const $avatarWrap = $('<div class="tooltip-wrap"></div>');
    const $avatar = $('<div class="sidebar-avatar" style="cursor:pointer;"></div>').text(avatarInitial());
    $avatar.on('click', () => {
      if (deps.router) deps.router.go('settings');
    });
    $avatarWrap.append($avatar);
    $avatarWrap.append('<span class="tooltip">Perfil</span>');
    $foot.append($avatarWrap);

    // Accounting closing lock (right, before theme) — icon reflects whether a period is set.
    const closingLbl = state.closing ? 'Fechamento: ' + esc(closingLabel(state.closing)) : 'Fechamento contábil';
    const $closingWrap = $('<div class="tooltip-wrap sidebar-foot-right"></div>');
    const $closingBtn = $('<button class="icon-btn"' + (state.closing ? ' style="color:var(--accent);"' : '') + '>' + icon(state.closing ? 'lock' : 'unlock', 16) + '</button>');
    $closingBtn.on('click', () => {
      if (!closingProvider) return;
      closingProvider.openDialog({
        current: state.closing,
        onChange: (p) => {
          state.closing = p;
          render();
        },
      });
    });
    $closingWrap.append($closingBtn);
    $closingWrap.append('<span class="tooltip">' + closingLbl + '</span>');
    $foot.append($closingWrap);

    // Theme toggle icon (right)
    const $themeWrap = $('<div class="tooltip-wrap"></div>');
    const $themeBtn = $('<button class="icon-btn">' + icon(themeName, 16) + '</button>');
    $themeBtn.on('click', () => {
      deps.theme.toggle();
      render();
    });
    $themeWrap.append($themeBtn);
    $themeWrap.append('<span class="tooltip">' + esc(themeLbl) + '</span>');
    $foot.append($themeWrap);

    if (state.collapsed) {
      const $expWrap = $('<div class="tooltip-wrap"></div>');
      const $expBtn = $('<button class="icon-btn">' + icon('chevronRight', 16) + '</button>');
      $expBtn.on('click', () => {
        state.collapsed = false;
        persist();
        render();
      });
      $expWrap.append($expBtn);
      $expWrap.append('<span class="tooltip">Expandir menu</span>');
      $foot.append($expWrap);
    }
    $s.append($foot);
  }

  return {
    mount($root, opts = {}) {
      state.$root = $root.addClass('sidebar');
      state.current = opts.current || state.current;
      state.onNav = opts.onNav || null;
      loadState();
      render();
      this.refreshVersion();
    },
    refreshVersion() {
      if (!deps.systemService) return;
      deps.systemService
        .getVersion()
        .then((res) => {
          state.version = (res && res.version) || '0.0.0';
          if (state.$root) render();
        })
        .catch((err) => {
          console.warn('Sidebar: falha ao obter versão', err);
        });
    },
    setCurrent(id) {
      state.current = id;
      // Auto-open parent group if current is a child.
      NAV.forEach((item) => {
        if (item.children && item.children.some((c) => c.id === id)) {
          state.groups[item.id] = true;
        }
      });
      persist();
      if (state.$root) render();
    },
    // Re-render to reflect a changed display name on the avatar (after a profile save).
    refreshUser() {
      if (state.$root) render();
    },
    // Re-read collapse/group state from preferences (after login reconciliation) and re-render.
    refreshState() {
      loadState();
      if (state.$root) render();
    },
    // Pulls the active accounting-closing period. Not called from mount() — an anonymous GET
    // before login would 401; composition-root calls this from hydrateSelf() instead.
    refreshClosing() {
      if (!closingProvider) return;
      closingProvider
        .get()
        .then((res) => {
          state.closing = (res && res.period) || null;
          if (state.$root) render();
        })
        .catch((err) => {
          console.warn('Sidebar: falha ao obter fechamento', err);
        });
    },
    configureClosing(provider) {
      closingProvider = provider;
    },
  };
}
