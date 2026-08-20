import { describe, expect, it } from 'vitest';
import $ from 'jquery';
import { bindRecordActions } from '@/core/kernel/_2_infrastructure/primary/record-actions.ts';

const RECS = [{ id: '1', name: 'um' }, { id: '2', name: 'dois' }];
const find = (id: string) => RECS.find((r) => r.id === id) || null;

function root(): JQuery {
  return $(
    '<div>' +
      '<button data-act="new"></button>' +
      '<button data-act="edit" data-id="1"></button>' +
      '<button data-act="trash" data-id="2"></button>' +
      '<button data-act="edit" data-id="999"></button>' +
    '</div>',
  );
}

describe('bindRecordActions', () => {
  it('despacha new, edit e trash com o registro resolvido', () => {
    const seen: string[] = [];
    const $r = root();
    bindRecordActions($r, '.t', {
      find,
      onNew: () => seen.push('new'),
      onEdit: (r) => seen.push('edit:' + r.name),
      onDelete: (r) => seen.push('trash:' + r.name),
    });
    $r.find('[data-act=new]').trigger('click');
    $r.find('[data-act=edit][data-id=1]').trigger('click');
    $r.find('[data-act=trash]').trigger('click');
    expect(seen).toEqual(['new', 'edit:um', 'trash:dois']);
  });

  it('id desconhecido chama onMissing e não o handler', () => {
    const seen: string[] = [];
    const $r = root();
    bindRecordActions($r, '.t', {
      find,
      onEdit: () => seen.push('edit'),
      onMissing: () => seen.push('missing'),
    });
    $r.find('[data-act=edit][data-id=999]').trigger('click');
    expect(seen).toEqual(['missing']);
  });

  it('id desconhecido sem onMissing é ignorado em silêncio', () => {
    const seen: string[] = [];
    const $r = root();
    bindRecordActions($r, '.t', { find, onEdit: () => seen.push('edit') });
    $r.find('[data-act=edit][data-id=999]').trigger('click');
    expect(seen).toEqual([]);
  });

  it('before roda antes de resolver', () => {
    const seen: string[] = [];
    const $r = root();
    bindRecordActions($r, '.t', {
      find,
      before: () => seen.push('before'),
      onEdit: () => seen.push('edit'),
    });
    $r.find('[data-act=edit][data-id=1]').trigger('click');
    expect(seen).toEqual(['before', 'edit']);
  });

  it('off(ns) desliga tudo', () => {
    const seen: string[] = [];
    const $r = root();
    bindRecordActions($r, '.t', { find, onEdit: () => seen.push('edit') });
    $r.off('.t');
    $r.find('[data-act=edit][data-id=1]').trigger('click');
    expect(seen).toEqual([]);
  });
});
