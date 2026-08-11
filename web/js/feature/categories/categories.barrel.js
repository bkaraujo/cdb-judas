/* feature/categories/categories.barrel.js — sequencia application → secondary → primary da
 * própria fatia. Não é chamado por ninguém de fora. Sem _0_domain própria: Domain.Category é
 * kernel (ver kernel.barrel.js) — os widgets genéricos de picker dependem dele. */
(function () {
  const base = 'js/feature/categories/';
  const files = [
    '_1_application/category-service.js',
    '_2_infrastructure/secondary/category-repository.js',
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
