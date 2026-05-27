// screens.jsx — Contas a Pagar, Extrato, Categorias, Cartões, Contas, Centros, Tags
const { useState: useScState } = React;

// ── Contas a Pagar e Receber ─────────────────────────────────
const PAGAR = [
  { id:1, name:'Aluguel',           due:'2026-05-02', amount:2500,   cat:'Moradia',    status:'pending'   },
  { id:2, name:'Energia Enel',      due:'2026-05-05', amount:342.1,  cat:'Moradia',    status:'pending'   },
  { id:3, name:'Internet Vivo',     due:'2026-05-08', amount:119.9,  cat:'Serviços',   status:'pending'   },
  { id:4, name:'Gympass',           due:'2026-05-10', amount:99.9,   cat:'Saúde',      status:'pending'   },
  { id:5, name:'Nubank Fatura',     due:'2026-05-15', amount:2345.8, cat:'Cartão',     status:'pending'   },
  { id:6, name:'Seguro Auto',       due:'2026-05-20', amount:380,    cat:'Transporte', status:'scheduled' },
  { id:7, name:'IPTU Parcela',      due:'2026-05-28', amount:210.5,  cat:'Moradia',    status:'scheduled' },
];
const RECEBER = [
  { id:1, name:'Salário',           due:'2026-05-05', amount:8500,   cat:'Receita',    status:'scheduled' },
  { id:2, name:'Freela Design',     due:'2026-05-12', amount:1200,   cat:'Receita',    status:'pending'   },
  { id:3, name:'Aluguel Imóvel',    due:'2026-05-10', amount:1800,   cat:'Receita',    status:'scheduled' },
];

window.ContasPagarScreen = function ContasPagarScreen() {
  const [tab, setTab] = useScState('pagar');
  const list = tab === 'pagar' ? PAGAR : RECEBER;
  const total = list.reduce((s,i) => s + i.amount, 0);

  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'A Pagar e Receber'),
      React.createElement('div', { className:'page-header-actions' },
        React.createElement(PeriodNav, { label:'Maio 2026', onPrev:()=>{}, onNext:()=>{} }),
        React.createElement(Button, { icon:'plus' }, 'Nova Conta')
      )
    ),
    React.createElement(Card, { style:{ padding:'16px 20px', marginBottom:16, display:'flex', gap:32 } },
      React.createElement('div', null,
        React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'A Pagar'),
        React.createElement('p', { style:{fontSize:20, fontWeight:800, color:'var(--expense)', marginTop:4} }, fmt(PAGAR.reduce((s,i)=>s+i.amount,0)))
      ),
      React.createElement('div', { style:{width:1, background:'var(--border)'} }),
      React.createElement('div', null,
        React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'A Receber'),
        React.createElement('p', { style:{fontSize:20, fontWeight:800, color:'var(--income)', marginTop:4} }, fmt(RECEBER.reduce((s,i)=>s+i.amount,0)))
      ),
      React.createElement('div', { style:{width:1, background:'var(--border)'} }),
      React.createElement('div', null,
        React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'Resultado'),
        React.createElement('p', { style:{fontSize:20, fontWeight:800, color:'var(--income)', marginTop:4} }, fmt(RECEBER.reduce((s,i)=>s+i.amount,0)-PAGAR.reduce((s,i)=>s+i.amount,0)))
      )
    ),
    React.createElement('div', { className:'tabs' },
      React.createElement('button', { className:`tab ${tab==='pagar'?'active':''}`, onClick:()=>setTab('pagar') }, `A pagar (${PAGAR.length})`),
      React.createElement('button', { className:`tab ${tab==='receber'?'active':''}`, onClick:()=>setTab('receber') }, `A receber (${RECEBER.length})`)
    ),
    React.createElement(Card, { style:{padding:0, overflow:'hidden'} },
      list.map((item, i) =>
        React.createElement('div', {
          key:item.id,
          style:{
            display:'flex', alignItems:'center', justifyContent:'space-between',
            padding:'14px 20px',
            borderBottom: i<list.length-1 ? '1px solid var(--border-light)' : 'none',
            transition:'background var(--transition)',
          },
          onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
          onMouseLeave:e=>e.currentTarget.style.background='transparent'
        },
          React.createElement('div', { style:{display:'flex', alignItems:'center', gap:14} },
            React.createElement('div', {
              style:{
                width:10, height:10, borderRadius:'50%',
                background: item.status==='pending' ? 'var(--expense)' : 'var(--warning)',
                flexShrink:0,
              }
            }),
            React.createElement('div', null,
              React.createElement('p', { style:{fontSize:13, fontWeight:600} }, item.name),
              React.createElement('p', { style:{fontSize:12, color:'var(--text-muted)', marginTop:2} }, item.cat)
            )
          ),
          React.createElement('div', { style:{display:'flex', alignItems:'center', gap:24} },
            React.createElement('span', { style:{fontSize:12, color:'var(--text-muted)'} }, fmtDate(item.due)),
            React.createElement(Badge, { color: item.status==='pending'?'expense':'warning' }, item.status==='pending'?'Pendente':'Agendado'),
            React.createElement('span', { style:{fontSize:14, fontWeight:700, color: tab==='pagar'?'var(--expense)':'var(--income)', minWidth:90, textAlign:'right'} },
              (tab==='receber'?'+':'-') + fmt(item.amount)
            ),
            React.createElement('button', { className:'icon-btn', style:{width:28,height:28} },
              React.createElement(Icon, { name:'moreVertical', size:14 })
            )
          )
        )
      )
    )
  );
};

// ── Extrato de Contas ──────────────────────────────────────
const EXTRATO_TX = [
  { date:'2026-04-24', desc:'iFood',               amount:-67.8,  status:'confirmed' },
  { date:'2026-04-23', desc:'Mercado Extra',        amount:-245.6, status:'confirmed' },
  { date:'2026-04-22', desc:'Transferência',        amount:-500,   status:'confirmed' },
  { date:'2026-04-22', desc:'Posto Shell',          amount:-187.4, status:'confirmed' },
  { date:'2026-04-21', desc:'Farmácia São João',    amount:-89.5,  status:'confirmed' },
  { date:'2026-04-21', desc:'Conta de Água',        amount:-78.3,  status:'confirmed' },
  { date:'2026-04-20', desc:'Freela Design',        amount:+1200,  status:'confirmed' },
  { date:'2026-04-18', desc:'Spotify',              amount:-21.9,  status:'confirmed' },
  { date:'2026-04-15', desc:'Academia Smart',       amount:-120,   status:'confirmed' },
  { date:'2026-04-10', desc:'Netflix',              amount:-55.9,  status:'confirmed' },
  { date:'2026-04-08', desc:'Posto Shell',          amount:-134.5, status:'confirmed' },
  { date:'2026-04-05', desc:'Salário',              amount:+8500,  status:'confirmed' },
  { date:'2026-04-05', desc:'Aluguel',              amount:-2500,  status:'confirmed' },
  { date:'2026-04-01', desc:'Saldo anterior',       amount:0,      status:'balance',  balance: 4820.4 },
];

window.ExtratoScreen = function ExtratoScreen() {
  const [conta, setConta] = useScState('Conta Corrente Itaú');
  let running = 4820.4;
  const txWithBal = EXTRATO_TX.map(t => {
    if (t.status === 'balance') return { ...t, runningBal: t.balance };
    running += t.amount;
    return { ...t, runningBal: running };
  }).reverse();

  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Extrato de Contas'),
      React.createElement(PeriodNav, { label:'Abril 2026', onPrev:()=>{}, onNext:()=>{} })
    ),
    React.createElement('div', { style:{display:'grid', gridTemplateColumns:'280px 1fr', gap:16} },
      React.createElement('div', { style:{display:'flex', flexDirection:'column', gap:12} },
        ['Conta Corrente Itaú','Nubank','XP Investimentos'].map(c =>
          React.createElement('button', {
            key:c,
            onClick:()=>setConta(c),
            style:{
              padding:'14px 16px', borderRadius:'var(--radius)', textAlign:'left',
              background: conta===c ? 'var(--accent-light)' : 'var(--bg-card)',
              border:`1px solid ${conta===c?'var(--accent)':'var(--border)'}`,
              color: conta===c ? 'var(--accent)' : 'var(--text-primary)',
              cursor:'pointer', fontWeight: conta===c ? 700 : 400, fontSize:13,
              transition:'all var(--transition)',
            }
          },
            React.createElement('p', null, c),
            React.createElement('p', { style:{fontSize:11, color: conta===c?'var(--accent)':'var(--text-muted)', marginTop:4} },
              c==='Conta Corrente Itaú'?'R$ 12.450,00':c==='Nubank'?'R$ 5.430,00':'R$ 18.920,00'
            )
          )
        )
      ),
      React.createElement(Card, { style:{padding:0, overflow:'hidden'} },
        txWithBal.map((tx, i) =>
          React.createElement('div', {
            key:i,
            style:{
              display:'flex', alignItems:'center', gap:16, padding:'11px 20px',
              borderBottom: i<txWithBal.length-1?'1px solid var(--border-light)':'none',
              background: tx.status==='balance' ? 'var(--bg-hover)' : 'transparent',
              transition:'background var(--transition)',
            },
            onMouseEnter:e=>{ if(tx.status!=='balance') e.currentTarget.style.background='var(--bg-hover)'; },
            onMouseLeave:e=>{ if(tx.status!=='balance') e.currentTarget.style.background='transparent'; }
          },
            React.createElement('span', { style:{fontSize:12, color:'var(--text-muted)', minWidth:56} }, fmtDate(tx.date)),
            React.createElement('span', {
              style:{
                flex:1, fontSize:13, fontWeight: tx.status==='balance'?700:500,
                color: tx.status==='balance'?'var(--text-secondary)':'var(--text-primary)'
              }
            }, tx.desc),
            tx.amount !== 0 && React.createElement('span', {
              style:{fontSize:13, fontWeight:700, color: tx.amount>0?'var(--income)':'var(--expense)'}
            }, (tx.amount>0?'+':'') + fmt(tx.amount)),
            React.createElement('span', {
              style:{fontSize:13, fontWeight:700, color:'var(--text-secondary)', minWidth:100, textAlign:'right'}
            }, fmt(tx.runningBal)),
            React.createElement('div', { style:{
              width:8, height:8, borderRadius:'50%', flexShrink:0,
              background: tx.status==='confirmed'?'var(--income)':tx.status==='balance'?'var(--text-muted)':'var(--warning)'
            }})
          )
        )
      )
    )
  );
};

// ── Categorias ─────────────────────────────────────────────
const CATS_DATA = [
  { name:'Alimentação', sub:[], type:'expense', count:8 },
  { name:'Moradia', sub:['Aluguel','Condomínio','Água','Energia'], type:'expense', count:4 },
  { name:'Transporte', sub:['Combustível','Uber/99','Estacionamento'], type:'expense', count:3 },
  { name:'Saúde', sub:['Farmácia','Plano de Saúde','Consultas'], type:'expense', count:3 },
  { name:'Lazer', sub:['Streaming','Restaurantes','Cinema'], type:'expense', count:5 },
  { name:'Investimentos', sub:['Renda Fixa','Renda Variável','Criptomoedas'], type:'expense', count:3 },
  { name:'Serviços', sub:['Internet','Telefone','Assinaturas'], type:'expense', count:6 },
  { name:'Educação', sub:[], type:'expense', count:2 },
  { name:'Salário', sub:[], type:'income', count:4 },
  { name:'Freelance', sub:[], type:'income', count:3 },
  { name:'Aluguel recebido', sub:[], type:'income', count:4 },
  { name:'Dividendos', sub:[], type:'income', count:6 },
];

window.CategoriasScreen = function CategoriasScreen() {
  const [tab, setTab] = useScState('expense');
  const [expanded, setExpanded] = useScState({});
  const cats = CATS_DATA.filter(c => c.type === tab);

  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Categorias'),
      React.createElement(Button, { icon:'plus' }, 'Nova Categoria')
    ),
    React.createElement('div', { style:{display:'grid', gridTemplateColumns:'240px 1fr', gap:16} },
      React.createElement(Card, { style:{padding:'16px'} },
        React.createElement('div', {
          style:{display:'flex', flexDirection:'column', gap:4, cursor:'pointer'}
        },
          React.createElement('button', {
            onClick:()=>setTab('expense'),
            style:{
              display:'flex', alignItems:'center', justifyContent:'space-between',
              padding:'10px 12px', borderRadius:'var(--radius-sm)',
              background: tab==='expense'?'var(--expense-light)':'transparent',
              color: tab==='expense'?'var(--expense)':'var(--text-secondary)',
              border:'none', cursor:'pointer', fontWeight:600, fontSize:13,
              transition:'all var(--transition)',
            }
          },
            React.createElement('span', { style:{display:'flex', alignItems:'center', gap:8} },
              React.createElement(Icon, { name:'arrowDown', size:15 }), 'Despesas'
            ),
            React.createElement(Badge, { color:'expense' }, CATS_DATA.filter(c=>c.type==='expense').length)
          ),
          React.createElement('button', {
            onClick:()=>setTab('income'),
            style:{
              display:'flex', alignItems:'center', justifyContent:'space-between',
              padding:'10px 12px', borderRadius:'var(--radius-sm)',
              background: tab==='income'?'var(--income-light)':'transparent',
              color: tab==='income'?'var(--income)':'var(--text-secondary)',
              border:'none', cursor:'pointer', fontWeight:600, fontSize:13,
              transition:'all var(--transition)',
            }
          },
            React.createElement('span', { style:{display:'flex', alignItems:'center', gap:8} },
              React.createElement(Icon, { name:'arrowUp', size:15 }), 'Receitas'
            ),
            React.createElement(Badge, { color:'income' }, CATS_DATA.filter(c=>c.type==='income').length)
          )
        )
      ),
      React.createElement(Card, { style:{padding:0, overflow:'hidden'} },
        cats.map((cat, i) =>
          React.createElement('div', { key:cat.name },
            React.createElement('div', {
              style:{
                display:'flex', alignItems:'center', justifyContent:'space-between',
                padding:'13px 20px',
                cursor: cat.sub.length>0?'pointer':'default',
                transition:'background var(--transition)',
              },
              onClick:()=>cat.sub.length>0 && setExpanded(p=>({...p,[cat.name]:!p[cat.name]})),
              onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
              onMouseLeave:e=>e.currentTarget.style.background='transparent'
            },
              React.createElement('div', { style:{display:'flex', alignItems:'center', gap:10} },
                cat.sub.length>0 && React.createElement(Icon, { name:expanded[cat.name]?'chevronDown':'chevronRight', size:14, color:'var(--text-muted)' }),
                React.createElement('span', { style:{fontSize:13, fontWeight:600} }, cat.name),
                cat.sub.length>0 && React.createElement(Badge, { color:'muted' }, cat.sub.length)
              ),
              React.createElement('button', { className:'icon-btn', style:{width:28,height:28} },
                React.createElement(Icon, { name:'moreVertical', size:14 })
              )
            ),
            expanded[cat.name] && cat.sub.map(sub =>
              React.createElement('div', {
                key:sub,
                style:{
                  display:'flex', alignItems:'center', justifyContent:'space-between',
                  padding:'10px 20px 10px 44px',
                  transition:'background var(--transition)',
                },
                onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
                onMouseLeave:e=>e.currentTarget.style.background='transparent'
              },
                React.createElement('span', { style:{fontSize:12, color:'var(--text-secondary)'} }, '↳ ' + sub),
                React.createElement('button', { className:'icon-btn', style:{width:28,height:28} },
                  React.createElement(Icon, { name:'moreVertical', size:14 })
                )
              )
            )
          )
        )
      )
    )
  );
};

// ── Cartões de Crédito ─────────────────────────────────────
const CARDS_DATA = [
  { id:1, name:'Nubank Mastercard', limit:8000, used:2345.8, color:'#820AD1', last4:'4389', due:15 },
  { id:2, name:'XP Visa Infinite',  limit:15000,used:1890.4, color:'#1C2951', last4:'7721', due:10 },
];

window.CartoesScreen = function CartoesScreen() {
  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Cartões de Crédito'),
      React.createElement(Button, { icon:'plus' }, 'Novo Cartão')
    ),
    React.createElement('div', { style:{display:'grid', gridTemplateColumns:'repeat(2, 1fr)', gap:16} },
      CARDS_DATA.map(card => {
        const pct = (card.used/card.limit)*100;
        return React.createElement(Card, { key:card.id },
          React.createElement('div', {
            style:{
              background:`linear-gradient(135deg, ${card.color}, ${card.color}99)`,
              borderRadius:'var(--radius)', padding:'20px', marginBottom:20,
              position:'relative', overflow:'hidden', minHeight:140,
            }
          },
            React.createElement('div', {
              style:{
                position:'absolute', right:-30, top:-30, width:160, height:160,
                borderRadius:'50%', background:'rgba(255,255,255,0.06)'
              }
            }),
            React.createElement('p', { style:{color:'rgba(255,255,255,0.7)', fontSize:11, fontWeight:600, letterSpacing:'0.1em'} }, 'CRÉDITO'),
            React.createElement('p', { style:{color:'#fff', fontSize:18, fontWeight:800, marginTop:24} }, card.name),
            React.createElement('p', { style:{color:'rgba(255,255,255,0.6)', fontSize:13, marginTop:4} }, `•••• •••• •••• ${card.last4}`),
            React.createElement('p', { style:{color:'rgba(255,255,255,0.5)', fontSize:11, position:'absolute', bottom:16, right:16} }, `Vence dia ${card.due}`)
          ),
          React.createElement('div', { style:{display:'flex', justifyContent:'space-between', marginBottom:10} },
            React.createElement('div', null,
              React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'Fatura Atual'),
              React.createElement('p', { style:{fontSize:20, fontWeight:800, color:'var(--expense)'} }, fmt(card.used))
            ),
            React.createElement('div', { style:{textAlign:'right'} },
              React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'Limite Total'),
              React.createElement('p', { style:{fontSize:20, fontWeight:800} }, fmt(card.limit))
            )
          ),
          React.createElement('div', { style:{height:8, background:'var(--bg-hover)', borderRadius:4, overflow:'hidden', marginBottom:6} },
            React.createElement('div', { style:{height:'100%', borderRadius:4, width:`${pct}%`, background: pct>80?'var(--expense)':pct>60?'var(--warning)':'var(--accent)'} })
          ),
          React.createElement('div', { style:{display:'flex', justifyContent:'space-between', fontSize:12, color:'var(--text-muted)'} },
            React.createElement('span', null, `${pct.toFixed(0)}% utilizado`),
            React.createElement('span', null, `${fmt(card.limit-card.used)} disponível`)
          )
        );
      })
    )
  );
};

// ── Contas Bancárias ───────────────────────────────────────
const CONTAS_DATA = [
  { id:1, name:'Conta Corrente',  bank:'Itaú Unibanco', balance:12450, type:'checking', active:true },
  { id:2, name:'Conta Poupança',  bank:'Banco do Brasil', balance:18920, type:'savings', active:true },
  { id:3, name:'Conta Digital',   bank:'Nubank',          balance:5430,  type:'checking', active:true },
];

window.ContasScreen = function ContasScreen() {
  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Contas Bancárias'),
      React.createElement(Button, { icon:'plus' }, 'Nova Conta')
    ),
    React.createElement(Card, { style:{marginBottom:16, padding:'16px 20px', display:'flex', alignItems:'center', justifyContent:'space-between'} },
      React.createElement('div', null,
        React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600, textTransform:'uppercase'} }, 'Saldo Total'),
        React.createElement('p', { style:{fontSize:24, fontWeight:800, marginTop:4} }, fmt(CONTAS_DATA.reduce((s,c)=>s+c.balance,0)))
      ),
      React.createElement(Badge, { color:'income' }, `${CONTAS_DATA.length} contas ativas`)
    ),
    React.createElement(Card, { style:{padding:0, overflow:'hidden'} },
      CONTAS_DATA.map((conta, i) =>
        React.createElement('div', {
          key:conta.id,
          style:{
            display:'flex', alignItems:'center', gap:16, padding:'16px 20px',
            transition:'background var(--transition)',
          },
          onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
          onMouseLeave:e=>e.currentTarget.style.background='transparent'
        },
          React.createElement('div', {
            style:{
              width:44, height:44, borderRadius:12, background:'var(--accent-light)',
              display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0,
            }
          }, React.createElement(Icon, { name:'building', size:20, color:'var(--accent)' })),
          React.createElement('div', { style:{flex:1} },
            React.createElement('p', { style:{fontSize:13, fontWeight:700} }, conta.name),
            React.createElement('p', { style:{fontSize:12, color:'var(--text-muted)', marginTop:2} }, conta.bank)
          ),
          React.createElement(Badge, { color:'income' }, 'Disponível'),
          React.createElement('p', { style:{fontSize:16, fontWeight:800, minWidth:120, textAlign:'right'} }, fmt(conta.balance)),
          React.createElement('button', { className:'icon-btn', style:{width:28,height:28} },
            React.createElement(Icon, { name:'moreVertical', size:14 })
          )
        )
      )
    )
  );
};

// ── Centro de Custo ─────────────────────────────────────────
window.CentroCustoScreen = function CentroCustoScreen() {
  const centros = ['Casa','Veículo','Trabalho','Projetos Pessoais','Investimentos','Família'];
  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Centros de Custo'),
      React.createElement(Button, { icon:'plus' }, 'Novo Centro')
    ),
    React.createElement(Card, { style:{padding:0, overflow:'hidden'} },
      centros.map((c, i) =>
        React.createElement('div', {
          key:c,
          style:{
            display:'flex', alignItems:'center', justifyContent:'space-between',
            padding:'14px 20px', borderBottom:i<centros.length-1?'1px solid var(--border-light)':'none',
            transition:'background var(--transition)',
          },
          onMouseEnter:e=>e.currentTarget.style.background='var(--bg-hover)',
          onMouseLeave:e=>e.currentTarget.style.background='transparent'
        },
          React.createElement('div', { style:{display:'flex', alignItems:'center', gap:12} },
            React.createElement('div', {
              style:{width:36, height:36, borderRadius:8, background:'var(--accent-light)', display:'flex', alignItems:'center', justifyContent:'center'}
            }, React.createElement(Icon, { name:'briefcase', size:16, color:'var(--accent)' })),
            React.createElement('span', { style:{fontSize:13, fontWeight:600} }, c)
          ),
          React.createElement('button', { className:'icon-btn', style:{width:28,height:28} },
            React.createElement(Icon, { name:'moreVertical', size:14 })
          )
        )
      )
    )
  );
};

// ── Tags ───────────────────────────────────────────────────
window.TagsScreen = function TagsScreen() {
  const tags = [
    {name:'mensal', count:24, color:'#6366F1'},
    {name:'fixo', count:8, color:'#F43F5E'},
    {name:'variável', count:15, color:'#F59E0B'},
    {name:'investimento', count:6, color:'#10B981'},
    {name:'lazer', count:11, color:'#38BDF8'},
    {name:'trabalho', count:4, color:'#A78BFA'},
    {name:'urgente', count:2, color:'#FB923C'},
    {name:'parcelado', count:7, color:'#34D399'},
  ];
  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Tags'),
      React.createElement(Button, { icon:'plus' }, 'Nova Tag')
    ),
    React.createElement('div', { style:{display:'flex', flexWrap:'wrap', gap:12} },
      tags.map(tag =>
        React.createElement('div', {
          key:tag.name,
          style:{
            display:'flex', alignItems:'center', gap:10, padding:'10px 16px',
            borderRadius:40, background:'var(--bg-card)', border:'1px solid var(--border)',
            cursor:'pointer', transition:'all var(--transition)',
          },
          onMouseEnter:e=>{e.currentTarget.style.borderColor=tag.color; e.currentTarget.style.background=tag.color+'18';},
          onMouseLeave:e=>{e.currentTarget.style.borderColor='var(--border)'; e.currentTarget.style.background='var(--bg-card)';}
        },
          React.createElement('span', { style:{width:10, height:10, borderRadius:'50%', background:tag.color} }),
          React.createElement('span', { style:{fontSize:13, fontWeight:600} }, '#' + tag.name),
          React.createElement('span', {
            style:{fontSize:11, background:'var(--bg-hover)', padding:'2px 7px', borderRadius:20, color:'var(--text-muted)', fontWeight:600}
          }, tag.count)
        )
      )
    )
  );
};
