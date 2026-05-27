// relatorios.jsx — Reports grid + Category Year Evolution com hierarquia expandível
const { useState: useRelState } = React;

const MONTHS_LABELS = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'];
const TODAY_MONTH = 3; // Abril = índice 3

// ── Hierarquia de categorias ──────────────────────────────────
// Estrutura: Macro > Categoria (com subcategorias obrigatórias)
const YEAR_HIERARCHY = [
  {
    id:'despesas', label:'Despesas', type:'macro', color:'var(--expense)',
    categories: [
      {
        id:'moradia', label:'Moradia', color:'#6366F1',
        subcats: [
          { id:'aluguel',     label:'Aluguel',       data:[2500,2500,2500,2500,null,null,null,null,null,null,null,null] },
          { id:'agua',        label:'Água',           data:[78,83,75,78,null,null,null,null,null,null,null,null] },
          { id:'energia',     label:'Energia',        data:[222,217,225,342,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'alimentacao', label:'Alimentação', color:'#38BDF8',
        subcats: [
          { id:'mercado',     label:'Mercado',        data:[620,680,520,550,null,null,null,null,null,null,null,null] },
          { id:'restaurante', label:'Restaurantes',   data:[210,230,245,190,null,null,null,null,null,null,null,null] },
          { id:'ifood',       label:'iFood/Delivery', data:[150,140,125,73,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'transporte', label:'Transporte', color:'#F59E0B',
        subcats: [
          { id:'combustivel', label:'Combustível',    data:[180,150,230,120,null,null,null,null,null,null,null,null] },
          { id:'uber',        label:'Uber/99',        data:[85,80,110,67,null,null,null,null,null,null,null,null] },
          { id:'estac',       label:'Estacionamento', data:[45,50,80,35,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'saude', label:'Saúde', color:'#10B981',
        subcats: [
          { id:'farmacia',    label:'Farmácia',       data:[45,80,32,90,null,null,null,null,null,null,null,null] },
          { id:'consultas',   label:'Consultas',      data:[0,80,0,120,null,null,null,null,null,null,null,null] },
          { id:'plano',       label:'Plano de Saúde', data:[75,40,57,0,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'lazer', label:'Lazer', color:'#F43F5E',
        subcats: [
          { id:'streaming',   label:'Streaming',      data:[77.8,77.8,77.8,77.8,null,null,null,null,null,null,null,null] },
          { id:'cinema',      label:'Cinema',         data:[52,0,80,42,null,null,null,null,null,null,null,null] },
          { id:'bares',       label:'Bares/Lazer',    data:[120.2,102.2,182.2,270.2,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'servicos', label:'Serviços', color:'#A78BFA',
        subcats: [
          { id:'internet',    label:'Internet',       data:[119.9,119.9,119.9,119.9,null,null,null,null,null,null,null,null] },
          { id:'telefone',    label:'Telefone',       data:[80,80,80,80,null,null,null,null,null,null,null,null] },
          { id:'assinaturas', label:'Assinaturas',    data:[140.1,142.1,142.1,142.1,null,null,null,null,null,null,null,null] },
        ]
      },
    ]
  },
  {
    id:'receitas', label:'Receitas', type:'macro', color:'var(--income)',
    categories: [
      {
        id:'trabalho', label:'Trabalho', color:'#10B981',
        subcats: [
          { id:'salario',     label:'Salário',        data:[8200,8500,8000,8500,null,null,null,null,null,null,null,null] },
          { id:'freelance',   label:'Freelance',      data:[0,0,1200,0,null,null,null,null,null,null,null,null] },
          { id:'bonus',       label:'Bônus',          data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'investimentos', label:'Investimentos', color:'#6366F1',
        subcats: [
          { id:'dividendos',  label:'Dividendos',     data:[0,0,0,0,null,null,null,null,null,null,null,null] },
          { id:'juros',       label:'Juros/CDB',      data:[0,0,0,0,null,null,null,null,null,null,null,null] },
          { id:'aluguel-rec', label:'Aluguel recebido',data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        ]
      },
      {
        id:'outros-rec', label:'Outros Recebimentos', color:'#F59E0B',
        subcats: [
          { id:'reembolso',   label:'Reembolsos',     data:[0,0,0,0,null,null,null,null,null,null,null,null] },
          { id:'vendas',      label:'Vendas',         data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        ]
      },
    ]
  },
];

// Calcular totais por mês para categoria e macro
function catMonthData(cat) {
  return Array.from({ length: 12 }, (_, m) => {
    const vals = cat.subcats.map(s => s.data[m]).filter(v => v !== null);
    return vals.length ? vals.reduce((a, b) => a + b, 0) : null;
  });
}
function macroMonthData(macro) {
  return Array.from({ length: 12 }, (_, m) => {
    const vals = macro.categories.map(c => catMonthData(c)[m]).filter(v => v !== null);
    return vals.length ? vals.reduce((a, b) => a + b, 0) : null;
  });
}
function sumData(data) { return data.filter(v => v !== null).reduce((a, b) => a + b, 0); }
function avgData(data) { const v = data.filter(v => v !== null); return v.length ? v.reduce((a,b)=>a+b,0)/v.length : 0; }

// ── Gráfico de evolução ───────────────────────────────────────
function CategoryYearChart({ activeCats }) {
  const W = 700, H = 240;
  const pad = { l: 60, r: 24, t: 20, b: 36 };
  const cw = W - pad.l - pad.r;
  const ch = H - pad.t - pad.b;
  const n = MONTHS_LABELS.length;

  const allSeries = YEAR_HIERARCHY.flatMap(macro =>
    macro.categories.map(cat => ({ ...cat, monthData: catMonthData(cat) }))
  ).filter(s => activeCats.includes(s.id));

  const allVals = allSeries.flatMap(s => s.monthData.filter(v => v !== null));
  if (!allVals.length) return null;
  const maxV = Math.max(...allVals) * 1.12;

  const toX = i => pad.l + (i / (n - 1)) * cw;
  const toY = v => pad.t + (1 - v / maxV) * ch;

  const gridVals = [0, 0.33, 0.66, 1].map(f => ({ y: pad.t + (1 - f) * ch, v: f * maxV }));

  return React.createElement('svg', { viewBox:`0 0 ${W} ${H}`, style:{ width:'100%', height:'100%' } },
    gridVals.map((g, i) =>
      React.createElement(React.Fragment, { key:i },
        React.createElement('line', { x1:pad.l, y1:g.y, x2:W-pad.r, y2:g.y, stroke:'var(--border)', strokeWidth:1 }),
        React.createElement('text', { x:pad.l-6, y:g.y+4, textAnchor:'end', fontSize:9, fill:'var(--text-muted)' }, fmtShort(g.v))
      )
    ),
    React.createElement('rect', {
      x:toX(TODAY_MONTH)+(toX(1)-toX(0))/2, y:pad.t,
      width:W-pad.r-toX(TODAY_MONTH)-(toX(1)-toX(0))/2, height:ch,
      fill:'var(--bg-hover)', opacity:0.5
    }),
    React.createElement('text', { x:toX(TODAY_MONTH+1), y:pad.t+12, fontSize:9, fill:'var(--text-muted)', textAnchor:'middle' }, 'Projeção →'),
    MONTHS_LABELS.map((l, i) =>
      React.createElement('text', { key:i, x:toX(i), y:H-8, textAnchor:'middle', fontSize:10, fill:i<=TODAY_MONTH?'var(--text-secondary)':'var(--text-muted)' }, l)
    ),
    allSeries.map(s => {
      const realPts = s.monthData
        .map((v, i) => v !== null && i <= TODAY_MONTH ? { x:toX(i), y:toY(v) } : null)
        .filter(Boolean);
      return React.createElement(React.Fragment, { key:s.id },
        realPts.length > 1 && React.createElement('path', { d:smoothPath(realPts), fill:'none', stroke:s.color, strokeWidth:2.5, strokeLinecap:'round' }),
        realPts.map((p, i) => React.createElement('circle', { key:i, cx:p.x, cy:p.y, r:3.5, fill:s.color, stroke:'var(--bg-card)', strokeWidth:1.5 }))
      );
    })
  );
}

// ── Tabela hierárquica ────────────────────────────────────────
const REAL_MONTHS = MONTHS_LABELS.slice(0, TODAY_MONTH + 1);

function TableCell({ value, bold, color, muted, indent = 0 }) {
  return React.createElement('td', {
    style:{
      padding:'10px 14px', textAlign:'right', fontSize:13,
      fontWeight: bold ? 700 : 400,
      color: color || (muted ? 'var(--text-muted)' : 'var(--text-primary)'),
      whiteSpace:'nowrap',
    }
  }, value !== null && value !== undefined && value !== 0 ? fmt(value) : value === 0 ? '—' : '—');
}

function HierarchyTable({ openMacros, toggleMacro, openCats, toggleCat, activeCats, toggleActiveCat }) {
  return React.createElement('div', { style:{ overflowX:'auto' } },
    React.createElement('table', { style:{ width:'100%', borderCollapse:'collapse', minWidth:700 } },
      React.createElement('thead', null,
        React.createElement('tr', { style:{ borderBottom:'2px solid var(--border)' } },
          React.createElement('th', { style:{ padding:'10px 16px', textAlign:'left', fontSize:11, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.05em', minWidth:200 } }, 'Categoria'),
          ...REAL_MONTHS.map(m => React.createElement('th', { key:m, style:{ padding:'10px 14px', textAlign:'right', fontSize:11, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.05em' } }, m)),
          React.createElement('th', { style:{ padding:'10px 14px', textAlign:'right', fontSize:11, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.05em' } }, 'Total'),
          React.createElement('th', { style:{ padding:'10px 14px', textAlign:'right', fontSize:11, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.05em' } }, 'Média/mês'),
        )
      ),
      React.createElement('tbody', null,
        YEAR_HIERARCHY.map(macro => {
          const macroData = macroMonthData(macro);
          const macroOpen = openMacros[macro.id];
          return React.createElement(React.Fragment, { key:macro.id },
            // ── Linha Macro ──
            React.createElement('tr', {
              onClick: () => toggleMacro(macro.id),
              style:{
                background:'var(--bg-hover)', cursor:'pointer',
                borderTop:'2px solid var(--border)',
                transition:'background var(--transition)',
              },
              onMouseEnter:e=>e.currentTarget.style.background='var(--border)',
              onMouseLeave:e=>e.currentTarget.style.background='var(--bg-hover)',
            },
              React.createElement('td', { style:{ padding:'12px 16px' } },
                React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:8 } },
                  React.createElement('div', {
                    style:{
                      width:18, height:18, borderRadius:4,
                      background: macro.id==='despesas'?'var(--expense-light)':'var(--income-light)',
                      display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0,
                    }
                  }, React.createElement(Icon, { name: macroOpen?'chevronDown':'chevronRight', size:11, color: macro.id==='despesas'?'var(--expense)':'var(--income)' })),
                  React.createElement('span', { style:{ fontSize:14, fontWeight:800, color: macro.id==='despesas'?'var(--expense)':'var(--income)' } }, macro.label)
                )
              ),
              ...macroData.slice(0,TODAY_MONTH+1).map((v,i)=>React.createElement(TableCell,{key:i,value:v,bold:true,color:macro.id==='despesas'?'var(--expense)':'var(--income)'})),
              React.createElement(TableCell, { value:sumData(macroData), bold:true, color:macro.id==='despesas'?'var(--expense)':'var(--income)' }),
              React.createElement(TableCell, { value:avgData(macroData.slice(0,TODAY_MONTH+1)), bold:true, color:macro.id==='despesas'?'var(--expense)':'var(--income)' }),
            ),

            // ── Linhas de categorias ──
            macroOpen && macro.categories.map(cat => {
              const catData = catMonthData(cat);
              const catOpen = openCats[cat.id];
              return React.createElement(React.Fragment, { key:cat.id },
                React.createElement('tr', {
                  onClick: () => toggleCat(cat.id),
                  style:{ cursor:'pointer', borderBottom:'1px solid var(--border-light)', transition:'background var(--transition)' },
                  onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
                  onMouseLeave:e=>e.currentTarget.style.background='transparent',
                },
                  React.createElement('td', { style:{ padding:'10px 16px 10px 36px' } },
                    React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:8 } },
                      React.createElement('div', {
                        style:{
                          width:16, height:16, borderRadius:3,
                          background:cat.color+'22',
                          display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0,
                        }
                      }, React.createElement(Icon, { name:catOpen?'chevronDown':'chevronRight', size:10, color:cat.color })),
                      React.createElement('span', { style:{ width:8, height:8, borderRadius:'50%', background:cat.color, flexShrink:0 } }),
                      React.createElement('span', { style:{ fontSize:13, fontWeight:600 } }, cat.label),
                      React.createElement(Badge, { color:'muted' }, cat.subcats.length),
                      React.createElement('button', {
                        onClick: e => { e.stopPropagation(); toggleActiveCat(cat.id); },
                        title: activeCats.includes(cat.id) ? 'Ocultar no gráfico' : 'Exibir no gráfico',
                        style:{
                          marginLeft:'auto', background:'none', border:'none', cursor:'pointer', padding:'2px 4px',
                          color: activeCats.includes(cat.id) ? cat.color : 'var(--text-muted)',
                          opacity: activeCats.includes(cat.id) ? 1 : 0.45,
                          display:'flex', alignItems:'center',
                          transition:'all var(--transition)',
                        }
                      }, React.createElement(Icon, { name: activeCats.includes(cat.id) ? 'eye' : 'eyeOff', size:14 }))
                    )
                  ),
                  ...catData.slice(0,TODAY_MONTH+1).map((v,i)=>React.createElement(TableCell,{key:i,value:v,bold:false})),
                  React.createElement(TableCell, { value:sumData(catData), bold:true }),
                  React.createElement(TableCell, { value:avgData(catData.slice(0,TODAY_MONTH+1)), muted:true }),
                ),

                // ── Linhas de subcategorias ──
                catOpen && cat.subcats.map(sub =>
                  React.createElement('tr', {
                    key:sub.id,
                    style:{ borderBottom:'1px solid var(--border-light)', transition:'background var(--transition)' },
                    onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
                    onMouseLeave:e=>e.currentTarget.style.background='transparent',
                  },
                    React.createElement('td', { style:{ padding:'8px 16px 8px 60px' } },
                      React.createElement('span', { style:{ fontSize:12, color:'var(--text-secondary)', display:'flex', alignItems:'center', gap:6 } },
                        React.createElement('span', { style:{ color:'var(--text-muted)', fontSize:11 } }, '↳'),
                        sub.label
                      )
                    ),
                    ...sub.data.slice(0,TODAY_MONTH+1).map((v,i)=>React.createElement(TableCell,{key:i,value:v,muted:true})),
                    React.createElement(TableCell, { value:sumData(sub.data), bold:false }),
                    React.createElement(TableCell, { value:avgData(sub.data.slice(0,TODAY_MONTH+1)), muted:true }),
                  )
                )
              );
            })
          );
        })
      )
    )
  );
}

// ── Grade de relatórios ───────────────────────────────────────
const REPORT_CARDS = [
  { id:'balanço',     title:'Balanço Patrimonial',       desc:'Ativos, passivos e patrimônio líquido',             icon:'building',   color:'accent' },
  { id:'evolucao-bp', title:'Evolução do Balanço',       desc:'Variação do patrimônio ao longo do tempo',          icon:'trendingUp', color:'income' },
  { id:'cat-total',   title:'Totais por Categoria',      desc:'Receitas e despesas agrupadas por categoria',       icon:'pieChart',   color:'accent' },
  { id:'cat-evo',     title:'Evolução por Categoria',    desc:'Tendência de gastos por categoria no ano',          icon:'activity',   color:'income' },
  { id:'fluxo',       title:'Fluxo de Caixa',            desc:'Entradas, saídas e evolução do saldo',              icon:'trendingUp', color:'info'   },
  { id:'cmp-periodo', title:'Comparação entre Períodos', desc:'Compare resultados financeiros de diferentes meses',icon:'barChart',   color:'warning'},
  { id:'contas-pagar-rel',title:'Contas a Pagar',        desc:'Compromissos financeiros futuros com vencimento',   icon:'calendar',   color:'expense'},
  { id:'lancamentos-cat',title:'Lançamentos por Categoria',desc:'Lista detalhada organizada por categoria',        icon:'list',       color:'accent' },
  { id:'contas-pagas',title:'Contas Pagas',              desc:'Histórico de todos os pagamentos realizados',       icon:'check',      color:'income' },
  { id:'centros',     title:'Totais por Centro',         desc:'Receitas e despesas agrupadas por centro',          icon:'briefcase',  color:'info'   },
  { id:'metas-cat',   title:'Evolução das Metas',        desc:'Comparativo entre meta e realizado por categoria',  icon:'target',     color:'warning'},
  { id:'patrimonio',  title:'Patrimônio ao Longo do Ano',desc:'Crescimento do patrimônio mês a mês',              icon:'trendingUp', color:'income' },
];

window.RelatoriosScreen = function RelatoriosScreen() {
  const [activeReport, setActiveReport] = useRelState(null);
  const [activeCats, setActiveCats] = useRelState(
    YEAR_HIERARCHY.flatMap(m => m.categories.map(c => c.id))
  );
  const [openMacros, setOpenMacros] = useRelState({ despesas:true, receitas:false });
  const [openCats, setOpenCats] = useRelState({});

  const toggleMacro = id => setOpenMacros(p => ({ ...p, [id]: !p[id] }));
  const toggleCat = id => setOpenCats(p => ({ ...p, [id]: !p[id] }));
  const toggleActiveCat = id => setActiveCats(p => p.includes(id) ? p.filter(x=>x!==id) : [...p,id]);

  const COLOR_MAP = { accent:'var(--accent)', income:'var(--income)', expense:'var(--expense)', warning:'var(--warning)', info:'var(--info)' };

  if (activeReport === 'cat-evo') {
    const allCats = YEAR_HIERARCHY.flatMap(m => m.categories);
    return React.createElement('div', { style:{ opacity:1 } },
      React.createElement('div', { className:'page-header' },
        React.createElement('div', { style:{ display:'flex', alignItems:'center', gap:12 } },
          React.createElement('button', { className:'icon-btn', onClick:()=>setActiveReport(null) },
            React.createElement(Icon, { name:'chevronLeft', size:18 })
          ),
          React.createElement('h1', null, 'Evolução por Categoria — 2026')
        ),
        React.createElement('div', { className:'page-header-actions' },
          React.createElement(Button, { variant:'secondary', icon:'download', size:'sm' }, 'Exportar')
        )
      ),

      // Chart card
      React.createElement(Card, { style:{ marginBottom:16 } },
        React.createElement('div', { style:{ height:240 } },
          React.createElement(CategoryYearChart, { activeCats })
        )
      ),

      // Hierarchical table
      React.createElement(Card, { style:{ padding:0, overflow:'hidden' } },
        React.createElement('div', { style:{ padding:'14px 16px 12px', borderBottom:'1px solid var(--border)', display:'flex', alignItems:'center', justifyContent:'space-between' } },
          React.createElement('h3', { style:{ fontSize:14, fontWeight:700 } }, 'Detalhamento por Categoria'),
          React.createElement('div', { style:{ display:'flex', gap:8 } },
            React.createElement('button', {
              onClick:()=>setOpenMacros({despesas:true,receitas:true}),
              style:{ fontSize:12, color:'var(--accent)', background:'none', border:'none', cursor:'pointer', fontWeight:600 }
            }, 'Expandir tudo'),
            React.createElement('button', {
              onClick:()=>{ setOpenMacros({despesas:false,receitas:false}); setOpenCats({}); },
              style:{ fontSize:12, color:'var(--text-muted)', background:'none', border:'none', cursor:'pointer', fontWeight:600 }
            }, 'Recolher tudo')
          )
        ),
        React.createElement(HierarchyTable, { openMacros, toggleMacro, openCats, toggleCat, activeCats, toggleActiveCat })
      )
    );
  }

  return React.createElement('div', { style:{ opacity:1 } },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Relatórios'),
      React.createElement('div', { style:{ position:'relative' } },
        React.createElement('div', { style:{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)', color:'var(--text-muted)' } },
          React.createElement(Icon, { name:'search', size:15 })
        ),
        React.createElement('input', { placeholder:'Pesquisar relatório...', style:{ paddingLeft:32, width:220 } })
      )
    ),
    React.createElement('div', { style:{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:14 } },
      REPORT_CARDS.map(r =>
        React.createElement('div', {
          key:r.id, onClick:()=>r.id==='cat-evo'&&setActiveReport('cat-evo'),
          style:{
            background:'var(--bg-card)', border:'1px solid var(--border)',
            borderRadius:'var(--radius)', padding:'18px',
            cursor:'pointer', transition:'all var(--transition)',
            display:'flex', flexDirection:'column', gap:10,
          },
          onMouseEnter:e=>{ e.currentTarget.style.borderColor=COLOR_MAP[r.color]; e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 8px 24px rgba(0,0,0,0.15)'; },
          onMouseLeave:e=>{ e.currentTarget.style.borderColor='var(--border)'; e.currentTarget.style.transform='none'; e.currentTarget.style.boxShadow='none'; }
        },
          React.createElement('div', {
            style:{ width:40, height:40, borderRadius:10, background:COLOR_MAP[r.color]+'18', display:'flex', alignItems:'center', justifyContent:'center' }
          }, React.createElement(Icon, { name:r.icon, size:20, color:COLOR_MAP[r.color] })),
          React.createElement('div', null,
            React.createElement('p', { style:{ fontSize:13, fontWeight:700, marginBottom:4 } }, r.title),
            React.createElement('p', { style:{ fontSize:12, color:'var(--text-muted)', lineHeight:1.4 } }, r.desc)
          ),
          r.id==='cat-evo' && React.createElement(Badge, { color:'income' }, '✦ Destaque')
        )
      )
    )
  );
};
