/* pages.js — barrel loader. Page modules.
 * Files keep their IIFE + window.Pages.* globals. */
(function () {
  const base = 'pages/';
  const files = [];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
