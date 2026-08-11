/* feature/tags/tags.barrel.js — sequencia application → secondary → primary da própria fatia.
 * Não é chamado por ninguém de fora. Sem _0_domain própria: Domain.Tag é kernel (ver
 * kernel.barrel.js) — os widgets genéricos de picker dependem dele. */
(function () {
  const base = 'js/feature/tags/';
  const files = [
    '_1_application/tag-service.js',
    '_2_infrastructure/secondary/tag-repository.js',
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
