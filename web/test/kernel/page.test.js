/* web/test/kernel/page.test.js — testes de core/kernel/_2_infrastructure/primary/page.js. */
(function () {
  'use strict';

  QUnit.module('kernel:page', {
    afterEach: function () { window.CBD = { tags: [] }; }
  }, function () {

    QUnit.test('window.page: mount chama state()+bind()+render() nessa ordem, com $root já setado', function (assert) {
      const calls = [];
      const $root = $('<div></div>');
      const p = window.page({
        ns: '.pg-a',
        state: function () { calls.push('state'); return { $root: null }; },
        bind: function ($r) { calls.push('bind'); assert.strictEqual($r, $root, 'bind recebe o $root do mount'); },
        render: function () { calls.push('render'); },
      });
      p.mount($root);
      assert.deepEqual(calls, ['state', 'bind', 'render'], 'ordem: state, bind, render');
    });

    QUnit.test('window.page: repassa argumentos extras do mount pra state()', function (assert) {
      let received = null;
      const $root = $('<div></div>');
      const p = window.page({
        ns: '.pg-b',
        state: function (cardId) { received = cardId; return { $root: null }; },
        render: function () {},
      });
      p.mount($root, 'card-42');
      assert.strictEqual(received, 'card-42', 'argumento extra do mount chega em state()');
    });

    QUnit.test('window.page: onMount roda depois do render, recebendo o state', function (assert) {
      let stateAtOnMount = null;
      const $root = $('<div></div>');
      const p = window.page({
        ns: '.pg-c',
        state: function () { return { $root: null, tag: 'x' }; },
        render: function () {},
        onMount: function (state) { stateAtOnMount = state; },
      });
      p.mount($root);
      assert.strictEqual(stateAtOnMount.tag, 'x', 'onMount recebe o mesmo objeto de state');
    });

    QUnit.test('window.page: unmount desliga listeners no namespace (.off(ns)) e roda onUnmount', function (assert) {
      const $root = $('<div></div>');
      let clicks = 0;
      let onUnmountCalled = false;
      const p = window.page({
        ns: '.pg-d',
        state: function () { return { $root: null }; },
        render: function () {},
        bind: function ($r) { $r.on('click.pg-d', function () { clicks++; }); },
        onUnmount: function () { onUnmountCalled = true; },
      });
      p.mount($root);
      $root.trigger('click');
      assert.strictEqual(clicks, 1, 'listener ligado no mount dispara antes do unmount');

      p.unmount();
      $root.trigger('click');
      assert.strictEqual(clicks, 1, 'depois do unmount, .off(ns) removeu o listener — não dispara de novo');
      assert.ok(onUnmountCalled, 'onUnmount rodou');
    });

    QUnit.test('window.cachePage: sync roda antes do primeiro render (state já populado no 1º paint)', function (assert) {
      window.CBD = { tags: [{ id: 1, name: 'a' }] };
      let tagsSeenAtFirstRender = null;
      let stateRef = null;
      const $root = $('<div></div>');
      const p = window.cachePage({
        ns: '.pg-e',
        collection: 'tags',
        event: 'TAG',
        state: function () { stateRef = { tags: [], $root: null }; return stateRef; },
        render: function () { if (tagsSeenAtFirstRender === null) tagsSeenAtFirstRender = stateRef.tags.length; },
      });
      p.mount($root);
      assert.strictEqual(tagsSeenAtFirstRender, 1, 'sync populou state.tags antes do primeiro render rodar');
      p.unmount();
    });

    QUnit.test('window.cachePage: reage ao onChange customizado re-sincronizando e re-renderizando', function (assert) {
      let changeCb = null;
      let syncCount = 0;
      let renderCount = 0;
      const $root = $('<div></div>');
      const p = window.cachePage({
        ns: '.pg-g',
        state: function () { return { $root: null }; },
        render: function () { renderCount++; },
        sync: function () { syncCount++; },
        onChange: function (cb) { changeCb = cb; return function () {}; },
      });
      p.mount($root);
      assert.strictEqual(renderCount, 1, 'um render no mount');
      assert.strictEqual(syncCount, 1, 'um sync no mount');

      changeCb();
      assert.strictEqual(syncCount, 2, 'callback de mudança re-sincroniza');
      assert.strictEqual(renderCount, 2, 'callback de mudança re-renderiza');

      p.unmount();
    });

    QUnit.test('window.cachePage: unmount desinscreve (chama o unsubscribe devolvido por onChange)', function (assert) {
      let unsubscribed = false;
      const $root = $('<div></div>');
      const p = window.cachePage({
        ns: '.pg-h',
        state: function () { return { $root: null }; },
        render: function () {},
        sync: function () {},
        onChange: function () { return function () { unsubscribed = true; }; },
      });
      p.mount($root);
      p.unmount();
      assert.ok(unsubscribed, 'unsubscribe retornado por onChange foi chamado no unmount');
    });

    QUnit.test('window.cachePage: default de sync/subscribe usa App.CacheStore[collection]/subscribe(event) quando sync/onChange não são dados', function (assert) {
      const originalAccounts = window.App.CacheStore.accounts;
      const originalSubscribe = window.App.CacheStore.subscribe;
      let subscribedEvent = null;
      window.App.CacheStore.accounts = function () { return [{ id: 1 }, { id: 2 }]; };
      window.App.CacheStore.subscribe = function (event, cb) { subscribedEvent = event; return function () {}; };

      let stateRef = null;
      const $root = $('<div></div>');
      const p = window.cachePage({
        ns: '.pg-i',
        collection: 'accounts',
        event: 'ACCOUNT',
        state: function () { stateRef = { accounts: [], $root: null }; return stateRef; },
        render: function () {},
      });
      p.mount($root);

      assert.strictEqual(stateRef.accounts.length, 2, 'default sync leu App.CacheStore.accounts() pra dentro de state.accounts');
      assert.strictEqual(subscribedEvent, 'ACCOUNT', 'default subscribe usou App.CacheStore.subscribe(event, cb)');

      p.unmount();
      window.App.CacheStore.accounts = originalAccounts;
      window.App.CacheStore.subscribe = originalSubscribe;
    });
  });
})();
