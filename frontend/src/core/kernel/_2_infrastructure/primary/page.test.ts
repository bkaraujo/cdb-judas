import $ from 'jquery';
import { describe, expect, it } from 'vitest';
import { cachePage, createPage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';

interface TestState extends PageState {
  tag?: string;
}

describe('kernel:page', () => {
  it('createPage: mount chama state()+bind()+render() nessa ordem, com $root já setado', () => {
    const calls: string[] = [];
    const $root = $('<div></div>');
    const p = createPage<TestState>({
      ns: '.pg-a',
      state: () => {
        calls.push('state');
        return {};
      },
      bind: ($r) => {
        calls.push('bind');
        expect($r.is($root)).toBe(true);
      },
      render: () => {
        calls.push('render');
      },
    });
    p.mount($root);
    expect(calls).toEqual(['state', 'bind', 'render']);
  });

  it('createPage: repassa o param extra do mount pra state()', () => {
    let received: string | null | undefined = null;
    const $root = $('<div></div>');
    const p = createPage<TestState>({
      ns: '.pg-b',
      state: (cardId) => {
        received = cardId;
        return {};
      },
      render: () => {},
    });
    p.mount($root, 'card-42');
    expect(received).toBe('card-42');
  });

  it('createPage: onMount roda depois do render, recebendo o state', () => {
    const captured: { state: TestState | null } = { state: null };
    const $root = $('<div></div>');
    const p = createPage<TestState>({
      ns: '.pg-c',
      state: () => ({ tag: 'x' }),
      render: () => {},
      onMount: (state) => {
        captured.state = state;
      },
    });
    p.mount($root);
    expect(captured.state?.tag).toBe('x');
  });

  it('createPage: unmount desliga listeners no namespace (.off(ns)) e roda onUnmount', () => {
    const $root = $('<div></div>');
    let clicks = 0;
    let onUnmountCalled = false;
    const p = createPage<TestState>({
      ns: '.pg-d',
      state: () => ({}),
      render: () => {},
      bind: ($r) => {
        $r.on('click.pg-d', () => {
          clicks++;
        });
      },
      onUnmount: () => {
        onUnmountCalled = true;
      },
    });
    p.mount($root);
    $root.trigger('click');
    expect(clicks).toBe(1);

    p.unmount();
    $root.trigger('click');
    expect(clicks).toBe(1);
    expect(onUnmountCalled).toBe(true);
  });

  it('cachePage: sync roda antes do primeiro render (state já populado no 1º paint)', () => {
    let tagsSeenAtFirstRender: number | null = null;
    let stateRef: (TestState & { tags: number[] }) | null = null;
    const $root = $('<div></div>');
    const p = cachePage<TestState & { tags: number[] }>({
      ns: '.pg-e',
      state: () => {
        stateRef = { tags: [] };
        return stateRef;
      },
      sync: (state) => {
        state.tags = [1];
      },
      subscribe: () => () => {},
      render: () => {
        if (tagsSeenAtFirstRender === null) tagsSeenAtFirstRender = (stateRef as unknown as { tags: number[] }).tags.length;
      },
    });
    p.mount($root);
    expect(tagsSeenAtFirstRender).toBe(1);
    p.unmount();
  });

  it('cachePage: reage ao callback de subscribe re-sincronizando e re-renderizando', () => {
    let changeCb: (() => void) | null = null;
    let syncCount = 0;
    let renderCount = 0;
    const $root = $('<div></div>');
    const p = cachePage<TestState>({
      ns: '.pg-g',
      state: () => ({}),
      render: () => {
        renderCount++;
      },
      sync: () => {
        syncCount++;
      },
      subscribe: (cb) => {
        changeCb = cb;
        return () => {};
      },
    });
    p.mount($root);
    expect(renderCount).toBe(1);
    expect(syncCount).toBe(1);

    (changeCb as unknown as () => void)();
    expect(syncCount).toBe(2);
    expect(renderCount).toBe(2);

    p.unmount();
  });

  it('cachePage: unmount desinscreve (chama o unsubscribe devolvido por subscribe)', () => {
    let unsubscribed = false;
    const $root = $('<div></div>');
    const p = cachePage<TestState>({
      ns: '.pg-h',
      state: () => ({}),
      render: () => {},
      sync: () => {},
      subscribe: () => () => {
        unsubscribed = true;
      },
    });
    p.mount($root);
    p.unmount();
    expect(unsubscribed).toBe(true);
  });
});
