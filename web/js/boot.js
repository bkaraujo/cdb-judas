/* boot.js — único entrypoint do frontend, único <script> referenciado por index.html.
 * Ordem: kernel → fatias migradas (feature/<slice>/<slice>.barrel.js) → legado (encolhe a
 * cada fatia migrada) → composition-root (roda por último, é quem faz o wiring de DI).
 * Espelha FeatureBootstrap.onStart() do backend: initialize(f000); fatias em ordem; initialize(f999).
 * Fatias entram nesta lista uma a uma, cada uma no seu próprio commit, junto da fatia sendo
 * migrada — nunca antes do próprio arquivo existir (evita 404 no console). */
(function () {
  function inject(src) {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    document.head.appendChild(s);
  }

  inject('js/kernel/kernel.barrel.js');

  [
    'js/feature/reports/reports.barrel.js',
    'js/feature/cost-centers/cost-centers.barrel.js',
    'js/feature/tags/tags.barrel.js',
    'js/feature/categories/categories.barrel.js',
    'js/feature/settings/settings.barrel.js',
    'js/feature/import-rules/import-rules.barrel.js'
  ].forEach(inject);

  // ── LEGADO: barrels por camada, encolhem a cada fatia migrada (ver .claude/frontend-refactor.md) ──
  inject('js/_1_domain.js');
  inject('js/_3_infrastructure.js');
  inject('js/_2_application.js');
  inject('js/pages.js');

  inject('js/composition-root/composition-root.barrel.js');
})();
