/* app.js — Router, header, tema, bootstrap (HTML5 + jQuery) */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui;
  var TITLES = SAS.data.SCREEN_TITLES;

  var state = {
    screen: localStorage.getItem('sas-screen') || 'dashboard',
    theme: localStorage.getItem('sas-theme') || 'dark'
  };

  // ── Tema ────────────────────────────────────────────────────
  function applyTheme() {
    $('html').attr('data-theme', state.theme);
    localStorage.setItem('sas-theme', state.theme);
  }
  function toggleTheme() {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    applyTheme();
    renderHeader();
  }

  // ── Header ──────────────────────────────────────────────────
  function renderHeader() {
    var h = '';
    h += '<div class="header-left">';
    h += '<button class="icon-btn" data-act="menu">' + ui.icon('menu', { size: 18 }) + '</button>';
    h += '<span class="header-title">' + (TITLES[state.screen] || 'SAS Finance') + '</span>';
    h += '</div>';
    h += '<div class="header-right">';
    h += '<div style="position:relative">' +
      '<div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted);pointer-events:none">' + ui.icon('search', { size: 15 }) + '</div>' +
      '<input placeholder="Buscar..." style="padding-left:32px;width:200px;height:32px;font-size:13px">' +
      '</div>';
    h += '<button class="icon-btn" style="position:relative">' + ui.icon('bell', { size: 17 }) +
      '<span style="position:absolute;top:6px;right:6px;width:7px;height:7px;border-radius:50%;background:var(--expense);border:1.5px solid var(--bg-surface)"></span></button>';
    h += '<button class="icon-btn" data-act="theme">' + ui.icon(state.theme === 'dark' ? 'sun' : 'moon', { size: 17 }) + '</button>';
    h += '<div style="width:32px;height:32px;border-radius:50%;background:var(--accent);display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:800;color:#fff;cursor:pointer;flex-shrink:0">S</div>';
    h += '</div>';
    $('#header').html(h);
  }

  // ── Router ──────────────────────────────────────────────────
  function renderScreen() {
    var screen = SAS.screens[state.screen];
    var $page = $('#page');
    $page.scrollTop(0);
    if (screen && typeof screen.render === 'function') {
      $page.html('<div class="fade-in">' + screen.render() + '</div>');
      if (typeof screen.mount === 'function') screen.mount($page);
    } else {
      $page.html('<div class="fade-in">' + ui.emptyState({ icon: 'activity', title: 'Em construção', desc: 'Esta tela será implementada em breve.' }) + '</div>');
    }
  }

  function navigate(id) {
    state.screen = id;
    localStorage.setItem('sas-screen', id);
    renderHeader();
    SAS.sidebar.render();
    renderScreen();
  }

  // ── Bootstrap ───────────────────────────────────────────────
  function init() {
    applyTheme();
    SAS.sidebar.render();
    SAS.sidebar.bind();
    renderHeader();
    renderScreen();

    $('#header').on('click', '[data-act="theme"]', function () { toggleTheme(); SAS.sidebar.render(); });
    $('#header').on('click', '[data-act="menu"]', function () { SAS.sidebar.render(); });
  }

  SAS.app = {
    init: init, navigate: navigate, toggleTheme: toggleTheme,
    current: function () { return state.screen; },
    theme: function () { return state.theme; },
    rerender: renderScreen
  };

  $(function () { SAS.app.init(); });
})(window, jQuery);
