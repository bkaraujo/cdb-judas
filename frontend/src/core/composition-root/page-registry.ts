/** Monta o `PageRegistry` (15 ids) a partir dos serviços/apis já wireados em
 * `composition-root.ts` — só orquestra `createXPage(deps)` de cada fatia, nenhuma lógica própria.
 */
import type {AccountsApi, AccountService} from '@/feature/accounts/index.ts';
import {createAccountsPage} from '@/feature/accounts/index.ts';
import type {AccountsPayableApi, PayableService} from '@/feature/accounts-payable/index.ts';
import {createAccountsPayablePage} from '@/feature/accounts-payable/index.ts';
import type {BudgetApi, BudgetService} from '@/feature/budget/index.ts';
import {createBudgetPage} from '@/feature/budget/index.ts';
import type {CreditCardsApi, CreditCardService} from '@/feature/credit-cards/index.ts';
import {createCardStatementPage, createCreditCardsPage} from '@/feature/credit-cards/index.ts';
import type {CategoriesApi, CategoryService} from '@/feature/categories/index.ts';
import {createCategoriesPage} from '@/feature/categories/index.ts';
import {createDashboardPage} from '@/feature/dashboard/index.ts';
import type {ImportRuleService} from '@/feature/import-rules/index.ts';
import {createImportRulesPage} from '@/feature/import-rules/index.ts';
import type {ImportStatementApi} from '@/feature/import-statement/index.ts';
import {
    createCategoryEvolutionPage,
    createCategoryEvolutionService
} from '@/feature/report-category-evolution/index.ts';
import {createReportsPage} from '@/feature/reports/index.ts';
import type {SettingsAuthPort, SettingsSelfPort, SettingsThemePort} from '@/feature/settings/index.ts';
import {createSettingsPage} from '@/feature/settings/index.ts';
import type {StatementService} from '@/feature/statement/index.ts';
import {createStatementPage} from '@/feature/statement/index.ts';
import type {TagService} from '@/feature/tags/index.ts';
import {createTagsPage} from '@/feature/tags/index.ts';
import type {TransactionActions, TransactionsApi, TransactionService, TxLike} from '@/feature/transactions/index.ts';
import {createInstallmentsPage, createTransactionsPage} from '@/feature/transactions/index.ts';

import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import type {PeriodService} from '@/core/kernel/_1_application/period-service.ts';
import type {PreferencesService} from '@/core/kernel/_1_application/preferences-service.ts';
import type {PageRegistry} from '@/core/kernel/_2_infrastructure/primary/router.ts';
import type {Sidebar} from '@/core/kernel/_2_infrastructure/primary/sidebar.ts';

export interface PageRegistryDeps {
  cache: CacheStore;
  periodService: PeriodService;
  preferences: PreferencesService;
  sidebar: Sidebar;
  authStore: SettingsAuthPort;
  self: SettingsSelfPort;
  theme: SettingsThemePort;
  accountService: AccountService;
  payableService: PayableService;
  budgetService: BudgetService;
  creditCardService: CreditCardService;
  categoryService: CategoryService;
  importRuleService: ImportRuleService;
  statementService: StatementService;
  tagService: TagService;
  transactionService: TransactionService;
  transactionActions: TransactionActions;
  transactionsApi: TransactionsApi;
  accountsApi: AccountsApi;
  accountsPayableApi: AccountsPayableApi;
  budgetApi: BudgetApi;
  categoriesApi: CategoriesApi;
  creditCardsApi: CreditCardsApi;
  importStatementApi: ImportStatementApi;
}

export function buildPageRegistry(deps: PageRegistryDeps): PageRegistry {
  const openEditor = (opts: unknown) => deps.transactionsApi.openEditor(opts as Parameters<TransactionsApi['openEditor']>[0]);
  const openDeleteFlow = (tx: unknown, opts: { onDone?: () => void }) =>
    deps.transactionsApi.openDeleteFlow(tx as TxLike, opts as Parameters<TransactionsApi['openDeleteFlow']>[1]);

  return {
    accounts: createAccountsPage(deps.accountService),
    'accounts-payable': createAccountsPayablePage({
      service: deps.payableService,
      periodService: deps.periodService,
      cache: deps.cache,
      transactions: {
        create: (data) => deps.transactionsApi.create(data as unknown as Parameters<TransactionsApi['create']>[0]),
        update: (id, data) => deps.transactionsApi.update(id, data as unknown as Parameters<TransactionsApi['update']>[1]),
        openDeleteFlow: (item, opts) => deps.transactionsApi.openDeleteFlow(item as unknown as TxLike, opts),
        markPaid: (item, opts) => deps.transactionsApi.markPaid(item as unknown as TxLike, opts),
      },
    }),
    budget: createBudgetPage({ service: deps.budgetService, periodService: deps.periodService, cache: deps.cache }),
    'card-statement': createCardStatementPage({
      service: deps.creditCardService,
      periodService: deps.periodService,
      cache: deps.cache,
      transactions: { openEditor, openDeleteFlow },
    }),
    categories: createCategoriesPage(deps.categoryService),
    'credit-cards': createCreditCardsPage({
      service: deps.creditCardService,
      periodService: deps.periodService,
      onAccountsChange: (cb) => deps.accountsApi.onChange(cb),
      listTransactions: (params) => deps.transactionsApi.list(params),
    }),
    dashboard: createDashboardPage({
      cache: deps.cache,
      preferences: deps.preferences,
      transactions: deps.transactionsApi,
      accountsPayable: deps.accountsPayableApi,
      creditCards: deps.creditCardsApi,
      budget: deps.budgetApi,
      accounts: deps.accountsApi,
      categories: deps.categoriesApi,
    }),
    'import-rules': createImportRulesPage({ service: deps.importRuleService, cache: deps.cache }),
    'report-category-evolution': createCategoryEvolutionPage({
      service: createCategoryEvolutionService({
        txRepo: { list: (q) => deps.transactionsApi.list(q) },
        cache: deps.cache,
      }),
      cache: deps.cache,
    }),
    installments: createInstallmentsPage({
      transactionService: deps.transactionService,
      periodService: deps.periodService,
      actions: deps.transactionActions,
      cache: deps.cache,
      openImport: (opts) => deps.importStatementApi.open(opts),
    }),
    reports: createReportsPage(),
    settings: createSettingsPage({
      authStore: deps.authStore,
      selfService: deps.self,
      theme: deps.theme,
      sidebar: { refreshUser: () => deps.sidebar.refreshUser() },
    }),
    statement: createStatementPage({
      service: deps.statementService,
      periodService: deps.periodService,
      cache: deps.cache,
      transactions: { openEditor, openDeleteFlow },
    }),
    tags: createTagsPage(deps.tagService),
    transactions: createTransactionsPage({
      transactionService: deps.transactionService,
      periodService: deps.periodService,
      actions: deps.transactionActions,
      cache: deps.cache,
      openImport: (opts) => deps.importStatementApi.open(opts),
    }),
  };
}
