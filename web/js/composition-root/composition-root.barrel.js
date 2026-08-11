/* composition-root/composition-root.barrel.js — "F999Module" do frontend: composition root,
 * roda por último. Sequencia wiring de DI → shell → login modal → bootstrap. */
(function () {
  const base = 'js/composition-root/';
  const files = [
    'composition-root.js',
    'shell.js',
    'login-modal.js',
    'bootstrap.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
