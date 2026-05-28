# Issue 01 — Fundação de identidade + estado limpo (descarte + seed)

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Introduzir o identificador imutável de usuário (`uuid`) como chave de identidade do sistema, ponta a ponta, **sem ainda mudar as rotas**. O login passa a autenticar por `username`, devolver o `uuid` ao cliente e o token passa a referenciar o `uuid`. A persistência por usuário passa a derivar o nome do arquivo do `uuid`. Os dados existentes são descartados; um seed inicial recria o usuário (com `uuid`) no registro central e provê a fonte global de centros de custo. Ao final, a aplicação continua funcionando normalmente (rotas antigas intactas), agora a partir de um estado limpo no esquema por `uuid`.

Esta é a fatia-chave (tracer bullet) que prova o caminho login → `uuid` → seleção de dados por usuário, sobre a qual todas as demais fatias se apoiam.

## Acceptance criteria

- [ ] Usuário possui `id` imutável (uuid); existe um registro central de usuários com `id`, `username` e hash de senha; `findByUsername` e `findById` funcionam
- [ ] Login autentica por `username`, valida a senha e retorna o token + o `uuid` no cabeçalho `X-User-Id`
- [ ] O token resolve para o `uuid`; o usuário autenticado expõe o id (`CurrentUser.getId()`)
- [ ] O nome do arquivo de dados por usuário deriva do `uuid`; um usuário autenticado só lê/grava o próprio arquivo `{uuid}`
- [ ] O MDC registra tanto o `uuid` quanto o `username`
- [ ] O cliente armazena o `uuid` no login (disponível para montagem de rotas nas fatias seguintes); as telas existentes continuam carregando dados
- [ ] Sem migração de dados: os dados existentes são descartados (sem conversão nem renomeação); não há código de migração (runtime ou avulso)
- [ ] Seed inicial: cria o registro central com o usuário e seu `uuid` (a senha vive no registro) e provê a fonte global de centros de custo (`cost-centers.json`)
- [ ] Rotas atuais continuam funcionando (ainda sem namespace); aplicação utilizável a partir do estado limpo
- [ ] Testes: registro de usuários + token (`findByUsername`/`findById`; emissão e rotação de token mapeiam para o `uuid`; `username` preservado para logs)

## Blocked by

None - can start immediately
