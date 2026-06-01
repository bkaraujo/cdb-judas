/* sidebar.js — Sidebar recolhível com grupos e tooltips (HTML5 + jQuery) */
(function (global, $) {
  'use strict';
  var SAS = global.SAS, ui = SAS.ui, NAV = SAS.data.NAV;

  var state = {
    collapsed: false,
    openGroups: { movimentacoes: true, cadastros: false }
  };

  function isActive(item, current) {
    if (item.id === current) return true;
    if (item.children) return item.children.some(function (c) { return c.id === current; });
    return false;
  }

  function render() {
    var current = SAS.app.current();
    var theme = SAS.app.theme();
    var $sb = $('#sidebar');
    $sb.toggleClass('collapsed', state.collapsed);

    var h = '';
    // Head
    h += '<div class="sidebar-head">';
    if (state.collapsed) {
      h += '<div class="logo-mark">S</div>';
    } else {
      h += '<div class="flex items-center gap-8"><div class="logo-mark">S</div><span class="logo-text">SAS Finance</span></div>';
      h += '<button class="icon-btn" data-act="collapse" style="color:var(--text-muted)">' + ui.icon('chevronLeft', { size: 16 }) + '</button>';
    }
    h += '</div>';

    // Nav
    h += '<nav class="sidebar-nav">';
    NAV.forEach(function (item) {
      var active = isActive(item, current);
      var hasChildren = !!item.children;
      var open = state.openGroups[item.id];
      var cls = 'nav-item' + (active && !hasChildren ? ' active' : (active ? ' group-open' : ''));

      h += '<div class="nav-item-wrap tooltip-wrap">';
      h += '<button class="' + cls + '" data-nav="' + item.id + '" data-group="' + (hasChildren ? '1' : '0') + '">';
      h += ui.icon(item.icon, { size: 17 });
      if (!state.collapsed) {
        h += '<span class="nav-label">' + item.label + '</span>';
        if (hasChildren) h += ui.icon(open ? 'chevronUp' : 'chevronDown', { size: 13 });
      }
      h += '</button>';
      if (state.collapsed) h += '<span class="tooltip">' + item.label + '</span>';
      h += '</div>';

      if (hasChildren && !state.collapsed && open) {
        h += '<div class="nav-sub">';
        item.children.forEach(function (child) {
          var ca = current === child.id ? ' active' : '';
          h += '<button class="nav-subitem' + ca + '" data-nav="' + child.id + '">' +
            ui.icon(child.icon, { size: 14 }) + '<span>' + child.label + '</span></button>';
        });
        h += '</div>';
      }
    });
    h += '</nav>';

    // Foot
    h += '<div class="sidebar-foot">';
    h += '<div class="tooltip-wrap"><button class="icon-btn" data-act="theme" style="justify-content:center">' +
      ui.icon(theme === 'dark' ? 'sun' : 'moon', { size: 16 }) +
      (!state.collapsed ? '<span style="font-size:13px;font-weight:500">' + (theme === 'dark' ? 'Modo Claro' : 'Modo Escuro') + '</span>' : '') +
      '</button>';
    if (state.collapsed) h += '<span class="tooltip">' + (theme === 'dark' ? 'Modo Claro' : 'Modo Escuro') + '</span>';
    h += '</div>';
    if (state.collapsed) {
      h += '<div class="tooltip-wrap"><button class="icon-btn" data-act="expand" style="justify-content:center">' +
        ui.icon('chevronRight', { size: 16 }) + '</button><span class="tooltip">Expandir menu</span></div>';
    }
    h += '</div>';

    $sb.html(h);
  }

  function bind() {
    $('#sidebar').on('click', '[data-nav]', function () {
      var id = $(this).data('nav');
      var isGroup = String($(this).data('group')) === '1';
      if (isGroup) {
        if (state.collapsed) { state.collapsed = false; state.openGroups[id] = true; }
        else state.openGroups[id] = !state.openGroups[id];
        render();
      } else {
        if (state.collapsed) state.collapsed = false;
        SAS.app.navigate(id);
      }
    });
    $('#sidebar').on('click', '[data-act]', function () {
      var act = $(this).data('act');
      if (act === 'collapse') { state.collapsed = true; render(); }
      else if (act === 'expand') { state.collapsed = false; render(); }
      else if (act === 'theme') { SAS.app.toggleTheme(); render(); }
    });
  }

  SAS.sidebar = { render: render, bind: bind };
})(window, jQuery);
