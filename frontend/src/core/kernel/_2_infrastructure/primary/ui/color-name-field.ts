import {esc} from '@/core/kernel/_0_domain/format.ts';
import {swatchesHtml} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';

export interface ColorNameFieldOptions {
  colorId: string;
  nameId: string;
  color: string;
  nameValue?: string;
  placeholder?: string;
  /** Paleta de swatches — sempre explícita (evita este módulo depender de `pickers.ts`, dono do
   * `PALETTE`, que por sua vez depende de `ui/` para os modais de quick-create). */
  swatches: readonly string[];
  swatchMarginTop?: string;
}

/** Round color swatch + text input + swatch grid. Byte-identical block em accounts/tags/
 * pickers.openTagCreateModal's create/edit forms. Retorna o label "Nome" + campo + grade de
 * swatches — quem chama envolve no próprio <div class="form-group full">. */
export function colorNameFieldHtml(opts: ColorNameFieldOptions): string {
  const swatches = swatchesHtml(opts.swatches, opts.color);
  const mt = opts.swatchMarginTop ? ' style="margin-top:' + esc(opts.swatchMarginTop) + ';"' : '';
  return (
    '<label class="form-label" for="' + esc(opts.nameId) + '">Nome</label>' +
    '<div class="form-color-field">' +
      '<input id="' + esc(opts.colorId) + '" name="color" type="color" value="' + esc(opts.color) + '" />' +
      '<input id="' + esc(opts.nameId) + '" name="name" type="text" required ' +
        'placeholder="' + esc(opts.placeholder || '') + '" value="' + esc(opts.nameValue || '') + '" />' +
    '</div>' +
    '<div class="form-color-field-swatches" data-region="swatches"' + mt + '>' + swatches + '</div>'
  );
}
