/* feature/reports/reports.barrel.js — "F0NNModule" do frontend para a fatia reports (stub).
 * Sequencia só os arquivos da própria fatia. */
(function () {
  const base = 'js/feature/reports/';
  const files = [
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
