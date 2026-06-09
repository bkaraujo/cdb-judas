# Issue 04 — Estado da sidebar persistido no servidor

## Parent

[`.claude/plans/perfil-configuracoes.md`](../../.claude/plans/perfil-configuracoes.md) — PRD "Perfil do Usuário & Configurações"

## What to build

Fatia **só de frontend**. Move o estado de recolhimento da sidebar do `localStorage`
cru para o pipeline write-through de preferências (reusando a máquina da Issue 03), de
modo que o layout preferido acompanhe o usuário entre sessões/dispositivos.

- **`sidebar.js`:** lê/grava `sidebarCollapsed` (e estado de grupos) via
  `preferences-service` em vez de `localStorage.getItem/setItem` direto. Mantém o
  comportamento atual de UI (recolher/expandir, abrir grupo).
- A mudança é **write-through**: aplica localmente na hora e sincroniza via
  `PATCH /api/me` (debounce), igual ao tema.

## Acceptance criteria

- [ ] `sidebar.js` não acessa mais `localStorage` cru para o estado de recolhimento;
      lê/grava via `preferences-service`.
- [ ] Recolher/expandir a sidebar persiste em `preferences.sidebarCollapsed` e é
      enviado ao servidor via `PATCH /api/me` (write-through, debounce).
- [ ] Ao relogar (inclusive de outro dispositivo), o estado de recolhimento é
      restaurado a partir do servidor.
- [ ] Comportamento de UI da sidebar (toggle, grupos, tooltips) permanece intacto.

## Blocked by

- Blocked by `issue-03-aparencia-tema-write-through.md`
