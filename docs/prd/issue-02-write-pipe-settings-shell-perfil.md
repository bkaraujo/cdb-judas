# Issue 02 — Write pipe `PATCH /api/me` + shell de Configurações + aba Perfil

## Parent

[`.claude/plans/perfil-configuracoes.md`](../../.claude/plans/perfil-configuracoes.md) — PRD "Perfil do Usuário & Configurações"

## What to build

Caminho de **escrita** ponta a ponta: o usuário abre Configurações pelo avatar, edita
o nome na aba Perfil e a alteração persiste via `PATCH /api/me`, refletindo no
avatar/saudação. O backend já implementa aqui o PATCH **completo** (nome **e**
preferências com merge), para que as fatias seguintes fiquem só no frontend.

- **API:** `PATCH /api/me` aceita corpo **parcial** e faz **merge**:
  `{ name?, preferences?: { theme?, language?, locale?, sidebarCollapsed? } }`. Campos
  ausentes/nulos não são alterados. DTO de entrada distinto do record de domínio
  (todos os campos anuláveis), preservando o domínio sempre preenchido. Validação de
  servidor no padrão `ProblemDetail`. Sem token → `401`.
- **Aplicação:** `UserUseCase.updateName` (aplica trim; nome em branco respeita a
  regra de opcionalidade → exibição cai para `username`) e
  `UserUseCase.updatePreferences` (merge de patch parcial: campo nulo mantém o valor
  atual). A regra de merge é lógica de domínio pura no record `Preferences`. Ambas
  expostas pela fachada do contexto de segurança.
- **Frontend — shell:** nova página = **shell com abas** (Perfil, Aparência),
  registrada no roteador hash (`#/settings`, rótulo "Configurações") e no carregador
  de páginas. **Não** entra no menu lateral principal.
- **Frontend — aba Perfil:** edita o **nome de exibição**; exibe `username` como
  **somente leitura**; permite deixar o nome em branco; retorno claro de sucesso/erro
  ao salvar. Ao salvar, dispara `PATCH /api/me` (só `name`) e atualiza avatar/nome no
  `auth-store`.
- **Frontend — avatar:** clique no avatar da sidebar navega para `#/settings`.

## Acceptance criteria

- [ ] `PATCH /api/me` com corpo parcial faz merge; só `name` não afeta preferências e
      vice-versa.
- [ ] DTO de entrada é distinto do record de domínio (campos anuláveis); domínio
      permanece sempre preenchido.
- [ ] `PATCH /api/me` sem token → `401`; erro de validação no padrão `ProblemDetail`.
- [ ] `updateName` aplica trim; nome em branco mantém exibição via `username` sem erro.
- [ ] `updatePreferences` faz merge correto (campo nulo mantém atual; demais mudam).
- [ ] Página `#/settings` ("Configurações") existe como shell com abas Perfil e
      Aparência; registrada no roteador e no carregador de páginas; **ausente** do menu
      lateral.
- [ ] Aba Perfil edita o nome, mostra `username` somente leitura, aceita nome em
      branco e dá feedback de sucesso/erro ao salvar.
- [ ] Salvar o nome reflete no avatar/saudação após `PATCH`.
- [ ] Clique no avatar da sidebar abre `#/settings`; tooltip "Perfil" mantido.
- [ ] `UserUseCaseTest` cobre `updateName` (trim/branco) e `updatePreferences` (merge
      parcial).
- [ ] Teste HTTP do recurso `self`: `PATCH` parcial só-nome não afeta preferências (e
      vice-versa); sem token `401`.

## Blocked by

- Blocked by `issue-01-read-pipe-api-me-avatar.md`
