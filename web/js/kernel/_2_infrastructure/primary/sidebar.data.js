/* _3_infrastructure/primary/sidebar.data.js — NAV (estrutura + ícones) do sidebar.
 * Exposto como global (window.SIDEBAR_NAV) e carregado via <script> no barrel,
 * pois fetch() falha sob file:// (origin null bloqueado por CORS). */
window.SIDEBAR_NAV = [
  { "id": "dashboard", "label": "Visão Geral", "icon": "home" },
  {
   "id": "movements", "label": "Movimentações", "icon": "layers",
    "children": [
      {"id": "transactions",     "label": "Lançamentos",       "icon": "list" },
      {"id": "accounts-payable", "label": "A pagar e receber", "icon": "calendar" }
    ]
  },
  {"id": "statement",    "label": "Extrato de Contas",  "icon": "bookOpen" },
  {"id": "credit-cards", "label": "Cartões de Crédito", "icon": "creditCard" },
  {"id": "budget",       "label": "Metas / Orçamento",  "icon": "target" },
  {"id": "reports",      "label": "Relatórios",         "icon": "barChart" },
  {
   "id": "registries", "label": "Cadastros", "icon": "database",
    "children": [
      {"id": "cost-centers", "label": "Centros de Custo", "icon": "briefcase" },
      {"id": "categories",   "label": "Categorias",      "icon": "tag" },
      {"id": "accounts",     "label": "Contas",           "icon": "building" },
      {"id": "tags",         "label": "Tags",             "icon": "hash" },
      {"id": "import-rules", "label": "Regras de Nomenclatura", "icon": "edit" }
    ]
  }
];
