/* core/boot.js — único entrypoint do frontend, único <script> referenciado por index.html.
 * Ordem: kernel (core/kernel) → fatias (web/feature/<slice>.js, um arquivo por fatia, sem
 * barrel — cada bloco interno já é um IIFE independente) → composition-root (roda por último,
 * faz o wiring de DI). Espelha FeatureBootstrap.onStart() do backend: initialize(f000); fatias
 * em ordem; initialize(f999). Migração fatia-por-fatia concluída — sem bloco legado.
 *
 * Injeção em UMA lista achatada só (kernel + fatias), nunca kernel.barrel.js injetado como
 * <script> próprio: um <script src> dinâmico com async=false só garante ordem de execução
 * relativa à ordem de INSERÇÃO no documento — e essa inserção acontece de forma síncrona, no
 * mesmo turno de JS. Se kernel.barrel.js fosse injetado aqui e só ao carregar (assíncrono, i.e.
 * depois deste laço já ter terminado) injetasse os arquivos reais do kernel, esses arquivos
 * entrariam na fila de execução DEPOIS de toda fatia — quebrando qualquer fatia que use uma
 * primitiva do kernel no nível do módulo (ex.: `window.Pages['tags'] = window.cachePage({...})`
 * em core/kernel/.../page.js). Mesmo padrão já usado (correto) em test/index.html — não duplicar
 * a lógica de achatamento lá, mas manter os dois em sincronia via CDB_MANIFEST. */
(function () {
  function inject(src) {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    document.head.appendChild(s);
  }

  const k = window.CDB_MANIFEST.kernel;
  const kernelFiles = []
    .concat(k.domain.map(function (p) { return 'core/kernel/_0_domain/' + p; }))
    .concat(k.application.map(function (p) { return 'core/kernel/_1_application/' + p; }))
    .concat(k.secondary.map(function (p) { return 'core/kernel/_2_infrastructure/secondary/' + p; }))
    .concat(k.primary.map(function (p) { return 'core/kernel/_2_infrastructure/primary/' + p; }));

  kernelFiles.concat(window.CDB_MANIFEST.feature).forEach(inject);

  inject('core/composition-root/composition-root.barrel.js');
})();
