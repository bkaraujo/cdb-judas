export * from './badge.ts';
export * from './button.ts';
export * from './card.ts';
export * from './color-name-field.ts';
export * from './empty-state.ts';
export * from './modal.ts';
export * from './page-header.ts';
export * from './period-nav.ts';
export * from './progress-bar.ts';
export * from './search-select.ts';
export * from './tabs.ts';
export * from './tags-dropdown.ts';
export * from './toast.ts';
export * from './type-toggle.ts';

import { installModalEscHandler } from './modal.ts';
import { installSearchDropdownHandlers } from './search-select.ts';
import type { Tag } from '../../../_0_domain/tag.ts';
import { installTagFlagHandlers } from './tags-dropdown.ts';

/** Assina os listeners globais (delegados no document) de toda a camada `ui/`: ESC fecha modal,
 * filtro/posicionamento/fechamento dos search-dropdowns, hover do indicador de tags. Chamar uma
 * única vez, na inicialização (composition-root, Fase 7) — nunca em nível de módulo. */
export function initUi(getTags: () => readonly Tag[]): void {
  installModalEscHandler();
  installSearchDropdownHandlers();
  installTagFlagHandlers(getTags);
}
