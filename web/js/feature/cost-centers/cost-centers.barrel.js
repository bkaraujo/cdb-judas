/* feature/cost-centers/cost-centers.barrel.js — sequencia domain → application → secondary → primary
 * da própria fatia. Não é chamado por ninguém de fora. */
(function () {
  const base = 'js/feature/cost-centers/';
  const files = [
    '_0_domain/cost-center.js',
    '_1_application/cost-center-service.js',
    '_2_infrastructure/secondary/cost-center-repository.js',
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
