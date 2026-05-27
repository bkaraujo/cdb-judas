// dashboard.jsx — Dashboard com painéis configuráveis e modal de personalização
const { useState: useDashState, useMemo: useDashMemo, useEffect: useDashEffect } = React;

// ── Dados mock ────────────────────────────────────────────────
const MONTHLY_DATA = [
  { month: 'Jan', receitas: 8200,  despesas: 5100 },
  { month: 'Fev', receitas: 8500,  despesas: 4800 },
  { month: 'Mar', receitas: 9200,  despesas: 6200 },
  { month: 'Abr', receitas: 8500,  despesas: 3900 },
];
const UPCOMING = [
  { id:1, name:'Aluguel',       due:'2026-05-02', amount:2500,   type:'expense' },
  { id:2, name:'Energia',       due:'2026-05-05', amount:342.1,  type:'expense' },
  { id:3, name:'Salário',       due:'2026-05-05', amount:8500,   type:'income'  },
  { id:4, name:'Internet',      due:'2026-05-08', amount:119.9,  type:'expense' },
  { id:5, name:'Gympass',       due:'2026-05-10', amount:99.9,   type:'expense' },
  { id:6, name:'Nubank fatura', due:'2026-05-15', amount:2345.8, type:'expense' },
];
const CONTAS_RECEBERS = [
  { id:1, name:'Salário',         due:'2026-05-05', amount:8500  },
  { id:2, name:'Freela Design',   due:'2026-05-12', amount:1200  },
  { id:3, name:'Aluguel imóvel',  due:'2026-05-10', amount:1800  },
];
const RECENT = [
  { id:1, desc:'Mercado Extra',      cat:'Alimentação', date:'2026-04-23', amount:-245.6,  icon:'🛒' },
  { id:2, desc:'Transferência Itaú', cat:'Transferência',date:'2026-04-22', amount:+1200,  icon:'🏦' },
  { id:3, desc:'Posto Shell',        cat:'Transporte',  date:'2026-04-22', amount:-187.4,  icon:'⛽' },
  { id:4, desc:'Netflix',            cat:'Lazer',       date:'2026-04-21', amount:-55.9,   icon:'🎬' },
  { id:5, desc:'Farmácia',           cat:'Saúde',       date:'2026-04-20', amount:-89.5,   icon:'💊' },
];
const EXPENSE_CATS = [
  { name:'Moradia',     amount:2920, color:'#6366F1' },
  { name:'Alimentação', amount:813,  color:'#38BDF8' },
  { name:'Transporte',  amount:222,  color:'#F59E0B' },
  { name:'Saúde',       amount:210,  color:'#10B981' },
  { name:'Lazer',       amount:390,  color:'#F43F5E' },
  { name:'Serviços',    amount:342,  color:'#A78BFA' },
];
const ACCOUNTS = [
  { name:'Conta Corrente Itaú', balance:12450, color:'#6366F1' },
  { name:'Nubank',              balance:5430,  color:'#820AD1' },
  { name:'Poupança BB',         balance:18920, color:'#F59E0B' },
];
const BUDGET_GOALS = [
  { name:'Alimentação', budgeted:1200, spent:813,  color:'#6366F1' },
  { name:'Moradia',     budgeted:2800, spent:2920, color:'#F43F5E' },
  { name:'Transporte',  budgeted:500,  spent:222,  color:'#F59E0B' },
  { name:'Lazer',       budgeted:300,  spent:390,  color:'#38BDF8' },
];
const CARDS_DATA = [
  { name:'Nubank', limit:8000,  used:2345.8, color:'#820AD1' },
  { name:'XP Visa',limit:15000, used:1890.4, color:'#1C2951' },
];

// ── Definição dos painéis ─────────────────────────────────────
const ALL_PANELS = [
  { id:'saldos-caixa',      label:'Saldos de caixa',                defaultOn: true  },
  { id:'resultado-mes',     label:'Resultado do mês',               defaultOn: true  },
  { id:'despesas-cat',      label:'Despesas por categoria',         defaultOn: true  },
  { id:'contas-pagar',      label:'Contas a pagar',                 defaultOn: true  },
  { id:'contas-receber',    label:'Contas a receber',               defaultOn: false },
  { id:'cartoes-credito',   label:'Cartões de crédito',             defaultOn: true  },
  { id:'fluxo-caixa',       label:'Fluxo de caixa',                 defaultOn: true  },
  { id:'metas-despesa',     label:'Metas de despesa',               defaultOn: true  },
  { id:'ultimos-lanc',      label:'Últimos lançamentos',            defaultOn: true  },
  { id:'balanco',           label:'Balanço patrimonial',            defaultOn: false },
  { id:'despesas-centro',   label:'Despesas por centro',            defaultOn: false },
  { id:'metas-despesa-ctr', label:'Metas de despesa por centro',    defaultOn: false },
  { id:'metas-economia',    label:'Metas de economia',              defaultOn: false },
  { id:'metas-invest',      label:'Metas de investimentos',         defaultOn: false },
  { id:'metas-receita',     label:'Metas de receita',               defaultOn: false },
  { id:'metas-receita-ctr', label:'Metas de receita por centro',    defaultOn: false },
  { id:'receitas-cat',      label:'Receitas por categoria',         defaultOn: false },
  { id:'receitas-centro',   label:'Receitas por centro',            defaultOn: false },
  { id:'resultados-caixa',  label:'Resultados de caixa',            defaultOn: false },
  { id:'resultados-centro', label:'Resultados por centro',          defaultOn: false },
];

const DEFAULT_SETTINGS = {
  viewMode: 'caixa',
  columns: 2,
  scrollPanels: false,
  includeInvestments: true,
  enabled: Object.fromEntries(ALL_PANELS.map(p => [p.id, p.defaultOn])),
  panelOrder: ALL_PANELS.map(p => p.id),
};

function loadSettings() {
  try {
    const s = localStorage.getItem('cbd-dashboard-settings');
    return s ? { ...DEFAULT_SETTINGS, ...JSON.parse(s) } : DEFAULT_SETTINGS;
  } catch { return DEFAULT_SETTINGS; }
}

// ── Mini gráficos SVG ─────────────────────────────────────────
function MiniLineChart({ data, colorInc, colorExp }) {
  const W = 400, H = 100;
  const pad = { l: 10, r: 10, t: 8, b: 20 };
  const cw = W - pad.l - pad.r;
  const ch = H - pad.t - pad.b;
  const maxV = Math.max(...data.flatMap(d => [d.receitas, d.despesas])) * 1.1;
  const toX = i => pad.l + (i / (data.length - 1)) * cw;
  const toY = v => pad.t + (1 - v / maxV) * ch;
  const incPts = data.map((d, i) => ({ x: toX(i), y: toY(d.receitas) }));
  const expPts = data.map((d, i) => ({ x: toX(i), y: toY(d.despesas) }));
  const areaInc = smoothPath(incPts) + ` L ${toX(data.length-1)},${H-pad.b} L ${pad.l},${H-pad.b} Z`;
  const areaExp = smoothPath(expPts) + ` L ${toX(data.length-1)},${H-pad.b} L ${pad.l},${H-pad.b} Z`;
  return React.createElement('svg', { viewBox:`0 0 ${W} ${H}`, style:{width:'100%', height:'100%'} },
    React.createElement('defs', null,
      React.createElement('linearGradient', { id:'mg-inc', x1:0, y1:0, x2:0, y2:1 },
        React.createElement('stop', { offset:'0%', stopColor:'var(--income)', stopOpacity:0.2 }),
        React.createElement('stop', { offset:'100%', stopColor:'var(--income)', stopOpacity:0.01 })
      ),
      React.createElement('linearGradient', { id:'mg-exp', x1:0, y1:0, x2:0, y2:1 },
        React.createElement('stop', { offset:'0%', stopColor:'var(--expense)', stopOpacity:0.15 }),
        React.createElement('stop', { offset:'100%', stopColor:'var(--expense)', stopOpacity:0.01 })
      )
    ),
    data.map((d, i) => React.createElement('text', { key:i, x:toX(i), y:H-6, textAnchor:'middle', fontSize:9, fill:'var(--text-muted)' }, d.month)),
    React.createElement('path', { d:areaInc, fill:'url(#mg-inc)' }),
    React.createElement('path', { d:areaExp, fill:'url(#mg-exp)' }),
    React.createElement('path', { d:smoothPath(incPts), fill:'none', stroke:'var(--income)', strokeWidth:2, strokeLinecap:'round' }),
    React.createElement('path', { d:smoothPath(expPts), fill:'none', stroke:'var(--expense)', strokeWidth:2, strokeLinecap:'round' }),
    incPts.map((p,i) => React.createElement('circle', { key:i, cx:p.x, cy:p.y, r:3, fill:'var(--income)', stroke:'var(--bg-card)', strokeWidth:1.5 })),
    expPts.map((p,i) => React.createElement('circle', { key:i, cx:p.x, cy:p.y, r:3, fill:'var(--expense)', stroke:'var(--bg-card)', strokeWidth:1.5 }))
  );
}

function CategoryBars({ data }) {
  const max = Math.max(...data.map(d => d.amount));
  return React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:8 } },
    data.map(d =>
      React.createElement('div', { key:d.name },
        React.createElement('div', { style:{ display:'flex', justifyContent:'space-between', marginBottom:4 } },
          React.createElement('span', { style:{ fontSize:12, color:'var(--text-secondary)' } }, d.name),
          React.createElement('span', { style:{ fontSize:12, fontWeight:700, color:d.color } }, fmt(d.amount))
        ),
        React.createElement('div', { style:{ height:6, background:'var(--bg-hover)', borderRadius:3, overflow:'hidden' } },
          React.createElement('div', {
            style:{ height:'100%', borderRadius:3, width:`${(d.amount/max)*100}%`, background:d.color, transition:'width 0.5s ease' }
          })
        )
      )
    )
  );
}

// ── Componentes de painel ─────────────────────────────────────
function PanelWrap({ title, icon, children, action }) {
  return React.createElement(Card, { style:{ padding:0, overflow:'hidden', display:'flex', flexDirection:'column' } },
    React.createElement('div', {
      style:{
        padding:'11px 16px 10px', borderBottom:'1px solid var(--border)',
        display:'flex', alignItems:'center', justifyContent:'space-between', flexShrink:0,
        cursor:'grab',
      }
    },
      React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:8 } },
        React.createElement('svg', {
          width:10, height:14, viewBox:'0 0 10 14', fill:'var(--text-muted)',
          style:{ flexShrink:0, opacity:0.5 }
        },
          [2,6].map(cx =>
            [2,6,11].map(cy =>
              React.createElement('circle', { key:`${cx}-${cy}`, cx, cy, r:1.3 })
            )
          )
        ),
        React.createElement(Icon, { name:icon, size:14, color:'var(--text-muted)' }),
        React.createElement('span', { style:{ fontSize:13, fontWeight:700 } }, title)
      ),
      action
    ),
    React.createElement('div', { style:{ padding:'14px 16px', flex:1, overflow:'hidden' } }, children)
  );
}

const Panels = {
  'saldos-caixa': () => {
    const total = ACCOUNTS.reduce((s,a) => s + a.balance, 0);
    return React.createElement(PanelWrap, { title:'Saldos de Caixa', icon:'building' },
      React.createElement('div', { style:{ marginBottom:12 } },
        React.createElement('p', { style:{ fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase' } }, 'Total'),
        React.createElement('p', { style:{ fontSize:24, fontWeight:800, marginTop:2 } }, fmt(total))
      ),
      ACCOUNTS.map(a =>
        React.createElement('div', { key:a.name, style:{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'8px 0', borderTop:'1px solid var(--border-light)' } },
          React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:8 } },
            React.createElement('span', { style:{ width:8, height:8, borderRadius:'50%', background:a.color, flexShrink:0 } }),
            React.createElement('span', { style:{ fontSize:12, color:'var(--text-secondary)' } }, a.name)
          ),
          React.createElement('span', { style:{ fontSize:13, fontWeight:700 } }, fmt(a.balance))
        )
      )
    );
  },

  'resultado-mes': () => {
    const inc = 8500, exp = 3900, res = inc - exp;
    return React.createElement(PanelWrap, { title:'Resultado do Mês', icon:'activity' },
      React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:10 } },
        [
          { label:'Receitas', value:inc, color:'var(--income)' },
          { label:'Despesas', value:exp, color:'var(--expense)' },
          { label:'Resultado', value:res, color: res>=0?'var(--income)':'var(--expense)', bold:true },
        ].map(r =>
          React.createElement('div', { key:r.label, style:{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'8px 0', borderBottom:'1px solid var(--border-light)' } },
            React.createElement('span', { style:{ fontSize:13, color:'var(--text-secondary)', fontWeight:r.bold?700:400 } }, r.label),
            React.createElement('span', { style:{ fontSize:14, fontWeight:700, color:r.color } }, fmt(r.value))
          )
        ),
        React.createElement('div', { style:{ height:80, marginTop:4 } },
          React.createElement(MiniLineChart, { data:MONTHLY_DATA })
        )
      )
    );
  },

  'despesas-cat': () =>
    React.createElement(PanelWrap, { title:'Despesas por Categoria', icon:'pieChart' },
      React.createElement(CategoryBars, { data:EXPENSE_CATS.slice(0,5) })
    ),

  'contas-pagar': () =>
    React.createElement(PanelWrap, { title:'Contas a Pagar', icon:'calendar',
      action: React.createElement(Badge, { color:'expense' }, UPCOMING.filter(u=>u.type==='expense').length)
    },
      React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:0 } },
        UPCOMING.filter(u=>u.type==='expense').slice(0,5).map((bill, i) =>
          React.createElement('div', { key:bill.id, style:{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'8px 0', borderBottom:'1px solid var(--border-light)' } },
            React.createElement('div', null,
              React.createElement('p', { style:{ fontSize:12, fontWeight:600 } }, bill.name),
              React.createElement('p', { style:{ fontSize:11, color:'var(--text-muted)' } }, fmtDate(bill.due))
            ),
            React.createElement('span', { style:{ fontSize:13, fontWeight:700, color:'var(--expense)' } }, '-' + fmt(bill.amount))
          )
        )
      )
    ),

  'contas-receber': () =>
    React.createElement(PanelWrap, { title:'Contas a Receber', icon:'arrowUp',
      action: React.createElement(Badge, { color:'income' }, CONTAS_RECEBERS.length)
    },
      React.createElement('div', null,
        CONTAS_RECEBERS.map(r =>
          React.createElement('div', { key:r.id, style:{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'8px 0', borderBottom:'1px solid var(--border-light)' } },
            React.createElement('div', null,
              React.createElement('p', { style:{ fontSize:12, fontWeight:600 } }, r.name),
              React.createElement('p', { style:{ fontSize:11, color:'var(--text-muted)' } }, fmtDate(r.due))
            ),
            React.createElement('span', { style:{ fontSize:13, fontWeight:700, color:'var(--income)' } }, '+' + fmt(r.amount))
          )
        )
      )
    ),

  'cartoes-credito': () =>
    React.createElement(PanelWrap, { title:'Cartões de Crédito', icon:'creditCard' },
      CARDS_DATA.map(card => {
        const pct = (card.used / card.limit) * 100;
        return React.createElement('div', { key:card.name, style:{ marginBottom:12 } },
          React.createElement('div', { style:{ display:'flex', justifyContent:'space-between', marginBottom:6 } },
            React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:6 } },
              React.createElement('span', { style:{ width:10, height:10, borderRadius:2, background:card.color, flexShrink:0 } }),
              React.createElement('span', { style:{ fontSize:12, fontWeight:600 } }, card.name)
            ),
            React.createElement('span', { style:{ fontSize:12, color:'var(--expense)', fontWeight:700 } }, fmt(card.used))
          ),
          React.createElement('div', { style:{ height:6, background:'var(--bg-hover)', borderRadius:3, overflow:'hidden', marginBottom:4 } },
            React.createElement('div', { style:{ height:'100%', borderRadius:3, width:`${pct}%`, background: pct>80?'var(--expense)':pct>60?'var(--warning)':'var(--accent)' } })
          ),
          React.createElement('div', { style:{ display:'flex', justifyContent:'space-between', fontSize:11, color:'var(--text-muted)' } },
            React.createElement('span', null, `${pct.toFixed(0)}% usado`),
            React.createElement('span', null, `Limite: ${fmt(card.limit)}`)
          )
        );
      })
    ),

  'fluxo-caixa': () =>
    React.createElement(PanelWrap, { title:'Fluxo de Caixa', icon:'trendingUp' },
      React.createElement('div', null,
        React.createElement('div', { style:{ display:'flex', gap:16, marginBottom:10, fontSize:12 } },
          React.createElement('span', { style:{ display:'flex', alignItems:'center', gap:4, color:'var(--text-secondary)' } },
            React.createElement('span', { style:{ width:14, height:3, borderRadius:2, background:'var(--income)', display:'inline-block' } }), 'Receitas'
          ),
          React.createElement('span', { style:{ display:'flex', alignItems:'center', gap:4, color:'var(--text-secondary)' } },
            React.createElement('span', { style:{ width:14, height:3, borderRadius:2, background:'var(--expense)', display:'inline-block' } }), 'Despesas'
          )
        ),
        React.createElement('div', { style:{ height:110 } },
          React.createElement(MiniLineChart, { data:MONTHLY_DATA })
        )
      )
    ),

  'metas-despesa': () =>
    React.createElement(PanelWrap, { title:'Metas de Despesa', icon:'target' },
      React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:10 } },
        BUDGET_GOALS.map(g => {
          const pct = Math.min((g.spent / g.budgeted) * 100, 100);
          const over = g.spent > g.budgeted;
          return React.createElement('div', { key:g.name },
            React.createElement('div', { style:{ display:'flex', justifyContent:'space-between', marginBottom:4, fontSize:12 } },
              React.createElement('span', { style:{ color:'var(--text-secondary)' } }, g.name),
              React.createElement('span', { style:{ fontWeight:700, color: over?'var(--expense)':'var(--text-primary)' } },
                `${fmt(g.spent)} / ${fmt(g.budgeted)}`
              )
            ),
            React.createElement('div', { style:{ height:6, background:'var(--bg-hover)', borderRadius:3, overflow:'hidden' } },
              React.createElement('div', { style:{
                height:'100%', borderRadius:3, width:`${pct}%`,
                background: over?'var(--expense)':pct>80?'var(--warning)':'var(--income)'
              }})
            )
          );
        })
      )
    ),

  'ultimos-lanc': () =>
    React.createElement(PanelWrap, { title:'Últimos Lançamentos', icon:'list',
      action: React.createElement(Button, { variant:'ghost', size:'sm' }, 'Ver todos')
    },
      RECENT.map(tx =>
        React.createElement('div', { key:tx.id,
          style:{ display:'flex', alignItems:'center', gap:10, padding:'7px 0', borderBottom:'1px solid var(--border-light)' }
        },
          React.createElement('span', { style:{ fontSize:16, flexShrink:0 } }, tx.icon),
          React.createElement('div', { style:{ flex:1, minWidth:0 } },
            React.createElement('p', { style:{ fontSize:12, fontWeight:600, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' } }, tx.desc),
            React.createElement('p', { style:{ fontSize:11, color:'var(--text-muted)' } }, tx.cat)
          ),
          React.createElement('span', { style:{ fontSize:12, fontWeight:700, color:tx.amount>0?'var(--income)':'var(--expense)', flexShrink:0 } },
            (tx.amount>0?'+':'') + fmt(tx.amount)
          )
        )
      )
    ),

  'balanco': () =>
    React.createElement(PanelWrap, { title:'Balanço Patrimonial', icon:'database' },
      React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:8 } },
        [
          { label:'Ativo Total',    value:36800+5000, color:'var(--income)' },
          { label:'Passivo Total',  value:4235.7,     color:'var(--expense)' },
          { label:'Patrimônio Líq.',value:36800+5000-4235.7, color:'var(--accent)', bold:true },
        ].map(r =>
          React.createElement('div', { key:r.label, style:{ display:'flex', justifyContent:'space-between', padding:'8px 0', borderBottom:'1px solid var(--border-light)' } },
            React.createElement('span', { style:{ fontSize:13, color:'var(--text-secondary)' } }, r.label),
            React.createElement('span', { style:{ fontSize:14, fontWeight:700, color:r.color } }, fmt(r.value))
          )
        )
      )
    ),
};

function StubPanel({ title, icon }) {
  return React.createElement(PanelWrap, { title, icon },
    React.createElement(EmptyState, { icon:'activity', title:'Em breve', desc:'Dados disponíveis em breve.' })
  );
}

function getPanelComponent(id) {
  const comp = Panels[id];
  if (comp) return React.createElement(comp);
  const def = ALL_PANELS.find(p => p.id === id);
  return React.createElement(StubPanel, { title: def?.label || id, icon:'barChart' });
}

// ── Modal de personalização ───────────────────────────────────
function CustomizeModal({ open, onClose, settings, onSave }) {
  const [local, setLocal] = useDashState(settings);
  const set = (k, v) => setLocal(s => ({ ...s, [k]: v }));
  const togglePanel = (id) => setLocal(s => ({
    ...s, enabled: { ...s.enabled, [id]: !s.enabled[id] }
  }));

  const RadioGroup = ({ field, options }) =>
    React.createElement('div', { style:{ display:'flex', gap:16, flexWrap:'wrap', marginTop:6 } },
      options.map(opt =>
        React.createElement('label', {
          key:opt.value,
          style:{ display:'flex', alignItems:'center', gap:6, cursor:'pointer', fontSize:13 }
        },
          React.createElement('input', {
            type:'radio', name:field, value:opt.value,
            checked: local[field] === opt.value,
            onChange: () => set(field, opt.value),
            style:{ accentColor:'var(--accent)', cursor:'pointer' }
          }),
          opt.label
        )
      )
    );

  return React.createElement(Modal, {
    open, onClose: () => { setLocal(settings); onClose(); },
    title: 'Personalizar visão geral',
    footer: React.createElement(React.Fragment, null,
      React.createElement(Button, { variant:'secondary', onClick:()=>{ setLocal(settings); onClose(); } }, 'Cancelar'),
      React.createElement(Button, { onClick:()=>{ onSave(local); onClose(); } }, 'Salvar')
    )
  },
    // Seção: modo de exibição
    React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:18 } },
      React.createElement('div', null,
        React.createElement('p', { style:{ fontSize:12, fontWeight:700, color:'var(--text-secondary)', marginBottom:6 } },
          'Exibir painéis de receitas e despesas na visão de'
        ),
        React.createElement(RadioGroup, { field:'viewMode', options:[
          { value:'caixa', label:'Caixa' },
          { value:'competencia', label:'Competência' },
          { value:'definir', label:'Definir em cada painel' },
        ]})
      ),

      React.createElement('div', { style:{ borderTop:'1px solid var(--border)', paddingTop:16 } },
        React.createElement('p', { style:{ fontSize:12, fontWeight:700, color:'var(--text-secondary)', marginBottom:6 } }, 'Layout de exibição'),
        React.createElement(RadioGroup, { field:'columns', options:[
          { value:1, label:'1 coluna' },
          { value:2, label:'2 colunas' },
          { value:3, label:'3 colunas' },
        ]})
      ),

      React.createElement('div', { style:{ borderTop:'1px solid var(--border)', paddingTop:16 } },
        React.createElement('p', { style:{ fontSize:12, fontWeight:700, color:'var(--text-secondary)', marginBottom:6 } }, 'Utilizar barra de rolagem nos painéis'),
        React.createElement(RadioGroup, { field:'scrollPanels', options:[
          { value:true, label:'Sim' },
          { value:false, label:'Não' },
        ]})
      ),

      React.createElement('div', { style:{ borderTop:'1px solid var(--border)', paddingTop:16 } },
        React.createElement('p', { style:{ fontSize:12, fontWeight:700, color:'var(--text-secondary)', marginBottom:6 } },
          'Considerar despesas e receitas vinculadas ao módulo de investimentos'
        ),
        React.createElement(RadioGroup, { field:'includeInvestments', options:[
          { value:true, label:'Sim' },
          { value:false, label:'Não' },
        ]})
      ),

      React.createElement('div', { style:{ borderTop:'1px solid var(--border)', paddingTop:16 } },
        React.createElement('p', { style:{ fontSize:12, fontWeight:700, color:'var(--text-secondary)', marginBottom:10 } },
          'Habilitar e desabilitar painéis'
        ),
        React.createElement('div', { style:{ display:'flex', flexDirection:'column', gap:6 } },
          ALL_PANELS.map(p =>
            React.createElement('label', {
              key:p.id,
              style:{ display:'flex', alignItems:'center', gap:10, padding:'7px 10px', borderRadius:'var(--radius-sm)', cursor:'pointer', transition:'background var(--transition)' },
              onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
              onMouseLeave:e=>e.currentTarget.style.background='transparent'
            },
              React.createElement('input', {
                type:'checkbox',
                checked: !!local.enabled[p.id],
                onChange: () => togglePanel(p.id),
                style:{ width:15, height:15, accentColor:'var(--accent)', cursor:'pointer', flexShrink:0 }
              }),
              React.createElement('span', { style:{ fontSize:13 } }, p.label)
            )
          )
        )
      )
    )
  );
}

// ── Dashboard principal ───────────────────────────────────────
window.DashboardScreen = function DashboardScreen() {
  const [settings, setSettings] = useDashState(loadSettings);
  const [customizeOpen, setCustomizeOpen] = useDashState(false);
  const [hideValues, setHideValues] = useDashState(false);
  const [draggedId, setDraggedId] = useDashState(null);
  const [dragOverId, setDragOverId] = useDashState(null);

  useDashEffect(() => {
    localStorage.setItem('cbd-dashboard-settings', JSON.stringify(settings));
  }, [settings]);

  // Build ordered enabled panels
  const order = settings.panelOrder && settings.panelOrder.length
    ? settings.panelOrder
    : ALL_PANELS.map(p => p.id);

  const enabledPanels = order
    .map(id => ALL_PANELS.find(p => p.id === id))
    .filter(p => p && settings.enabled[p.id]);

  const cols = settings.columns || 2;

  // Drag handlers
  const handleDragStart = (e, id) => {
    setDraggedId(id);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', id);
  };
  const handleDragOver = (e, id) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (id !== dragOverId) setDragOverId(id);
  };
  const handleDrop = (e, targetId) => {
    e.preventDefault();
    if (!draggedId || draggedId === targetId) return;
    setSettings(s => {
      const currentOrder = s.panelOrder && s.panelOrder.length
        ? [...s.panelOrder]
        : ALL_PANELS.map(p => p.id);
      const fromIdx = currentOrder.indexOf(draggedId);
      const toIdx = currentOrder.indexOf(targetId);
      if (fromIdx === -1 || toIdx === -1) return s;
      const newOrder = [...currentOrder];
      newOrder.splice(fromIdx, 1);
      newOrder.splice(toIdx, 0, draggedId);
      return { ...s, panelOrder: newOrder };
    });
    setDraggedId(null);
    setDragOverId(null);
  };
  const handleDragEnd = () => { setDraggedId(null); setDragOverId(null); };


  return React.createElement('div', { style:{ opacity:1 } },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Visão Geral'),
      React.createElement('div', { className:'page-header-actions' },
        React.createElement('button', {
          className:'icon-btn', onClick:()=>setHideValues(v=>!v),
          title: hideValues?'Mostrar valores':'Ocultar valores',
          style:{ width:34, height:34 }
        }, React.createElement(Icon, { name:hideValues?'eyeOff':'eye', size:16 })),
        React.createElement(Button, { variant:'secondary', icon:'settings', size:'sm', onClick:()=>setCustomizeOpen(true) }, 'Personalizar'),
        React.createElement(Button, { icon:'plus', size:'sm' }, 'Novo Lançamento')
      )
    ),

    React.createElement('div', {
      style:{
        display:'grid',
        gridTemplateColumns:`repeat(${cols}, 1fr)`,
        gap:14, alignItems:'start',
      }
    },
      enabledPanels.map(p =>
        React.createElement('div', {
          key: p.id,
          draggable: true,
          onDragStart: e => handleDragStart(e, p.id),
          onDragOver:  e => handleDragOver(e, p.id),
          onDrop:      e => handleDrop(e, p.id),
          onDragEnd:   handleDragEnd,
          style:{
            opacity: draggedId === p.id ? 0.35 : 1,
            transform: dragOverId === p.id && draggedId !== p.id ? 'scale(1.01)' : 'none',
            transition: 'opacity 0.15s, transform 0.15s',
            borderRadius: 'var(--radius)',
            outline: dragOverId === p.id && draggedId !== p.id
              ? '2px solid var(--accent)' : '2px solid transparent',
          }
        },
          getPanelComponent(p.id)
        )
      )
    ),

    enabledPanels.length === 0 && React.createElement(EmptyState, {
      icon:'settings', title:'Nenhum painel habilitado',
      desc:'Clique em "Personalizar" para adicionar painéis à visão geral.'
    }),

    React.createElement(CustomizeModal, {
      open:customizeOpen,
      onClose:()=>setCustomizeOpen(false),
      settings,
      onSave:s => setSettings(s),
    })
  );
};
