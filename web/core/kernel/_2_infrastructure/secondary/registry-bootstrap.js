/* _3_infrastructure/secondary/registry-bootstrap.js — initial hydration of CBD cache. */
(function () {
  function create(deps) {
    const repos = deps.repos;
    return {
      hydrate: function () {
        return Promise.all([
          repos.categories.list(),
          repos.accounts.list(),
          repos.tags.list(),
          repos.costCenters.list(),
          repos.importRules.list(),
        ]).then(function (arr) {
          window.CBD = window.CBD || {};
          window.CBD.categories  = Array.isArray(arr[0]) ? arr[0] : [];
          window.CBD.accounts    = Array.isArray(arr[1]) ? arr[1] : [];
          window.CBD.tags        = Array.isArray(arr[2]) ? arr[2] : [];
          window.CBD.costCenters = Array.isArray(arr[3]) ? arr[3] : [];
          window.CBD.importRules = Array.isArray(arr[4]) ? arr[4] : [];
          return true;
        });
      },
    };
  }

  window.Infra = window.Infra || {};
  window.Infra.RegistryBootstrap = { create: create };
})();
