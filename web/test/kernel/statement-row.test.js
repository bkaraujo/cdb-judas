/* web/test/kernel/statement-row.test.js — testes de core/kernel/_2_infrastructure/primary/statement-row.js. */
(function () {
  'use strict';

  function seedCache(categories, accounts) {
    window.CBD = { categories: categories || [], accounts: accounts || [] };
  }

  QUnit.module('kernel:statement-row', {
    afterEach: function () { window.CBD = { categories: [], accounts: [] }; }
  }, function () {

    QUnit.test('statementColumns mede a maior categoria/conta e memoiza entre chamadas', function (assert) {
      seedCache(
        [{ id: 1, name: 'Curta', nature: 'EXPENSE', active: true },
         { id: 2, name: 'Uma categoria bem mais comprida', nature: 'EXPENSE', active: true }],
        [{ id: 1, name: 'Conta A' }, { id: 2, name: 'Conta Bem Mais Comprida' }]
      );
      const first = window.statementColumns();
      assert.ok(first.catColCh > 20, 'catColCh acompanha o label mais longo');
      assert.ok(first.accColCh > 15, 'accColCh acompanha o nome de conta mais longo');

      // Muda o cache sem disparar evento CATEGORY/ACCOUNT — memoização deve manter o valor antigo.
      seedCache([{ id: 1, name: 'X', nature: 'EXPENSE', active: true }], [{ id: 1, name: 'Y' }]);
      const second = window.statementColumns();
      assert.deepEqual(second, first, 'sem invalidação, statementColumns devolve o cache');
    });

    QUnit.test('statementRowHtml estilo dot: linha de saldo anterior some o valor e mostra o acumulado', function (assert) {
      seedCache([], []);
      const row = { id: null, date: '2026-03-01', description: 'Saldo anterior', amount: 0, status: 'balance', runningBal: 1000, categoryId: null, tagIds: [] };
      const html = window.statementRowHtml(row, { showBalance: true, status: 'dot', isLast: false });

      assert.ok(html.indexOf('1.000,00') >= 0, 'mostra o saldo acumulado');
      assert.ok(html.indexOf('background:var(--bg-hover)') >= 0, 'linha de header usa fundo destacado');
      assert.ok(html.indexOf('data-id=""') >= 0, 'header sem id de transação');
    });

    QUnit.test('statementRowHtml estilo dot: transação comum mostra valor, acumulado e ações; isLast omite borda', function (assert) {
      seedCache([{ id: 9, name: 'Mercado', nature: 'EXPENSE', active: true }], []);
      const row = { id: 5, date: '2026-03-10', description: 'Compra', amount: -80, status: 'pending', runningBal: 920, categoryId: 9, tagIds: [] };

      const withBorder = window.statementRowHtml(row, { showBalance: true, status: 'dot', isLast: false });
      assert.ok(withBorder.indexOf('border-bottom:1px solid var(--border-light);') >= 0, 'linha do meio tem borda inferior');

      const html = window.statementRowHtml(row, {
        showBalance: true, status: 'dot', isLast: true,
        actions: function (r) { return '<button data-act="edit" data-id="' + r.id + '"></button>'; },
      });
      assert.ok(html.indexOf('80,00') >= 0, 'mostra o valor da transação');
      assert.ok(html.indexOf('920,00') >= 0, 'mostra o saldo acumulado');
      assert.ok(html.indexOf('data-act="edit"') >= 0, 'callback de ações é chamado pra linhas normais');
      assert.ok(html.indexOf('border-bottom:1px solid var(--border-light);') === -1, 'isLast omite a borda inferior');
    });

    QUnit.test('statementRowHtml esconde ações em linha de cabeçalho mesmo com actions() informado', function (assert) {
      seedCache([], []);
      const row = { id: null, date: '2026-03-01', description: 'Fatura anterior', amount: 0, status: 'balance', runningBal: 300, categoryId: null, tagIds: [] };
      const html = window.statementRowHtml(row, {
        showBalance: true, status: 'dot',
        actions: function () { return '<button data-act="trash"></button>'; },
      });
      assert.ok(html.indexOf('data-act="trash"') === -1, 'header nunca recebe ações, mesmo com callback');
    });

    QUnit.test('statementRowHtml estilo badge: mostra índice, conta, status como badge e sempre o valor', function (assert) {
      seedCache([], []);
      const row = { id: 3, date: '2026-03-05', description: 'Salário', amount: 500, status: 'confirmed', type: 'income', categoryId: null, tagIds: [] };
      const html = window.statementRowHtml(row, {
        index: 1, showAccount: true, accountName: 'Conta Corrente', status: 'badge',
      });

      assert.ok(html.indexOf('>1</span>') >= 0, 'mostra o ordinal');
      assert.ok(html.indexOf('Conta Corrente') >= 0, 'mostra a coluna de conta');
      assert.ok(html.indexOf('class="badge badge-') >= 0, 'status vira badge, não dot');
      assert.ok(html.indexOf('500,00') >= 0, 'valor sempre visível no estilo badge');
    });

    QUnit.test('statementRowHtml invoiceLink: linka a descrição só quando invoiceLink e row.invoice são true', function (assert) {
      seedCache([], []);
      const invoiceRow = { id: 7, date: '2026-03-05', description: 'Cartões de crédito', amount: -200, status: 'confirmed', invoice: true, categoryId: null, tagIds: [] };

      const linked = window.statementRowHtml(invoiceRow, { showBalance: true, status: 'dot', invoiceLink: true });
      assert.ok(linked.indexOf('<a href="#/credit-cards"') >= 0, 'com invoiceLink:true e row.invoice:true, vira link');

      const unlinked = window.statementRowHtml(invoiceRow, { showBalance: true, status: 'dot', invoiceLink: false });
      assert.ok(unlinked.indexOf('<a href="#/credit-cards"') === -1, 'sem invoiceLink, mesmo com row.invoice:true, não vira link');
    });

    QUnit.test('rowCountLabel: singular vs plural', function (assert) {
      assert.strictEqual(window.rowCountLabel(1), '1 transação exibida');
      assert.strictEqual(window.rowCountLabel(0), '0 transações exibidas');
      assert.strictEqual(window.rowCountLabel(3), '3 transações exibidas');
    });
  });
})();
