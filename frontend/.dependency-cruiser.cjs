module.exports = {
  forbidden: [
    {
      name: 'no-cross-slice',
      comment: 'Uma fatia só alcança outra pelo api.ts dela (espelha FNNNApi do backend).',
      severity: 'error',
      from: { path: '^src/feature/([^/]+)/' },
      to: { path: '^src/feature/(?!$1/)[^/]+/(?!api\\.ts$)' },
    },
    {
      name: 'no-domain-to-infra',
      comment: '_0_domain/_1_application não acessam _2_infrastructure em runtime. Tipo-só é '
        + 'exceção deliberada: as portas (Storage/AuthStore/HttpClient/SelfRepository/SseClient) '
        + 'nascem dentro dos arquivos _2_infrastructure/secondary (Fase 2 do plano) e '
        + '_1_application as consome via DI (`createXService(port: PortType)`) — só a FORMA é '
        + 'referenciada, nunca a implementação; o objeto real chega de fora, via composition-root.',
      severity: 'error',
      from: { path: '^src/core/kernel/_(0_domain|1_application)/' },
      to: { path: '^src/core/kernel/_2_infrastructure/', dependencyTypesNot: ['type-only'] },
      // money.ts é facade deliberada sobre format.ts (documentado no topo do próprio arquivo,
      // e já era exceção nomeada em web/tools/check-slices.js:32-35).
      // A exceção some se format.ts for classificado como _0_domain — ver Fase 3.
    },
    {
      name: 'composition-root-only-from-main',
      severity: 'error',
      from: { pathNot: '^src/(main\\.ts|core/composition-root/)' },
      to: { path: '^src/core/composition-root/' },
    },
    { name: 'no-circular', severity: 'error', from: {}, to: { circular: true } },
    { name: 'no-orphans', severity: 'error', from: { orphan: true, pathNot: '\\.d\\.ts$' }, to: {} },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsConfig: { fileName: 'tsconfig.json' },
    exclude: { path: '\\.test\\.ts$' },
    // Sem isto, `import type` (obrigatório com verbatimModuleSyntax) fica invisível ao grafo —
    // no-orphans/no-cross-slice/no-domain-to-infra passariam cegos pelo acoplamento via tipos.
    tsPreCompilationDeps: true,
  },
};
