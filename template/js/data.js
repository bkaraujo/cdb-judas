/* data.js — Dados mock, formatadores, ícones SVG (HTML5 + jQuery) */
(function (global) {
  'use strict';

  // ── Formatadores ───────────────────────────────────────────
  function fmt(v) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);
  }
  function fmtShort(v) {
    var abs = Math.abs(v);
    if (abs >= 1000) return (v < 0 ? '-' : '') + 'R$ ' + (abs / 1000).toFixed(1).replace('.', ',') + 'k';
    return fmt(v);
  }
  function fmtDate(d) {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(d));
  }

  // ── Ícones SVG (paths) ──────────────────────────────────────
  var ICONS = {
    home:        'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10',
    layers:      'M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5',
    list:        'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
    calendar:    'M3 4h18v18H3zM16 2v4M8 2v4M3 10h18',
    bookOpen:    'M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2zM22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z',
    creditCard:  'M1 4h22v16H1zM1 10h22',
    target:      'M22 12A10 10 0 1112 2M22 12h-4M12 2v4M12 6a6 6 0 100 12M18 12a6 6 0 00-6-6',
    barChart:    'M18 20V10M12 20V4M6 20v-6',
    database:    'M12 2C6.48 2 2 4.24 2 7s4.48 5 10 5 10-2.24 10-5-4.48-5-10-5zM2 7v5c0 2.76 4.48 5 10 5s10-2.24 10-5V7M2 12v5c0 2.76 4.48 5 10 5s10-2.24 10-5v-5',
    tag:         'M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82zM7 7h.01',
    briefcase:   'M16 20H8a2 2 0 01-2-2V8h12v10a2 2 0 01-2 2zM20 8H4a2 2 0 01-2-2V5a2 2 0 012-2h16a2 2 0 012 2v1a2 2 0 01-2 2zM8 3v2M16 3v2',
    building:    'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2zM9 22V12h6v10',
    hash:        'M4 9h16M4 15h16M10 3L8 21M16 3l-2 18',
    plus:        'M12 5v14M5 12h14',
    search:      'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0',
    bell:        'M15 17H5a2 2 0 000 4h14a2 2 0 000-4h-4zM18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9',
    sun:         'M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42M12 17a5 5 0 100-10 5 5 0 000 10z',
    moon:        'M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z',
    chevronRight:'M9 18l6-6-6-6',
    chevronLeft: 'M15 18l-6-6 6-6',
    chevronDown: 'M6 9l6 6 6-6',
    chevronUp:   'M18 15l-6-6-6 6',
    x:           'M18 6L6 18M6 6l12 12',
    edit:        'M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z',
    trash:       'M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2',
    moreVertical:'M12 5h.01M12 12h.01M12 19h.01',
    filter:      'M22 3H2l8 9.46V19l4 2v-8.54L22 3z',
    download:    'M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3',
    arrowUp:     'M12 19V5M5 12l7-7 7 7',
    arrowDown:   'M12 5v14M19 12l-7 7-7-7',
    trendingUp:  'M23 6l-9.5 9.5-5-5L1 18',
    trendingDown:'M23 18l-9.5-9.5-5 5L1 6',
    dollarSign:  'M12 1v22M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6',
    eye:         'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zM12 9a3 3 0 100 6 3 3 0 000-6z',
    eyeOff:      'M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22',
    check:       'M20 6L9 17l-5-5',
    alertCircle: 'M12 22c5.52 0 10-4.48 10-10S17.52 2 12 2 2 6.48 2 12s4.48 10 10 10zM12 8v4M12 16h.01',
    settings:    'M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z',
    menu:        'M3 12h18M3 6h18M3 18h18',
    activity:    'M22 12h-4l-3 9L9 3l-3 9H2',
    pieChart:    'M21.21 15.89A10 10 0 118 2.83M22 12A10 10 0 0012 2v10z'
  };

  // ── Navegação ───────────────────────────────────────────────
  var NAV = [
    { id: 'dashboard', label: 'Visão Geral', icon: 'home' },
    { id: 'movimentacoes', label: 'Movimentações', icon: 'layers', children: [
      { id: 'lancamentos', label: 'Lançamentos', icon: 'list' },
      { id: 'contas-pagar', label: 'A pagar e receber', icon: 'calendar' }
    ]},
    { id: 'extrato', label: 'Extrato de Contas', icon: 'bookOpen' },
    { id: 'cartoes', label: 'Cartões de Crédito', icon: 'creditCard' },
    { id: 'orcamento', label: 'Metas / Orçamento', icon: 'target' },
    { id: 'relatorios', label: 'Relatórios', icon: 'barChart' },
    { id: 'cadastros', label: 'Cadastros', icon: 'database', children: [
      { id: 'categorias', label: 'Categorias', icon: 'tag' },
      { id: 'centros-custo', label: 'Centros de Custo', icon: 'briefcase' },
      { id: 'contas', label: 'Contas', icon: 'building' },
      { id: 'tags', label: 'Tags', icon: 'hash' }
    ]}
  ];

  var SCREEN_TITLES = {
    'dashboard': 'Visão Geral', 'lancamentos': 'Lançamentos',
    'contas-pagar': 'A Pagar e Receber', 'extrato': 'Extrato de Contas',
    'cartoes': 'Cartões de Crédito', 'orcamento': 'Metas / Orçamento',
    'relatorios': 'Relatórios', 'categorias': 'Categorias',
    'centros-custo': 'Centros de Custo', 'contas': 'Contas Bancárias', 'tags': 'Tags'
  };

  // ── Dados: Dashboard ────────────────────────────────────────
  var MONTHLY_DATA = [
    { month: 'Jan', receitas: 8200, despesas: 5100 },
    { month: 'Fev', receitas: 8500, despesas: 4800 },
    { month: 'Mar', receitas: 9200, despesas: 6200 },
    { month: 'Abr', receitas: 8500, despesas: 3900 }
  ];
  var UPCOMING = [
    { id:1, name:'Aluguel',       due:'2026-05-02', amount:2500,   type:'expense' },
    { id:2, name:'Energia',       due:'2026-05-05', amount:342.1,  type:'expense' },
    { id:3, name:'Salário',       due:'2026-05-05', amount:8500,   type:'income'  },
    { id:4, name:'Internet',      due:'2026-05-08', amount:119.9,  type:'expense' },
    { id:5, name:'Gympass',       due:'2026-05-10', amount:99.9,   type:'expense' },
    { id:6, name:'Nubank fatura', due:'2026-05-15', amount:2345.8, type:'expense' }
  ];
  var CONTAS_RECEBER = [
    { id:1, name:'Salário',        due:'2026-05-05', amount:8500 },
    { id:2, name:'Freela Design',  due:'2026-05-12', amount:1200 },
    { id:3, name:'Aluguel imóvel', due:'2026-05-10', amount:1800 }
  ];
  var RECENT = [
    { id:1, desc:'Mercado Extra',      cat:'Alimentação',  date:'2026-04-23', amount:-245.6, icon:'🛒' },
    { id:2, desc:'Transferência Itaú', cat:'Transferência',date:'2026-04-22', amount:1200,   icon:'🏦' },
    { id:3, desc:'Posto Shell',        cat:'Transporte',   date:'2026-04-22', amount:-187.4, icon:'⛽' },
    { id:4, desc:'Netflix',            cat:'Lazer',        date:'2026-04-21', amount:-55.9,  icon:'🎬' },
    { id:5, desc:'Farmácia',           cat:'Saúde',        date:'2026-04-20', amount:-89.5,  icon:'💊' }
  ];
  var EXPENSE_CATS = [
    { name:'Moradia',     amount:2920, color:'#6366F1' },
    { name:'Alimentação', amount:813,  color:'#38BDF8' },
    { name:'Transporte',  amount:222,  color:'#F59E0B' },
    { name:'Saúde',       amount:210,  color:'#10B981' },
    { name:'Lazer',       amount:390,  color:'#F43F5E' },
    { name:'Serviços',    amount:342,  color:'#A78BFA' }
  ];
  var ACCOUNTS = [
    { name:'Conta Corrente Itaú', balance:12450, color:'#6366F1' },
    { name:'Nubank',              balance:5430,  color:'#820AD1' },
    { name:'Poupança BB',         balance:18920, color:'#F59E0B' }
  ];
  var BUDGET_GOALS = [
    { name:'Alimentação', budgeted:1200, spent:813,  color:'#6366F1' },
    { name:'Moradia',     budgeted:2800, spent:2920, color:'#F43F5E' },
    { name:'Transporte',  budgeted:500,  spent:222,  color:'#F59E0B' },
    { name:'Lazer',       budgeted:300,  spent:390,  color:'#38BDF8' }
  ];
  var DASH_CARDS = [
    { name:'Nubank',  limit:8000,  used:2345.8, color:'#820AD1' },
    { name:'XP Visa', limit:15000, used:1890.4, color:'#1C2951' }
  ];

  // ── Dados: Lançamentos ──────────────────────────────────────
  var CATS = ['Alimentação','Moradia','Transporte','Saúde','Lazer','Educação','Investimentos','Serviços','Vestuário','Outros'];
  var CONTAS_NOMES = ['Conta Corrente Itaú','Nubank','XP Investimentos'];
  var SAMPLE_TX = [
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
    { id:15, desc:'iFood',             cat:'Alimentação',  conta:'Nubank',              date:'2026-04-24', amount:-67.8,  type:'expense', status:'confirmed' }
  ];
  var STATUS_LABEL = { confirmed:'Confirmado', pending:'Pendente', scheduled:'Agendado' };
  var STATUS_COLOR = { confirmed:'income', pending:'warning', scheduled:'info' };

  // ── Dados: Orçamento ────────────────────────────────────────
  var BUDGET_CATS = [
    { id:1, name:'Alimentação', budgeted:1200, spent:812.9,  color:'#6366F1', icon:'🛒' },
    { id:2, name:'Moradia',     budgeted:2800, spent:2920.4, color:'#F43F5E', icon:'🏠' },
    { id:3, name:'Transporte',  budgeted:500,  spent:221.9,  color:'#F59E0B', icon:'🚗' },
    { id:4, name:'Saúde',       budgeted:400,  spent:209.5,  color:'#10B981', icon:'💊' },
    { id:5, name:'Lazer',       budgeted:300,  spent:389.7,  color:'#38BDF8', icon:'🎬' },
    { id:6, name:'Educação',    budgeted:600,  spent:0,      color:'#A78BFA', icon:'📚' },
    { id:7, name:'Vestuário',   budgeted:200,  spent:0,      color:'#FB923C', icon:'👕' },
    { id:8, name:'Serviços',    budgeted:350,  spent:342.1,  color:'#34D399', icon:'⚡' }
  ];

  // ── Dados: Relatórios (hierarquia 2 níveis: macro > categoria > subcategoria) ──
  var MONTHS_LABELS = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'];
  var TODAY_MONTH = 3;
  var YEAR_HIERARCHY = [
    { id:'despesas', label:'Despesas', type:'macro', color:'var(--expense)', categories: [
      { id:'moradia', label:'Moradia', color:'#6366F1', subcats: [
        { id:'aluguel', label:'Aluguel', data:[2500,2500,2500,2500,null,null,null,null,null,null,null,null] },
        { id:'agua',    label:'Água',    data:[78,83,75,78,null,null,null,null,null,null,null,null] },
        { id:'energia', label:'Energia', data:[222,217,225,342,null,null,null,null,null,null,null,null] }
      ]},
      { id:'alimentacao', label:'Alimentação', color:'#38BDF8', subcats: [
        { id:'mercado',     label:'Mercado',        data:[620,680,520,550,null,null,null,null,null,null,null,null] },
        { id:'restaurante', label:'Restaurantes',   data:[210,230,245,190,null,null,null,null,null,null,null,null] },
        { id:'ifood',       label:'iFood/Delivery', data:[150,140,125,73,null,null,null,null,null,null,null,null] }
      ]},
      { id:'transporte', label:'Transporte', color:'#F59E0B', subcats: [
        { id:'combustivel', label:'Combustível',    data:[180,150,230,120,null,null,null,null,null,null,null,null] },
        { id:'uber',        label:'Uber/99',        data:[85,80,110,67,null,null,null,null,null,null,null,null] },
        { id:'estac',       label:'Estacionamento', data:[45,50,80,35,null,null,null,null,null,null,null,null] }
      ]},
      { id:'saude', label:'Saúde', color:'#10B981', subcats: [
        { id:'farmacia',  label:'Farmácia',       data:[45,80,32,90,null,null,null,null,null,null,null,null] },
        { id:'consultas', label:'Consultas',      data:[0,80,0,120,null,null,null,null,null,null,null,null] },
        { id:'plano',     label:'Plano de Saúde', data:[75,40,57,0,null,null,null,null,null,null,null,null] }
      ]},
      { id:'lazer', label:'Lazer', color:'#F43F5E', subcats: [
        { id:'streaming', label:'Streaming',   data:[77.8,77.8,77.8,77.8,null,null,null,null,null,null,null,null] },
        { id:'cinema',    label:'Cinema',      data:[52,0,80,42,null,null,null,null,null,null,null,null] },
        { id:'bares',     label:'Bares/Lazer', data:[120.2,102.2,182.2,270.2,null,null,null,null,null,null,null,null] }
      ]},
      { id:'servicos', label:'Serviços', color:'#A78BFA', subcats: [
        { id:'internet',    label:'Internet',    data:[119.9,119.9,119.9,119.9,null,null,null,null,null,null,null,null] },
        { id:'telefone',    label:'Telefone',    data:[80,80,80,80,null,null,null,null,null,null,null,null] },
        { id:'assinaturas', label:'Assinaturas', data:[140.1,142.1,142.1,142.1,null,null,null,null,null,null,null,null] }
      ]}
    ]},
    { id:'receitas', label:'Receitas', type:'macro', color:'var(--income)', categories: [
      { id:'trabalho', label:'Trabalho', color:'#10B981', subcats: [
        { id:'salario',   label:'Salário',   data:[8200,8500,8000,8500,null,null,null,null,null,null,null,null] },
        { id:'freelance', label:'Freelance', data:[0,0,1200,0,null,null,null,null,null,null,null,null] },
        { id:'bonus',     label:'Bônus',     data:[0,0,0,0,null,null,null,null,null,null,null,null] }
      ]},
      { id:'investimentos', label:'Investimentos', color:'#6366F1', subcats: [
        { id:'dividendos',  label:'Dividendos',      data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        { id:'juros',       label:'Juros/CDB',       data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        { id:'aluguel-rec', label:'Aluguel recebido',data:[0,0,0,0,null,null,null,null,null,null,null,null] }
      ]},
      { id:'outros-rec', label:'Outros Recebimentos', color:'#F59E0B', subcats: [
        { id:'reembolso', label:'Reembolsos', data:[0,0,0,0,null,null,null,null,null,null,null,null] },
        { id:'vendas',    label:'Vendas',     data:[0,0,0,0,null,null,null,null,null,null,null,null] }
      ]}
    ]}
  ];

  var REPORT_CARDS = [
    { id:'balanco',     title:'Balanço Patrimonial',       desc:'Ativos, passivos e patrimônio líquido',             icon:'building',   color:'accent' },
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
    { id:'patrimonio',  title:'Patrimônio ao Longo do Ano',desc:'Crescimento do patrimônio mês a mês',              icon:'trendingUp', color:'income' }
  ];

  // ── Dados: demais telas ─────────────────────────────────────
  var PAGAR = [
    { id:1, name:'Aluguel',       due:'2026-05-02', amount:2500,   cat:'Moradia',    status:'pending'   },
    { id:2, name:'Energia Enel',  due:'2026-05-05', amount:342.1,  cat:'Moradia',    status:'pending'   },
    { id:3, name:'Internet Vivo', due:'2026-05-08', amount:119.9,  cat:'Serviços',   status:'pending'   },
    { id:4, name:'Gympass',       due:'2026-05-10', amount:99.9,   cat:'Saúde',      status:'pending'   },
    { id:5, name:'Nubank Fatura', due:'2026-05-15', amount:2345.8, cat:'Cartão',     status:'pending'   },
    { id:6, name:'Seguro Auto',   due:'2026-05-20', amount:380,    cat:'Transporte', status:'scheduled' },
    { id:7, name:'IPTU Parcela',  due:'2026-05-28', amount:210.5,  cat:'Moradia',    status:'scheduled' }
  ];
  var RECEBER = [
    { id:1, name:'Salário',        due:'2026-05-05', amount:8500, cat:'Receita', status:'scheduled' },
    { id:2, name:'Freela Design',  due:'2026-05-12', amount:1200, cat:'Receita', status:'pending'   },
    { id:3, name:'Aluguel Imóvel', due:'2026-05-10', amount:1800, cat:'Receita', status:'scheduled' }
  ];
  var EXTRATO_TX = [
    { date:'2026-04-24', desc:'iFood',            amount:-67.8,  status:'confirmed' },
    { date:'2026-04-23', desc:'Mercado Extra',    amount:-245.6, status:'confirmed' },
    { date:'2026-04-22', desc:'Transferência',    amount:-500,   status:'confirmed' },
    { date:'2026-04-22', desc:'Posto Shell',      amount:-187.4, status:'confirmed' },
    { date:'2026-04-21', desc:'Farmácia São João',amount:-89.5,  status:'confirmed' },
    { date:'2026-04-21', desc:'Conta de Água',    amount:-78.3,  status:'confirmed' },
    { date:'2026-04-20', desc:'Freela Design',    amount:1200,   status:'confirmed' },
    { date:'2026-04-18', desc:'Spotify',          amount:-21.9,  status:'confirmed' },
    { date:'2026-04-15', desc:'Academia Smart',   amount:-120,   status:'confirmed' },
    { date:'2026-04-10', desc:'Netflix',          amount:-55.9,  status:'confirmed' },
    { date:'2026-04-08', desc:'Posto Shell',      amount:-134.5, status:'confirmed' },
    { date:'2026-04-05', desc:'Salário',          amount:8500,   status:'confirmed' },
    { date:'2026-04-05', desc:'Aluguel',          amount:-2500,  status:'confirmed' },
    { date:'2026-04-01', desc:'Saldo anterior',   amount:0,      status:'balance', balance:4820.4 }
  ];
  var EXTRATO_CONTAS = [
    { name:'Conta Corrente Itaú', saldo:'R$ 12.450,00' },
    { name:'Nubank',              saldo:'R$ 5.430,00' },
    { name:'XP Investimentos',    saldo:'R$ 18.920,00' }
  ];
  var CATS_DATA = [
    { name:'Alimentação', sub:[], type:'expense' },
    { name:'Moradia', sub:['Aluguel','Condomínio','Água','Energia'], type:'expense' },
    { name:'Transporte', sub:['Combustível','Uber/99','Estacionamento'], type:'expense' },
    { name:'Saúde', sub:['Farmácia','Plano de Saúde','Consultas'], type:'expense' },
    { name:'Lazer', sub:['Streaming','Restaurantes','Cinema'], type:'expense' },
    { name:'Investimentos', sub:['Renda Fixa','Renda Variável','Criptomoedas'], type:'expense' },
    { name:'Serviços', sub:['Internet','Telefone','Assinaturas'], type:'expense' },
    { name:'Educação', sub:[], type:'expense' },
    { name:'Salário', sub:[], type:'income' },
    { name:'Freelance', sub:[], type:'income' },
    { name:'Aluguel recebido', sub:[], type:'income' },
    { name:'Dividendos', sub:[], type:'income' }
  ];
  var CARDS_DATA = [
    { id:1, name:'Nubank Mastercard', limit:8000, used:2345.8, color:'#820AD1', last4:'4389', due:15 },
    { id:2, name:'XP Visa Infinite',  limit:15000,used:1890.4, color:'#1C2951', last4:'7721', due:10 }
  ];
  var CONTAS_DATA = [
    { id:1, name:'Conta Corrente', bank:'Itaú Unibanco',   balance:12450 },
    { id:2, name:'Conta Poupança', bank:'Banco do Brasil', balance:18920 },
    { id:3, name:'Conta Digital',  bank:'Nubank',          balance:5430  }
  ];
  var CENTROS = ['Casa','Veículo','Trabalho','Projetos Pessoais','Investimentos','Família'];
  var TAGS = [
    { name:'mensal', count:24, color:'#6366F1' },
    { name:'fixo', count:8, color:'#F43F5E' },
    { name:'variável', count:15, color:'#F59E0B' },
    { name:'investimento', count:6, color:'#10B981' },
    { name:'lazer', count:11, color:'#38BDF8' },
    { name:'trabalho', count:4, color:'#A78BFA' },
    { name:'urgente', count:2, color:'#FB923C' },
    { name:'parcelado', count:7, color:'#34D399' }
  ];

  // ── Exporta ─────────────────────────────────────────────────
  global.SAS = global.SAS || {};
  global.SAS.fmt = fmt;
  global.SAS.fmtShort = fmtShort;
  global.SAS.fmtDate = fmtDate;
  global.SAS.ICONS = ICONS;
  global.SAS.data = {
    NAV: NAV, SCREEN_TITLES: SCREEN_TITLES,
    MONTHLY_DATA: MONTHLY_DATA, UPCOMING: UPCOMING, CONTAS_RECEBER: CONTAS_RECEBER,
    RECENT: RECENT, EXPENSE_CATS: EXPENSE_CATS, ACCOUNTS: ACCOUNTS,
    BUDGET_GOALS: BUDGET_GOALS, DASH_CARDS: DASH_CARDS,
    CATS: CATS, CONTAS_NOMES: CONTAS_NOMES, SAMPLE_TX: SAMPLE_TX,
    STATUS_LABEL: STATUS_LABEL, STATUS_COLOR: STATUS_COLOR,
    BUDGET_CATS: BUDGET_CATS,
    MONTHS_LABELS: MONTHS_LABELS, TODAY_MONTH: TODAY_MONTH,
    YEAR_HIERARCHY: YEAR_HIERARCHY, REPORT_CARDS: REPORT_CARDS,
    PAGAR: PAGAR, RECEBER: RECEBER, EXTRATO_TX: EXTRATO_TX, EXTRATO_CONTAS: EXTRATO_CONTAS,
    CATS_DATA: CATS_DATA, CARDS_DATA: CARDS_DATA, CONTAS_DATA: CONTAS_DATA,
    CENTROS: CENTROS, TAGS: TAGS
  };
})(window);
