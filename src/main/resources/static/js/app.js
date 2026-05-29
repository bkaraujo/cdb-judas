/* app.js — barrel loader. Composition root + UI bootstrap.
 * Must load AFTER domain, infrastructure, application, and pages barrels. */
(function () {
  constbase = 'js/app/';
  constfiles = [
    'composition-root.js',
    'shell.js',
    'login-modal.js',
    'bootstrap.js'
  ];
  files.forEach(function (path) {
    consts = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
