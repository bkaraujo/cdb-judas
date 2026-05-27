// sidebar.jsx — Collapsible sidebar navigation
const { useState: useSidebarState } = React;

const NAV = [
  { id: 'dashboard', label: 'Visão Geral', icon: 'home' },
  {
    id: 'movimentacoes', label: 'Movimentações', icon: 'layers',
    children: [
      { id: 'lancamentos', label: 'Lançamentos', icon: 'list' },
      { id: 'contas-pagar', label: 'A pagar e receber', icon: 'calendar' },
    ]
  },
  { id: 'extrato', label: 'Extrato de Contas', icon: 'bookOpen' },
  { id: 'cartoes', label: 'Cartões de Crédito', icon: 'creditCard' },
  { id: 'orcamento', label: 'Metas / Orçamento', icon: 'target' },
  { id: 'relatorios', label: 'Relatórios', icon: 'barChart' },
  {
    id: 'cadastros', label: 'Cadastros', icon: 'database',
    children: [
      { id: 'categorias', label: 'Categorias', icon: 'tag' },
      { id: 'centros-custo', label: 'Centros de Custo', icon: 'briefcase' },
      { id: 'contas', label: 'Contas', icon: 'building' },
      { id: 'tags', label: 'Tags', icon: 'hash' },
    ]
  },
];

window.Sidebar = function Sidebar({ current, onNav }) {
  const [collapsed, setCollapsed] = useSidebarState(false);
  const [openGroups, setOpenGroups] = useSidebarState({ movimentacoes: true, cadastros: false });
  const { theme, toggle } = useTheme();

  const toggleGroup = (id) =>
    setOpenGroups(prev => ({ ...prev, [id]: !prev[id] }));

  const isActive = (item) => {
    if (item.id === current) return true;
    if (item.children) return item.children.some(c => c.id === current);
    return false;
  };

  return React.createElement('aside', {
    style: {
      width: collapsed ? 60 : 220,
      background: 'var(--sidebar-bg)',
      display: 'flex', flexDirection: 'column',
      borderRight: '1px solid var(--border)',
      transition: 'width 0.22s cubic-bezier(.4,0,.2,1)',
      flexShrink: 0, overflow: 'hidden',
      color: 'var(--text-primary)',
    }
  },
    // Logo + collapse btn
    React.createElement('div', {
      style: {
        height: 56, display: 'flex', alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'space-between',
        padding: collapsed ? '0' : '0 14px 0 18px',
        borderBottom: '1px solid var(--border)', flexShrink: 0,
      }
    },
      !collapsed && React.createElement('div', {
        style: { display: 'flex', alignItems: 'center', gap: 8 }
      },
        React.createElement('div', {
          style: {
            width: 28, height: 28, borderRadius: 8,
            background: 'var(--accent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }
        }, React.createElement('span', { style: { color: '#fff', fontWeight: 800, fontSize: 13 } }, 'C')),
        React.createElement('span', {
          style: { fontWeight: 800, fontSize: 15, color: 'var(--text-primary)', letterSpacing: '-0.02em' }
        }, 'CBD Finance')
      ),
      collapsed && React.createElement('div', {
        style: {
          width: 28, height: 28, borderRadius: 8, background: 'var(--accent)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }
      }, React.createElement('span', { style: { color: '#fff', fontWeight: 800, fontSize: 13 } }, 'C')),
      !collapsed && React.createElement('button', {
        className: 'icon-btn', onClick: () => setCollapsed(true),
        style: { color: 'var(--text-muted)' }
      }, React.createElement(Icon, { name: 'chevronLeft', size: 16 }))
    ),

    // Nav items
    React.createElement('nav', {
      style: { flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '10px 0' }
    },
      NAV.map(item => {
        const active = isActive(item);
        const hasChildren = !!item.children;
        const open = openGroups[item.id];

        const itemEl = React.createElement('div', {
          key: item.id,
          className: 'tooltip-wrap',
          style: { padding: '1px 8px' }
        },
          React.createElement('button', {
            onClick: () => {
              if (hasChildren) {
                if (collapsed) { setCollapsed(false); setOpenGroups(prev => ({ ...prev, [item.id]: true })); }
                else toggleGroup(item.id);
              } else {
                onNav(item.id);
                if (collapsed) setCollapsed(false);
              }
            },
            style: {
              display: 'flex', alignItems: 'center', gap: 10,
              width: '100%', padding: '8px 10px',
              borderRadius: 'var(--radius-sm)',
              background: active && !hasChildren ? 'var(--accent-light)' : 'transparent',
              color: active && !hasChildren ? 'var(--accent)' : active ? 'var(--text-primary)' : 'var(--text-secondary)',
              fontWeight: active ? 600 : 400,
              fontSize: 13, justifyContent: collapsed ? 'center' : 'flex-start',
              transition: 'all var(--transition)',
              border: 'none', cursor: 'pointer', whiteSpace: 'nowrap',
            },
            onMouseEnter: e => { if (!active) e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; },
            onMouseLeave: e => {
              e.currentTarget.style.background = active && !hasChildren ? 'var(--accent-light)' : 'transparent';
              e.currentTarget.style.color = active && !hasChildren ? 'var(--accent)' : active ? 'var(--text-primary)' : 'var(--text-secondary)';
            }
          },
            React.createElement(Icon, { name: item.icon, size: 17 }),
            !collapsed && React.createElement(React.Fragment, null,
              React.createElement('span', { style: { flex: 1, textAlign: 'left' } }, item.label),
              hasChildren && React.createElement(Icon, { name: open ? 'chevronUp' : 'chevronDown', size: 13 })
            )
          ),
          collapsed && React.createElement('span', { className: 'tooltip' }, item.label)
        );

        if (!hasChildren) return itemEl;

        return React.createElement(React.Fragment, { key: item.id },
          itemEl,
          !collapsed && open && React.createElement('div', {
            style: { padding: '2px 8px 4px 8px' }
          },
            item.children.map(child =>
              React.createElement('div', { key: child.id, className: 'tooltip-wrap' },
                React.createElement('button', {
                  onClick: () => onNav(child.id),
                  style: {
                    display: 'flex', alignItems: 'center', gap: 8,
                    width: '100%', padding: '7px 10px 7px 28px',
                    borderRadius: 'var(--radius-sm)', fontSize: 13,
                    background: current === child.id ? 'var(--accent-light)' : 'transparent',
                    color: current === child.id ? 'var(--accent)' : 'var(--text-secondary)',
                    fontWeight: current === child.id ? 600 : 400,
                    border: 'none', cursor: 'pointer', whiteSpace: 'nowrap',
                    transition: 'all var(--transition)',
                  },
                  onMouseEnter: e => { if (current !== child.id) { e.currentTarget.style.background = 'var(--bg-hover)'; e.currentTarget.style.color = 'var(--text-primary)'; } },
                  onMouseLeave: e => {
                    e.currentTarget.style.background = current === child.id ? 'var(--accent-light)' : 'transparent';
                    e.currentTarget.style.color = current === child.id ? 'var(--accent)' : 'var(--text-secondary)';
                  }
                },
                  React.createElement(Icon, { name: child.icon, size: 14 }),
                  React.createElement('span', null, child.label)
                )
              )
            )
          )
        );
      })
    ),

    // Bottom: theme toggle + collapse btn when expanded
    React.createElement('div', {
      style: {
        borderTop: '1px solid var(--border)', padding: '10px 8px',
        display: 'flex', flexDirection: 'column', gap: 4,
      }
    },
      React.createElement('div', { className: 'tooltip-wrap' },
        React.createElement('button', {
          className: 'icon-btn', onClick: toggle,
          style: { width: '100%', justifyContent: 'center', gap: 8, height: 36, display: 'flex', alignItems: 'center' }
        },
          React.createElement(Icon, { name: theme === 'dark' ? 'sun' : 'moon', size: 16 }),
          !collapsed && React.createElement('span', { style: { fontSize: 13, fontWeight: 500 } }, theme === 'dark' ? 'Modo Claro' : 'Modo Escuro')
        ),
        collapsed && React.createElement('span', { className: 'tooltip' }, theme === 'dark' ? 'Modo Claro' : 'Modo Escuro')
      ),
      collapsed && React.createElement('div', { className: 'tooltip-wrap' },
        React.createElement('button', {
          className: 'icon-btn',
          onClick: () => setCollapsed(false),
          style: { width: '100%', justifyContent: 'center', height: 36, display: 'flex', alignItems: 'center' }
        },
          React.createElement(Icon, { name: 'chevronRight', size: 16 })
        ),
        React.createElement('span', { className: 'tooltip' }, 'Expandir menu')
      )
    )
  );
};
