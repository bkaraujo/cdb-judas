/** NAV (estrutura + ícones) do sidebar. Substitui `sidebar.data.js`, que só era `<script>` porque
 * `fetch` falha sob `file://` (origin null bloqueado por CORS) — com Vite vira import normal. */
export interface NavItem {
  id: string;
  label: string;
  icon: string;
  children?: NavItem[];
}

export const SIDEBAR_NAV: NavItem[] = [
  { id: 'dashboard', label: 'Visão Geral', icon: 'home' },
  {
    id: 'movements',
    label: 'Movimentações',
    icon: 'layers',
    children: [
      { id: 'transactions', label: 'Lançamentos', icon: 'list' },
      { id: 'installments', label: 'Parcelamentos', icon: 'repeat' },
      { id: 'accounts-payable', label: 'A pagar e receber', icon: 'calendar' },
    ],
  },
  { id: 'statement', label: 'Extrato de Contas', icon: 'bookOpen' },
  { id: 'credit-cards', label: 'Cartões de Crédito', icon: 'creditCard' },
  { id: 'budget', label: 'Metas / Orçamento', icon: 'target' },
  {
    id: 'reports',
    label: 'Relatórios',
    icon: 'barChart',
    children: [
      { id: 'report-category-evolution', label: 'Evolução por categoria', icon: 'trendingUp' },
    ],
  },
  {
    id: 'registries',
    label: 'Cadastros',
    icon: 'database',
    children: [
      { id: 'cost-centers', label: 'Centros de Custo', icon: 'briefcase' },
      { id: 'categories', label: 'Categorias', icon: 'tag' },
      { id: 'accounts', label: 'Contas', icon: 'building' },
      { id: 'tags', label: 'Tags', icon: 'hash' },
      { id: 'import-rules', label: 'Regras de Nomenclatura', icon: 'edit' },
    ],
  },
];
