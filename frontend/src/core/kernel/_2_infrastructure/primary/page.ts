/**
 * Fábrica de ciclo de vida de página.
 *
 * 13 páginas repetiam o mesmo mount/unmount à mão, e o bug latente era sempre o mesmo: esquecer
 * `.off(ns)` (listener DOM vazado entre navegações) ou o `unsubscribe` do CacheStore (callback
 * vazado). `createPage`/`cachePage` centralizam essa sequência.
 *
 * Cada fatia mantém sua própria `state` (closure) como sempre teve — a fábrica não a substitui,
 * só orquestra: chama `cfg.state(param)` e guarda o MESMO objeto por referência, pra poder setar
 * `.$root`/`.unsubscribe` nele sem precisar enxergar a variável de módulo da fatia.
 *
 * `arguments`+`.apply` do `page.js` original (repasse de args variádicos de rota) vira um único
 * `param?: string | null` opcional — nenhuma tela usa mais de um parâmetro de rota hoje (só
 * `card-statement`, o id do cartão; ver `router.ts`), então rest params genéricos seriam
 * capacidade sem uso real.
 */
export interface Page {
  mount(root: JQuery, param?: string | null): void;
  unmount(): void;
}

export interface PageState {
  $root?: JQuery;
  unsubscribe?: () => void;
}

export interface PageConfig<TState extends PageState> {
  ns: string;
  state: (param?: string | null) => TState;
  render: () => void;
  bind?: (root: JQuery) => void;
  onMount?: (state: TState) => void;
  onUnmount?: (state: TState) => void;
}

export function createPage<TState extends PageState>(cfg: PageConfig<TState>): Page {
  let current: TState | null = null;
  return {
    mount(root, param) {
      current = cfg.state(param);
      current.$root = root;
      if (cfg.bind) cfg.bind(root);
      cfg.render();
      if (cfg.onMount) cfg.onMount(current);
    },
    unmount() {
      if (current && current.$root) current.$root.off(cfg.ns);
      if (cfg.onUnmount) cfg.onUnmount(current as TState);
      current = null;
    },
  };
}

export interface CachePageConfig<TState extends PageState> extends PageConfig<TState> {
  /** Sincroniza `state` a partir do cache antes do primeiro render e a cada evento de cadastro
   * aceito. Substitui `state[cfg.collection] = CacheStore[cfg.collection]()` (indexação por
   * string) do `page.js` original — o acessor tipado já vem fechado sobre o `CacheStore` no ponto
   * de chamada (fatia, Fase 6), sem indexação dinâmica. */
  sync: (state: TState) => void;
  /** Assina o evento de cadastro relevante; retorna a função de unsubscribe. Tipicamente
   * `(cb) => cacheStore.subscribe('TAG', cb)`. */
  subscribe: (cb: () => void) => () => void;
}

/** Por cima de `createPage`: sincroniza do cache no mount e reassina no evento de cadastro. */
export function cachePage<TState extends PageState>(cfg: CachePageConfig<TState>): Page {
  let current: TState | null = null;
  return createPage<TState>({
    ns: cfg.ns,
    state: (param) => {
      current = cfg.state(param);
      return current;
    },
    bind: cfg.bind,
    // Sync antes do primeiro paint (evita um render vazio seguido do real) — mesma ordem
    // sync→render que cada fatia já fazia à mão.
    render: () => {
      if (current) cfg.sync(current);
      cfg.render();
    },
    onMount: (state) => {
      state.unsubscribe = cfg.subscribe(() => {
        cfg.sync(state);
        cfg.render();
      });
      if (cfg.onMount) cfg.onMount(state);
    },
    onUnmount: (state) => {
      if (state.unsubscribe) state.unsubscribe();
      if (cfg.onUnmount) cfg.onUnmount(state);
    },
  });
}
