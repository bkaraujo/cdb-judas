/* app.js — barrel loader. Composition root + UI bootstrap.
 * Must load AFTER domain, infrastructure, application, and pages barrels. */
(function () {
  var base = 'js/app/';
  var files = [
    'composition-root.js',
    'shell.js',
    'login-modal.js',
    'bootstrap.js'
  ];
  files.forEach(function (path) {
    var s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
