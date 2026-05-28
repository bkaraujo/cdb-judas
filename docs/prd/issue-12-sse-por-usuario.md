# Issue 12 — Eventos em tempo real (SSE) escopados por usuário

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Mover o stream de eventos para o namespace do usuário em `/api/{uuid}/stream` e escopar a assinatura pelo `uuid`: o registro de assinantes passa a ser mantido por usuário e os eventos de domínio são entregues apenas aos assinantes do usuário dono do dado alterado. O cliente SSE passa a usar o caminho com prefixo de usuário. A guarda de propriedade protege o stream como qualquer rota de usuário.

## Acceptance criteria

- [ ] Stream servido em `/api/{uuid}/stream`; a rota global anterior (`/api/v1/sse/stream`) deixa de existir
- [ ] A guarda de propriedade protege o stream: `uuid` divergente do usuário autenticado retorna **403**
- [ ] Autenticação do stream permanece por token validado sem rotação (stream longo)
- [ ] O registro de assinantes é mantido por `uuid`; eventos de domínio são entregues apenas aos assinantes do usuário dono do dado alterado (um usuário nunca recebe eventos de outro)
- [ ] O cliente SSE usa o caminho com prefixo de usuário (`/api/{uuid}/stream`); a reconexão preserva o escopo
- [ ] Funciona ponta a ponta: uma alteração de dado de um usuário atualiza em tempo real apenas a(s) sessão(ões) daquele usuário

## Blocked by

- Blocked by `issue-01-fundacao-identidade-migracao.md`
- Blocked by `issue-02-namespace-guarda-contas.md`
