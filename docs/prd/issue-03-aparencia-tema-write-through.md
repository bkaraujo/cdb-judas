# Issue 03 — Aba Aparência: tema persistido no servidor (write-through + reconciliação + sem flash)

## Parent

[`.claude/plans/perfil-configuracoes.md`](../../.claude/plans/perfil-configuracoes.md) — PRD "Perfil do Usuário & Configurações"

## What to build

Fatia **só de frontend** (o backend já aceita `PATCH` de preferências na Issue 02).
Migra o tema do `localStorage` cru para o pipeline **write-through** servidor-dono, com
cache espelho para boot sem flash e reconciliação no login.

- **`preferences-service` (refatoração):** vira camada de **cache espelho +
  write-through**. Toda mudança de preferência aplica localmente na hora e envia
  `PATCH /api/me` com **debounce**; funciona offline e sincroniza ao reconectar. O
  `localStorage` passa a ser cache espelho das preferências do servidor.
- **`theme.js`:** passa a ler/gravar tema via `preferences-service` em vez do
  `localStorage` cru.
- **Boot sem flash:** no boot, o app aplica o tema do cache espelho imediatamente
  (sem flash do tema errado).
- **Reconciliação no `hydrate`:** ao logar, `GET /api/me` popula nome + preferências
  no cache e aplica tema. **O servidor vence**, exceto quando o `theme` do servidor é
  nulo: nesse caso o valor do cliente é enviado via `PATCH` (o servidor "aprende" a
  preferência atual).
- **Aba Aparência:** ajusta o **tema** (claro/escuro); a escolha é persistida no
  servidor e o efeito visual é imediato.

## Acceptance criteria

- [ ] `preferences-service` aplica mudança local na hora e envia `PATCH /api/me` com
      debounce (write-through); mudança offline vale localmente e sincroniza ao
      reconectar.
- [ ] `localStorage` atua como cache espelho das preferências; `theme.js` lê/grava via
      `preferences-service`.
- [ ] No boot o tema do cache é aplicado imediatamente — sem flash do tema errado.
- [ ] No login, `hydrate` reconcilia: servidor vence; se `theme` do servidor é nulo, o
      valor do cliente é enviado via `PATCH`.
- [ ] Aba Aparência alterna o tema com efeito visual imediato e persistência no
      servidor.
- [ ] Verificação manual: alternar tema, recarregar (sem flash) e relogar de outro
      ponto mantém o tema escolhido.

## Blocked by

- Blocked by `issue-01-read-pipe-api-me-avatar.md`
- Blocked by `issue-02-write-pipe-settings-shell-perfil.md`
