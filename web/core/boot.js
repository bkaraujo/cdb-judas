/* core/boot.js — único entrypoint do frontend, único <script> referenciado por index.html.
 * Ordem: kernel (core/kernel) → fatias migradas (web/feature/<slice>.js, um arquivo por fatia,
 * sem barrel — cada bloco interno já é um IIFE independente) → legado (core/_1_domain.js etc.,
 * encolhe a cada fatia migrada) → composition-root (roda por último, faz o wiring de DI).
 * Espelha FeatureBootstrap.onStart() do backend: initialize(f000); fatias em ordem; initialize(f999).
 * Fatias entram nesta lista uma a uma, junto da fatia sendo migrada — nunca antes do próprio
 * arquivo existir (evita 404 no console). */
(function () {
  function inject(src) {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    document.head.appendChild(s);
  }

  inject('core/kernel/kernel.barrel.js');

  [
    'feature/reports.js',
    'feature/cost-centers.js',
    'feature/tags.js',
    'feature/categories.js',
    'feature/settings.js',
    'feature/import-rules.js',
    'feature/accounts.js'
  ].forEach(inject);

  // ── LEGADO: barrels por camada, encolhem a cada fatia migrada (ver .claude/frontend-refactor.md) ──
  inject('core/_1_domain.js');
  inject('core/_3_infrastructure.js');
  inject('core/_2_application.js');
  inject('core/pages.js');

  inject('core/composition-root/composition-root.barrel.js');
})();
