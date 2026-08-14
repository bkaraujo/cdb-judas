/* _2_infrastructure/primary/page.js — fábrica de ciclo de vida de página (plan.md Fase 3).
 *
 * 13 páginas repetiam o mesmo mount/unmount à mão, e o bug latente era sempre o mesmo: esquecer
 * `.off(ns)` (listener DOM vazado entre navegações) ou o `unsubscribe` do CacheStore (callback
 * vazado). `window.page`/`window.cachePage` centralizam essa sequência.
 *
 * Cada fatia mantém sua própria variável `state` de módulo (closure) como sempre teve — a fábrica
 * não a substitui, só orquestra: chama `cfg.state(...)` (a função `resetState` já existente na
 * fatia, agora retornando o objeto que atribui a `state`) e guarda o MESMO objeto por referência,
 * pra poder setar `.$root`/`.unsubscribe` nele sem precisar enxergar a variável de módulo da
 * fatia. `render`/`bind` continuam sendo passadas por referência, fechadas sobre a `state` da
 * própria fatia — nenhuma delas muda de assinatura.
 */
(function () {
  /* cfg: { ns, state(...args)->obj, render(), bind($root)?, onMount(state)?, onUnmount(state)? }
   * `state` é a função resetState() já existente na fatia — precisa terminar com `return state;`.
   * Argumentos extras passados a `.mount($root, ...)` (ex. cardId de rota) são repassados a
   * `cfg.state`. */
  function page(cfg) {
    let current = null;
    return {
      mount: function ($root) {
        const extra = Array.prototype.slice.call(arguments, 1);
        current = cfg.state.apply(null, extra);
        current.$root = $root;
        if (cfg.bind) cfg.bind($root);
        cfg.render();
        if (cfg.onMount) cfg.onMount(current);
      },
      unmount: function () {
        if (current && current.$root) current.$root.off(cfg.ns);
        if (cfg.onUnmount) cfg.onUnmount(current);
        current = null;
      },
    };
  }

  /* cachePage: por cima de page(), sincroniza do App.CacheStore no mount e reassina no evento.
   * cfg adiciona: { collection?, event, sync(state)?, onChange(cb)->unsubscribe? }
   *  - `collection` (nome no CacheStore, ex. 'tags'): default de sync é
   *    `state[collection] = window.App.CacheStore[collection]().slice()`.
   *  - `sync(state)` — sobrescreve o default quando a sincronização não é uma cópia direta
   *    (ex. credit-cards agrupa cartões por conta).
   *  - `event` — tipo passado a `App.CacheStore.subscribe(event, cb)`. Default de `onChange`.
   *  - `onChange(cb)` — sobrescreve a fonte do subscribe (ex. `AccountsApi.onChange`, que já é o
   *    canal público cross-slice) quando não é direto no CacheStore.
   */
  function cachePage(cfg) {
    const sync = cfg.sync || function (state) {
      state[cfg.collection] = window.App.CacheStore[cfg.collection]().slice();
    };
    const subscribeFn = cfg.onChange || function (cb) {
      return window.App.CacheStore.subscribe(cfg.event, cb);
    };
    let current = null;
    return page({
      ns: cfg.ns,
      state: function () {
        current = cfg.state.apply(null, arguments);
        return current;
      },
      bind: cfg.bind,
      // Sync antes do primeiro paint (evita um render vazio seguido do real) — mesma ordem
      // sync→render que cada fatia já fazia à mão.
      render: function () { sync(current); cfg.render(); },
      onMount: function (state) {
        state.unsubscribe = subscribeFn(function () {
          sync(state);
          cfg.render();
        });
        if (cfg.onMount) cfg.onMount(state);
      },
      onUnmount: function (state) {
        if (state && state.unsubscribe) state.unsubscribe();
        if (cfg.onUnmount) cfg.onUnmount(state);
      },
    });
  }

  window.page = page;
  window.cachePage = cachePage;
})();
