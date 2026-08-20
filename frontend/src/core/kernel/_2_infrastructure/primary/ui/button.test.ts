import { describe, expect, it } from 'vitest';
import $ from 'jquery';
import { rowActionBtn, rowActionsHtml } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';

describe('rowActionBtn / rowActionsHtml', () => {
  it('produzem a mesma marcação para o mesmo botão', () => {
    const jq = rowActionBtn('edit', 'Editar', '7')[0]?.outerHTML;
    const str = rowActionsHtml('7', { trash: false });
    expect($(str)[0]?.outerHTML).toBe(jq);
  });

  it('marca o botão de excluir como danger', () => {
    expect(rowActionsHtml('7', { edit: false })).toContain('is-danger');
    expect(rowActionsHtml('7', { edit: false })).toContain('data-act="trash"');
  });

  it('act sobrepõe o nome do ícone', () => {
    expect(rowActionBtn('eye', 'Reativar', '3', { act: 'reactivate' })[0]?.outerHTML)
      .toContain('data-act="reactivate"');
  });

  it('tamanho 28 vira classe e não estilo inline', () => {
    const html = rowActionsHtml('1', { trash: false });
    expect(html).toContain('icon-btn-sm');
    expect(html).not.toContain('style="width');
  });

  it('tamanho fora dos dois casos comuns continua inline', () => {
    expect(rowActionsHtml('1', { size: 40, trash: false })).toContain('width:40px;height:40px;');
  });

  it('extra vem antes de editar/excluir e escapa o id', () => {
    const html = rowActionsHtml('a"b', { extra: '<i></i>' });
    expect(html.indexOf('<i></i>')).toBe(0);
    expect(html).toContain('data-id="a&quot;b"');
  });
});
