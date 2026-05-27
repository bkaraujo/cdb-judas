// orcamento.jsx — Budget overview + Burn Down chart
const { useState: useOrcState, useMemo: useOrcMemo } = React;

// Budget categories for April 2026
const BUDGET_CATS = [
  { id:1, name:'Alimentação', budgeted:1200, spent:812.9,  color:'#6366F1', icon:'🛒' },
  { id:2, name:'Moradia',     budgeted:2800, spent:2920.4, color:'#F43F5E', icon:'🏠' },
  { id:3, name:'Transporte',  budgeted:500,  spent:221.9,  color:'#F59E0B', icon:'🚗' },
  { id:4, name:'Saúde',       budgeted:400,  spent:209.5,  color:'#10B981', icon:'💊' },
  { id:5, name:'Lazer',       budgeted:300,  spent:389.7,  color:'#38BDF8', icon:'🎬' },
  { id:6, name:'Educação',    budgeted:600,  spent:0,      color:'#A78BFA', icon:'📚' },
  { id:7, name:'Vestuário',   budgeted:200,  spent:0,      color:'#FB923C', icon:'👕' },
  { id:8, name:'Serviços',    budgeted:350,  spent:342.1,  color:'#34D399', icon:'⚡' },
];

// Generate burn down data for April (30 days, up to day 24)
function genBurnDown(budgeted, spent, days = 30, today = 24) {
  const ideal = Array.from({ length: days + 1 }, (_, i) => ({
    day: i, value: budgeted - (budgeted / days) * i
  }));

  // Simulate daily spending spread unevenly up to today
  const dailyRates = [0,0.06,0.02,0.10,0.03,0.00,0.08,0.04,0.00,0.07,0.05,
                      0.00,0.06,0.03,0.09,0.04,0.00,0.05,0.07,0.00,0.06,0.04,0.05,0.06,0.08];
  const totalRate = dailyRates.slice(0, today + 1).reduce((s,r) => s+r, 0);
  const factor = spent / (budgeted * totalRate || 1);
  let cum = budgeted;
  const actual = [{ day: 0, value: budgeted }];
  for (let d = 1; d <= today; d++) {
    cum -= budgeted * (dailyRates[d] || 0.04) * factor;
    actual.push({ day: d, value: Math.max(0, cum) });
  }
  return { ideal, actual };
}

function BurnDownChart({ cat }) {
  const { ideal, actual } = useOrcMemo(() => genBurnDown(cat.budgeted, cat.spent), [cat]);
  const today = actual.length - 1;

  const W = 560, H = 220;
  const pad = { l: 60, r: 20, t: 16, b: 36 };
  const cw = W - pad.l - pad.r;
  const ch = H - pad.t - pad.b;
  const days = ideal.length - 1;
  const maxV = cat.budgeted;

  const toX = d => pad.l + (d / days) * cw;
  const toY = v => pad.t + (1 - v / maxV) * ch;

  const idealPts = ideal.map(p => ({ x: toX(p.day), y: toY(p.value) }));
  const actualPts = actual.map(p => ({ x: toX(p.day), y: toY(p.value) }));

  const idealPath = smoothPath(idealPts);
  const actualPath = smoothPath(actualPts);

  // Fill area: under actual vs ideal
  const isOver = cat.spent > cat.budgeted;
  const fillColor = isOver ? 'var(--expense)' : 'var(--income)';
  const fillOpacity = 0.12;

  // Area under actual curve
  const areaActual = actualPath + ` L ${toX(today)},${pad.t + ch} L ${pad.l},${pad.t + ch} Z`;

  // Grid
  const gridVals = [0, 0.25, 0.5, 0.75, 1].map(f => ({
    y: pad.t + (1 - f) * ch,
    v: f * maxV,
  }));

  // Day labels
  const dayLabels = [0, 5, 10, 15, 20, 25, 30].filter(d => d <= days);

  const todayX = toX(today);
  const lastActualY = toY(actual[actual.length - 1].value);

  return React.createElement('svg', { viewBox: `0 0 ${W} ${H}`, style: { width: '100%', height: '100%' } },
    React.createElement('defs', null,
      React.createElement('linearGradient', { id: 'burnGrad', x1: 0, y1: 0, x2: 0, y2: 1 },
        React.createElement('stop', { offset: '0%', stopColor: fillColor, stopOpacity: fillOpacity + 0.06 }),
        React.createElement('stop', { offset: '100%', stopColor: fillColor, stopOpacity: 0.01 })
      )
    ),
    // Grid
    gridVals.map((g, i) =>
      React.createElement(React.Fragment, { key: i },
        React.createElement('line', { x1: pad.l, y1: g.y, x2: W - pad.r, y2: g.y, stroke: 'var(--border)', strokeWidth: 1 }),
        React.createElement('text', { x: pad.l - 6, y: g.y + 4, textAnchor: 'end', fontSize: 9, fill: 'var(--text-muted)' },
          fmtShort(g.v)
        )
      )
    ),
    // Day labels
    dayLabels.map(d =>
      React.createElement('text', { key: d, x: toX(d), y: H - 8, textAnchor: 'middle', fontSize: 9, fill: 'var(--text-muted)' }, d)
    ),
    // Today vertical line
    React.createElement('line', { x1: todayX, y1: pad.t, x2: todayX, y2: pad.t + ch, stroke: 'var(--text-muted)', strokeWidth: 1, strokeDasharray: '4,3' }),
    React.createElement('text', { x: todayX + 4, y: pad.t + 12, fontSize: 9, fill: 'var(--text-muted)' }, 'Hoje'),
    // Actual area fill
    React.createElement('path', { d: areaActual, fill: 'url(#burnGrad)' }),
    // Ideal line (dashed)
    React.createElement('path', { d: idealPath, fill: 'none', stroke: 'var(--text-muted)', strokeWidth: 1.5, strokeDasharray: '6,4' }),
    // Actual line
    React.createElement('path', { d: actualPath, fill: 'none', stroke: fillColor, strokeWidth: 2.5, strokeLinecap: 'round' }),
    // Current value dot
    React.createElement('circle', { cx: todayX, cy: lastActualY, r: 5, fill: fillColor, stroke: 'var(--bg-card)', strokeWidth: 2 })
  );
}

function BudgetBar({ cat }) {
  const pct = Math.min((cat.spent / cat.budgeted) * 100, 100);
  const over = cat.spent > cat.budgeted;
  const barColor = over ? 'var(--expense)' : cat.spent / cat.budgeted > 0.8 ? 'var(--warning)' : 'var(--income)';

  return React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: 6 } },
    React.createElement('div', { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' } },
      React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: 8 } },
        React.createElement('span', { style: { fontSize: 16 } }, cat.icon),
        React.createElement('span', { style: { fontSize: 13, fontWeight: 600 } }, cat.name),
        over && React.createElement(Badge, { color: 'expense' }, 'Estourado')
      ),
      React.createElement('div', { style: { textAlign: 'right', display: 'flex', gap: 12 } },
        React.createElement('span', { style: { fontSize: 12, color: 'var(--text-muted)' } }, fmt(cat.spent)),
        React.createElement('span', { style: { fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 } }, '/ ' + fmt(cat.budgeted))
      )
    ),
    React.createElement('div', {
      style: { height: 8, background: 'var(--bg-hover)', borderRadius: 4, overflow: 'hidden' }
    },
      React.createElement('div', {
        style: {
          height: '100%', borderRadius: 4, width: `${pct}%`,
          background: barColor, transition: 'width 0.6s cubic-bezier(.4,0,.2,1)',
        }
      })
    ),
    React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between' } },
      React.createElement('span', { style: { fontSize: 11, color: 'var(--text-muted)' } },
        `${pct.toFixed(0)}% utilizado`
      ),
      React.createElement('span', {
        style: { fontSize: 11, fontWeight: 600, color: over ? 'var(--expense)' : 'var(--income)' }
      }, over ? `+${fmt(cat.spent - cat.budgeted)} acima` : `${fmt(cat.budgeted - cat.spent)} restante`)
    )
  );
}

window.OrcamentoScreen = function OrcamentoScreen() {
  const [selectedCat, setSelectedCat] = useOrcState(BUDGET_CATS[0]);
  const [tab, setTab] = useOrcState('overview');

  const totalBudgeted = BUDGET_CATS.reduce((s, c) => s + c.budgeted, 0);
  const totalSpent = BUDGET_CATS.reduce((s, c) => s + c.spent, 0);
  const overBudget = BUDGET_CATS.filter(c => c.spent > c.budgeted);

  return React.createElement('div', { className: 'fade-in' },
    React.createElement('div', { className: 'page-header' },
      React.createElement('h1', null, 'Metas / Orçamento'),
      React.createElement('div', { className: 'page-header-actions' },
        React.createElement(PeriodNav, { label: 'Abril 2026', onPrev: ()=>{}, onNext: ()=>{} }),
        React.createElement(Button, { icon: 'plus' }, 'Nova Meta')
      )
    ),

    // Summary cards
    React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14, marginBottom: 20 } },
      React.createElement(Card, { style: { padding: '16px 20px' } },
        React.createElement('p', { style: { fontSize: 11, color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 } }, 'Orçamento Total'),
        React.createElement('p', { style: { fontSize: 22, fontWeight: 800 } }, fmt(totalBudgeted))
      ),
      React.createElement(Card, { style: { padding: '16px 20px' } },
        React.createElement('p', { style: { fontSize: 11, color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 } }, 'Total Gasto'),
        React.createElement('p', { style: { fontSize: 22, fontWeight: 800, color: totalSpent > totalBudgeted ? 'var(--expense)' : 'var(--text-primary)' } }, fmt(totalSpent))
      ),
      React.createElement(Card, { style: { padding: '16px 20px' } },
        React.createElement('p', { style: { fontSize: 11, color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 } }, 'Categorias Estouradas'),
        React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: 10 } },
          React.createElement('p', { style: { fontSize: 22, fontWeight: 800, color: overBudget.length > 0 ? 'var(--expense)' : 'var(--income)' } }, overBudget.length),
          overBudget.length > 0 && React.createElement('span', { style: { fontSize: 12, color: 'var(--text-muted)' } },
            overBudget.map(c => c.name).join(', ')
          )
        )
      )
    ),

    // Tabs
    React.createElement('div', { className: 'tabs' },
      React.createElement('button', { className: `tab ${tab === 'overview' ? 'active' : ''}`, onClick: () => setTab('overview') }, 'Visão Geral'),
      React.createElement('button', { className: `tab ${tab === 'burndown' ? 'active' : ''}`, onClick: () => setTab('burndown') }, 'Burn Down')
    ),

    tab === 'overview' && React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: 14 } },
      BUDGET_CATS.map(cat =>
        React.createElement(Card, {
          key: cat.id,
          style: { padding: '16px 20px', cursor: 'pointer', borderColor: selectedCat.id === cat.id ? 'var(--accent)' : 'var(--border)' },
          onClick: () => setSelectedCat(cat)
        },
          React.createElement(BudgetBar, { cat })
        )
      )
    ),

    tab === 'burndown' && React.createElement('div', null,
      // Category selector
      React.createElement('div', { style: { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 } },
        BUDGET_CATS.map(cat =>
          React.createElement('button', {
            key: cat.id,
            onClick: () => setSelectedCat(cat),
            style: {
              padding: '6px 14px', borderRadius: 'var(--radius-sm)',
              fontSize: 12, fontWeight: 600, cursor: 'pointer',
              border: `1px solid ${selectedCat.id === cat.id ? cat.color : 'var(--border)'}`,
              background: selectedCat.id === cat.id ? cat.color + '22' : 'transparent',
              color: selectedCat.id === cat.id ? cat.color : 'var(--text-secondary)',
              transition: 'all var(--transition)',
            }
          }, cat.icon + ' ' + cat.name)
        )
      ),

      React.createElement(Card, null,
        React.createElement('div', { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 } },
          React.createElement('div', null,
            React.createElement('h3', { style: { fontSize: 14, fontWeight: 700 } }, `Burn Down — ${selectedCat.name}`),
            React.createElement('p', { style: { fontSize: 12, color: 'var(--text-muted)', marginTop: 3 } }, 'Evolução do saldo orçado ao longo do mês')
          ),
          React.createElement('div', { style: { display: 'flex', gap: 16, fontSize: 12 } },
            React.createElement('span', { style: { display: 'flex', alignItems: 'center', gap: 5, color: 'var(--text-secondary)' } },
              React.createElement('span', { style: { width: 20, height: 2, background: 'var(--text-muted)', display: 'inline-block', borderStyle: 'dashed' } }),
              'Ideal'
            ),
            React.createElement('span', { style: { display: 'flex', alignItems: 'center', gap: 5, color: 'var(--text-secondary)' } },
              React.createElement('span', { style: { width: 20, height: 3, borderRadius: 2, background: selectedCat.spent > selectedCat.budgeted ? 'var(--expense)' : 'var(--income)', display: 'inline-block' } }),
              'Realizado'
            )
          )
        ),
        React.createElement('div', { style: { height: 220 } },
          React.createElement(BurnDownChart, { cat: selectedCat })
        ),
        React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', marginTop: 14, padding: '12px 0 0', borderTop: '1px solid var(--border)' } },
          React.createElement('span', { style: { fontSize: 13, color: 'var(--text-secondary)' } },
            `Orçado: `, React.createElement('strong', null, fmt(selectedCat.budgeted))
          ),
          React.createElement('span', { style: { fontSize: 13, color: 'var(--text-secondary)' } },
            `Gasto até hoje: `, React.createElement('strong', { style: { color: selectedCat.spent > selectedCat.budgeted ? 'var(--expense)' : 'var(--income)' } }, fmt(selectedCat.spent))
          ),
          React.createElement('span', { style: { fontSize: 13, color: 'var(--text-secondary)' } },
            `Restante: `, React.createElement('strong', { style: { color: 'var(--accent)' } }, fmt(Math.max(0, selectedCat.budgeted - selectedCat.spent)))
          ),
          React.createElement('span', { style: { fontSize: 13, color: 'var(--text-secondary)' } },
            `Projeção final: `, React.createElement('strong', { style: { color: selectedCat.spent * (30/24) > selectedCat.budgeted ? 'var(--expense)' : 'var(--income)' } }, fmt(selectedCat.spent * (30 / 24)))
          )
        )
      )
    )
  );
};
