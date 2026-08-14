/* kernel/kernel.barrel.js — "F000Module" do frontend: sequencia os arquivos do kernel
 * compartilhado (_0_domain → _1_application → _2_infrastructure/secondary → _2_infrastructure/primary).
 * Secondary antes de primary porque os arquivos de primary já leem App.* / Infra.* só dentro de corpo de
 * função (nunca top-level) — ordem interna preservada para não introduzir risco novo.
 * Não é chamado por ninguém de fora; só sequencia os próprios arquivos.
 *
 * _0_domain/category.js e tag.js moraram em feature/categories e feature/tags até o
 * check-slices.js apontar que os widgets genéricos de picker do kernel (flatCategories,
 * categoryPickerHtml, tagsDropdownHtml, tagFlagHtml — padrão 007 do CLAUDE.md, usados por toda
 * fatia) dependem da forma pura desses modelos (labelChain/isRoot/resolve, sem I/O). Mesmo
 * critério que já valia para money.js: vocabulário puro e sem dependência sobe pro kernel; o
 * o serviço/CRUD de categorias/tags continua nas próprias fatias (_1_application/_2_infrastructure).
 *
 * transaction.js (movido de feature/transactions.js) e statement-item.js (nunca teve fatia
 * própria): mesmo critério — statement/credit-cards/accounts-payable precisam de Transaction.
 * describe/statusBadgeVariant e StatementItem.buildRows pra montar linha de extrato/fatura.
 *
 * account.js (movido de feature/accounts.js) e invoice.js (nunca teve fatia própria): pure
 * vocabulary do "cluster financeiro" — transactions/credit-cards/statement/dashboard precisam de
 * Domain.Account.{currentBalance,hasCards} e Domain.Invoice.{collapse,fetchWindow,dueDate} pra
 * montar linha de extrato/fatura. invoice.js depende de account.js (hasCards) — account antes. */
(function () {
  function inject(base, path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  }

  const m = window.CDB_MANIFEST.kernel;
  const domainBase = 'core/kernel/_0_domain/';
  const domainFiles = m.domain;

  const applicationBase = 'core/kernel/_1_application/';
  const applicationFiles = m.application;

  const secondaryBase = 'core/kernel/_2_infrastructure/secondary/';
  const secondaryFiles = m.secondary;

  const primaryBase = 'core/kernel/_2_infrastructure/primary/';
  const primaryFiles = m.primary;

  domainFiles.forEach(function (p) { inject(domainBase, p); });
  applicationFiles.forEach(function (p) { inject(applicationBase, p); });
  secondaryFiles.forEach(function (p) { inject(secondaryBase, p); });
  primaryFiles.forEach(function (p) { inject(primaryBase, p); });
})();
