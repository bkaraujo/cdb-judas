/* core/boot.js — único entrypoint do frontend, único <script> referenciado por index.html.
 * Ordem: kernel (core/kernel) → fatias (web/feature/<slice>.js, um arquivo por fatia, sem
 * barrel — cada bloco interno já é um IIFE independente) → composition-root (roda por último,
 * faz o wiring de DI). Espelha FeatureBootstrap.onStart() do backend: initialize(f000); fatias
 * em ordem; initialize(f999). Migração fatia-por-fatia concluída — sem bloco legado. */
(function () {
  function inject(src) {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    document.head.appendChild(s);
  }

  inject('core/kernel/kernel.barrel.js');

  window.CDB_MANIFEST.feature.forEach(inject);

  inject('core/composition-root/composition-root.barrel.js');
})();
