// main.jsx — App root, header, routing
const { useState: useAppState } = React;

const SCREEN_TITLES = {
  'dashboard':     'Visão Geral',
  'lancamentos':   'Lançamentos',
  'contas-pagar':  'A Pagar e Receber',
  'extrato':       'Extrato de Contas',
  'cartoes':       'Cartões de Crédito',
  'orcamento':     'Metas / Orçamento',
  'relatorios':    'Relatórios',
  'categorias':    'Categorias',
  'centros-custo': 'Centros de Custo',
  'contas':        'Contas Bancárias',
  'tags':          'Tags',
};

function Header({ screen, onMenuToggle }) {
  const { theme, toggle } = useTheme();
  return React.createElement('header', { className: 'header' },
    React.createElement('div', { className: 'header-left' },
      React.createElement('button', { className: 'icon-btn', onClick: onMenuToggle },
        React.createElement(Icon, { name: 'menu', size: 18 })
      ),
      React.createElement('span', { className: 'header-title' }, SCREEN_TITLES[screen] || 'CBD Finance')
    ),
    React.createElement('div', { className: 'header-right' },
      React.createElement('div', { style: { position: 'relative' } },
        React.createElement('div', { style: { position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' } },
          React.createElement(Icon, { name: 'search', size: 15 })
        ),
        React.createElement('input', {
          placeholder: 'Buscar...',
          style: { paddingLeft: 32, width: 200, height: 32, fontSize: 13, borderRadius: 'var(--radius-sm)' }
        })
      ),
      React.createElement('button', { className: 'icon-btn', style: { position: 'relative' } },
        React.createElement(Icon, { name: 'bell', size: 17 }),
        React.createElement('span', {
          style: {
            position: 'absolute', top: 6, right: 6, width: 7, height: 7,
            borderRadius: '50%', background: 'var(--expense)',
            border: '1.5px solid var(--bg-surface)',
          }
        })
      ),
      React.createElement('button', {
        className: 'icon-btn', onClick: toggle, title: theme === 'dark' ? 'Modo claro' : 'Modo escuro'
      },
        React.createElement(Icon, { name: theme === 'dark' ? 'sun' : 'moon', size: 17 })
      ),
      React.createElement('div', {
        style: {
          width: 32, height: 32, borderRadius: '50%',
          background: 'var(--accent)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 13, fontWeight: 800, color: '#fff', cursor: 'pointer',
          flexShrink: 0,
        }
      }, 'C')
    )
  );
}

function ScreenRouter({ screen }) {
  switch (screen) {
    case 'dashboard':    return React.createElement(DashboardScreen);
    case 'lancamentos':  return React.createElement(LancamentosScreen);
    case 'contas-pagar': return React.createElement(ContasPagarScreen);
    case 'extrato':      return React.createElement(ExtratoScreen);
    case 'cartoes':      return React.createElement(CartoesScreen);
    case 'orcamento':    return React.createElement(OrcamentoScreen);
    case 'relatorios':   return React.createElement(RelatoriosScreen);
    case 'categorias':   return React.createElement(CategoriasScreen);
    case 'centros-custo':return React.createElement(CentroCustoScreen);
    case 'contas':       return React.createElement(ContasScreen);
    case 'tags':         return React.createElement(TagsScreen);
    default:
      return React.createElement('div', { className: 'fade-in', style: { color: 'var(--text-muted)', padding: '40px', textAlign: 'center' } },
        React.createElement(EmptyState, { icon: 'activity', title: 'Em construção', desc: 'Esta tela será implementada em breve.' })
      );
  }
}

function App() {
  const [screen, setScreen] = useAppState(
    () => localStorage.getItem('cbd-screen') || 'dashboard'
  );

  const nav = (id) => {
    setScreen(id);
    localStorage.setItem('cbd-screen', id);
  };

  return React.createElement(ThemeProvider, null,
    React.createElement('div', { className: 'app-layout' },
      React.createElement(Sidebar, { current: screen, onNav: nav }),
      React.createElement('div', { className: 'main-area' },
        React.createElement(Header, { screen, onMenuToggle: () => {} }),
        React.createElement('main', { className: 'page-content' },
          React.createElement(ScreenRouter, { screen })
        )
      )
    )
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(React.createElement(App));
