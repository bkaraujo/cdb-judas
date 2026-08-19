export * from '@/core/kernel/_2_infrastructure/primary/ui/badge.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/card.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/color-name-field.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/period-nav.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/tabs.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/tags-dropdown.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
export * from '@/core/kernel/_2_infrastructure/primary/ui/type-toggle.ts';

import { installModalEscHandler } from '@/core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { installSearchDropdownHandlers } from '@/core/kernel/_2_infrastructure/primary/ui/search-select.ts';
import type { Tag } from '@/core/kernel/_0_domain/tag.ts';
import { installTagFlagHandlers } from '@/core/kernel/_2_infrastructure/primary/ui/tags-dropdown.ts';

/** Assina os listeners globais (delegados no document) de toda a camada `ui/`: ESC fecha modal,
 * filtro/posicionamento/fechamento dos search-dropdowns, hover do indicador de tags. Chamar uma
 * única vez, na inicialização (composition-root, Fase 7) — nunca em nível de módulo. */
export function initUi(getTags: () => readonly Tag[]): void {
  installModalEscHandler();
  installSearchDropdownHandlers();
  installTagFlagHandlers(getTags);
}
