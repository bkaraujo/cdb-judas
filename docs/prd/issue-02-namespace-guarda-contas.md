# Issue 02 — Namespace `/api/{uuid}` + guarda de propriedade (contas como primeira rota)

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Introduzir o namespace `/api/{uuid}/...` e o interceptor central que valida a propriedade, migrando o recurso de **Contas** como primeira rota ponta a ponta (back-end + front-end + testes). Prova o vertical "login → uuid → rota protegida → dados do usuário → tela".

O `http-client` ganha a capacidade de montar caminhos com prefixo de usuário (opt-in por chamada durante a transição), de modo que os demais recursos possam migrar resource-a-resource nas fatias seguintes sem quebrar os que ainda estão nas rotas antigas.

## Acceptance criteria

- [ ] Interceptor central sobre `/api/{uuid}/**` retorna **403** quando o `uuid` da rota difere do usuário autenticado
- [ ] Rotas globais (centro de custo, login) não passam pela guarda (SSE deixa de ser tratado como rota global; passa a ser escopado por usuário na issue-12)
- [ ] Contas servidas em `/api/{uuid}/accounts` (coleção, item e saldo por período mês/ano); a rota antiga `/api/accounts` deixa de existir
- [ ] Os controladores não fazem binding do segmento `uuid`; a seleção de dados continua pelo token
- [ ] O `http-client` monta caminhos com prefixo `/{uuid}`; o repositório e a página de contas passam a usá-lo
- [ ] CRUD de contas + consulta de saldo funcionam ponta a ponta pela UI no novo caminho
- [ ] Testes: interceptor (uuid igual passa; divergente 403; rota global — centro de custo/login — ignora a guarda; uuid ausente/malformado rejeitado); recurso de contas no novo caminho

## Blocked by

- Blocked by `issue-01-fundacao-identidade-migracao.md`
