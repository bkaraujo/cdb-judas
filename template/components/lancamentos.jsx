// lancamentos.jsx — Transaction list with filters and add modal
const { useState: useLancState } = React;

const CATS = ['Alimentação','Moradia','Transporte','Saúde','Lazer','Educação','Investimentos','Serviços','Vestuário','Outros'];
const CONTAS = ['Conta Corrente Itaú','Nubank','XP Investimentos'];

const SAMPLE_TX = [
  { id:1,  desc:'Salário',           cat:'Receita',      conta:'Conta Corrente Itaú', date:'2026-04-05', amount:8500,   type:'income',  status:'confirmed' },
  { id:2,  desc:'Aluguel',           cat:'Moradia',      conta:'Conta Corrente Itaú', date:'2026-04-05', amount:-2500,  type:'expense', status:'confirmed' },
  { id:3,  desc:'Mercado Extra',     cat:'Alimentação',  conta:'Nubank',              date:'2026-04-06', amount:-245.6, type:'expense', status:'confirmed' },
  { id:4,  desc:'Posto Shell',       cat:'Transporte',   conta:'Nubank',              date:'2026-04-08', amount:-187.4, type:'expense', status:'confirmed' },
  { id:5,  desc:'Netflix',           cat:'Lazer',        conta:'Nubank',              date:'2026-04-10', amount:-55.9,  type:'expense', status:'confirmed' },
  { id:6,  desc:'Farmácia São João', cat:'Saúde',        conta:'Nubank',              date:'2026-04-12', amount:-89.5,  type:'expense', status:'confirmed' },
  { id:7,  desc:'Academia Smart',    cat:'Saúde',        conta:'Conta Corrente Itaú', date:'2026-04-14', amount:-120,   type:'expense', status:'confirmed' },
  { id:8,  desc:'Spotify',           cat:'Lazer',        conta:'Nubank',              date:'2026-04-15', amount:-21.9,  type:'expense', status:'confirmed' },
  { id:9,  desc:'Freela Design',     cat:'Receita',      conta:'Conta Corrente Itaú', date:'2026-04-18', amount:1200,   type:'income',  status:'confirmed' },
  { id:10, desc:'Restaurante Bom',   cat:'Alimentação',  conta:'Nubank',              date:'2026-04-19', amount:-98.5,  type:'expense', status:'confirmed' },
  { id:11, desc:'Transferência',     cat:'Transferência',conta:'Conta Corrente Itaú', date:'2026-04-20', amount:-500,   type:'transfer',status:'confirmed' },
  { id:12, desc:'Conta de Água',     cat:'Moradia',      conta:'Conta Corrente Itaú', date:'2026-04-21', amount:-78.3,  type:'expense', status:'confirmed' },
  { id:13, desc:'Energia Enel',      cat:'Moradia',      conta:'Conta Corrente Itaú', date:'2026-04-22', amount:-342.1, type:'expense', status:'pending'   },
  { id:14, desc:'Uber',              cat:'Transporte',   conta:'Nubank',              date:'2026-04-23', amount:-34.5,  type:'expense', status:'confirmed' },
  { id:15, desc:'iFood',             cat:'Alimentação',  conta:'Nubank',              date:'2026-04-24', amount:-67.8,  type:'expense', status:'confirmed' },
];

const STATUS_LABEL = { confirmed:'Confirmado', pending:'Pendente', scheduled:'Agendado' };
const STATUS_COLOR = { confirmed:'income', pending:'warning', scheduled:'info' };

function AddModal({ open, onClose, onAdd }) {
  const [form, setForm] = useLancState({ type:'expense', desc:'', amount:'', cat:'Alimentação', conta:'Nubank', date: new Date().toISOString().slice(0,10), status:'confirmed' });
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = () => {
    if (!form.desc || !form.amount) return;
    onAdd({ ...form, id: Date.now(), amount: form.type === 'expense' ? -Math.abs(parseFloat(form.amount)) : Math.abs(parseFloat(form.amount)) });
    onClose();
  };

  const TypeBtn = ({ val, label, color }) =>
    React.createElement('button', {
      onClick: () => set('type', val),
      style: {
        flex:1, padding:'8px', borderRadius:'var(--radius-sm)', fontSize:13, fontWeight:600,
        border: `1px solid ${form.type === val ? `var(--${color})` : 'var(--border)'}`,
        background: form.type === val ? `var(--${color}-light)` : 'transparent',
        color: form.type === val ? `var(--${color})` : 'var(--text-secondary)',
        cursor:'pointer', transition:'all var(--transition)',
      }
    }, label);

  return React.createElement(Modal, {
    open, onClose, title: 'Novo Lançamento',
    footer: React.createElement(React.Fragment, null,
      React.createElement(Button, { variant:'secondary', onClick: onClose }, 'Cancelar'),
      React.createElement(Button, { onClick: handleSubmit }, 'Salvar')
    )
  },
    React.createElement('div', { style:{ display:'flex', gap:8, marginBottom:16 } },
      React.createElement(TypeBtn, { val:'expense', label:'↓ Despesa', color:'expense' }),
      React.createElement(TypeBtn, { val:'income', label:'↑ Receita', color:'income' }),
      React.createElement(TypeBtn, { val:'transfer', label:'⇄ Transferência', color:'accent' })
    ),
    React.createElement('div', { className:'form-grid' },
      React.createElement('div', { className:'form-group full' },
        React.createElement('label', { className:'form-label' }, 'Descrição'),
        React.createElement('input', { value:form.desc, onChange:e=>set('desc',e.target.value), placeholder:'Ex: Mercado, Salário...' })
      ),
      React.createElement('div', { className:'form-group' },
        React.createElement('label', { className:'form-label' }, 'Valor (R$)'),
        React.createElement('input', { type:'number', value:form.amount, onChange:e=>set('amount',e.target.value), placeholder:'0,00', min:0, step:'0.01' })
      ),
      React.createElement('div', { className:'form-group' },
        React.createElement('label', { className:'form-label' }, 'Data'),
        React.createElement('input', { type:'date', value:form.date, onChange:e=>set('date',e.target.value) })
      ),
      React.createElement('div', { className:'form-group' },
        React.createElement('label', { className:'form-label' }, 'Categoria'),
        React.createElement('select', { value:form.cat, onChange:e=>set('cat',e.target.value) },
          CATS.map(c => React.createElement('option', { key:c, value:c }, c))
        )
      ),
      React.createElement('div', { className:'form-group' },
        React.createElement('label', { className:'form-label' }, 'Conta'),
        React.createElement('select', { value:form.conta, onChange:e=>set('conta',e.target.value) },
          CONTAS.map(c => React.createElement('option', { key:c, value:c }, c))
        )
      ),
      React.createElement('div', { className:'form-group full' },
        React.createElement('label', { className:'form-label' }, 'Status'),
        React.createElement('select', { value:form.status, onChange:e=>set('status',e.target.value) },
          React.createElement('option', { value:'confirmed' }, 'Confirmado'),
          React.createElement('option', { value:'pending' }, 'Pendente'),
          React.createElement('option', { value:'scheduled' }, 'Agendado')
        )
      )
    )
  );
}

window.LancamentosScreen = function LancamentosScreen() {
  const [txs, setTxs] = useLancState(SAMPLE_TX);
  const [search, setSearch] = useLancState('');
  const [filterType, setFilterType] = useLancState('all');
  const [addOpen, setAddOpen] = useLancState(false);

  const filtered = txs.filter(t => {
    const matchSearch = t.desc.toLowerCase().includes(search.toLowerCase()) || t.cat.toLowerCase().includes(search.toLowerCase());
    const matchType = filterType === 'all' || t.type === filterType;
    return matchSearch && matchType;
  });

  const totalIncome = filtered.filter(t => t.type === 'income').reduce((s,t) => s+t.amount, 0);
  const totalExpense = filtered.filter(t => t.type === 'expense').reduce((s,t) => s+Math.abs(t.amount), 0);

  const FilterBtn = ({ val, label }) =>
    React.createElement('button', {
      onClick: () => setFilterType(val),
      style: {
        padding:'6px 14px', borderRadius:'var(--radius-sm)', fontSize:13, fontWeight:500,
        border:'1px solid var(--border)',
        background: filterType === val ? 'var(--accent)' : 'transparent',
        color: filterType === val ? '#fff' : 'var(--text-secondary)',
        cursor:'pointer', transition:'all var(--transition)',
      }
    }, label);

  return React.createElement('div', { className:'fade-in' },
    React.createElement('div', { className:'page-header' },
      React.createElement('h1', null, 'Lançamentos'),
      React.createElement('div', { className:'page-header-actions' },
        React.createElement(Button, { icon:'download', variant:'secondary', size:'sm' }, 'Exportar'),
        React.createElement(Button, { icon:'plus', onClick:()=>setAddOpen(true) }, 'Novo Lançamento')
      )
    ),

    // Summary strip
    React.createElement('div', { style:{ display:'flex', gap:12, marginBottom:18 } },
      React.createElement(Card, { style:{ flex:1, padding:'14px 18px', display:'flex', alignItems:'center', gap:12 } },
        React.createElement(Icon, { name:'arrowUp', size:20, color:'var(--income)' }),
        React.createElement('div', null,
          React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600} }, 'RECEITAS'),
          React.createElement('p', { style:{fontSize:18, fontWeight:800, color:'var(--income)'} }, fmt(totalIncome))
        )
      ),
      React.createElement(Card, { style:{ flex:1, padding:'14px 18px', display:'flex', alignItems:'center', gap:12 } },
        React.createElement(Icon, { name:'arrowDown', size:20, color:'var(--expense)' }),
        React.createElement('div', null,
          React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600} }, 'DESPESAS'),
          React.createElement('p', { style:{fontSize:18, fontWeight:800, color:'var(--expense)'} }, fmt(totalExpense))
        )
      ),
      React.createElement(Card, { style:{ flex:1, padding:'14px 18px', display:'flex', alignItems:'center', gap:12 } },
        React.createElement(Icon, { name:'trendingUp', size:20, color: totalIncome-totalExpense >= 0 ? 'var(--income)' : 'var(--expense)' }),
        React.createElement('div', null,
          React.createElement('p', { style:{fontSize:11, color:'var(--text-muted)', fontWeight:600} }, 'RESULTADO'),
          React.createElement('p', { style:{fontSize:18, fontWeight:800, color: totalIncome-totalExpense >= 0 ? 'var(--income)' : 'var(--expense)'} }, fmt(totalIncome-totalExpense))
        )
      )
    ),

    // Filters row
    React.createElement('div', { style:{ display:'flex', gap:10, alignItems:'center', marginBottom:14 } },
      React.createElement('div', { style:{ position:'relative', flex:1, maxWidth:320 } },
        React.createElement('div', { style:{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)', color:'var(--text-muted)' } },
          React.createElement(Icon, { name:'search', size:15 })
        ),
        React.createElement('input', {
          value:search, onChange:e=>setSearch(e.target.value),
          placeholder:'Pesquisar lançamentos...',
          style:{ paddingLeft:32 }
        })
      ),
      React.createElement(FilterBtn, { val:'all', label:'Todos' }),
      React.createElement(FilterBtn, { val:'income', label:'Receitas' }),
      React.createElement(FilterBtn, { val:'expense', label:'Despesas' }),
      React.createElement(FilterBtn, { val:'transfer', label:'Transferências' })
    ),

    // Table
    React.createElement(Card, { style:{ padding:0, overflow:'hidden' } },
      React.createElement('table', { style:{ width:'100%', borderCollapse:'collapse' } },
        React.createElement('thead', null,
          React.createElement('tr', { style:{ borderBottom:'1px solid var(--border)' } },
            ['Data','Descrição','Categoria','Conta','Status','Valor',''].map(h =>
              React.createElement('th', {
                key:h, style:{
                  padding:'12px 16px', textAlign: h === 'Valor' ? 'right' : 'left',
                  fontSize:11, fontWeight:700, color:'var(--text-muted)',
                  textTransform:'uppercase', letterSpacing:'0.05em'
                }
              }, h)
            )
          )
        ),
        React.createElement('tbody', null,
          filtered.map((tx, i) =>
            React.createElement('tr', {
              key:tx.id,
              style:{ borderBottom: i < filtered.length-1 ? '1px solid var(--border-light)' : 'none', transition:'background var(--transition)' },
              onMouseEnter: e => e.currentTarget.style.background = 'var(--bg-hover)',
              onMouseLeave: e => e.currentTarget.style.background = 'transparent'
            },
              React.createElement('td', { style:{padding:'11px 16px', fontSize:12, color:'var(--text-muted)'} }, fmtDate(tx.date)),
              React.createElement('td', { style:{padding:'11px 16px', fontSize:13, fontWeight:500} }, tx.desc),
              React.createElement('td', { style:{padding:'11px 16px'} },
                React.createElement(Badge, { color:'muted' }, tx.cat)
              ),
              React.createElement('td', { style:{padding:'11px 16px', fontSize:12, color:'var(--text-secondary)'} }, tx.conta),
              React.createElement('td', { style:{padding:'11px 16px'} },
                React.createElement(Badge, { color: STATUS_COLOR[tx.status] }, STATUS_LABEL[tx.status])
              ),
              React.createElement('td', {
                style:{
                  padding:'11px 16px', textAlign:'right', fontSize:13, fontWeight:700,
                  color: tx.amount > 0 ? 'var(--income)' : tx.type === 'transfer' ? 'var(--text-secondary)' : 'var(--expense)'
                }
              }, (tx.amount > 0 ? '+' : '') + fmt(tx.amount)),
              React.createElement('td', { style:{padding:'11px 12px'} },
                React.createElement('button', { className:'icon-btn', style:{width:28, height:28} },
                  React.createElement(Icon, { name:'moreVertical', size:14 })
                )
              )
            )
          )
        )
      )
    ),

    React.createElement(AddModal, { open:addOpen, onClose:()=>setAddOpen(false), onAdd: tx => setTxs(p=>[tx,...p]) })
  );
};
