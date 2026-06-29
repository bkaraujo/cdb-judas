# Architecture

Condutores arquiteturais independente de linguagem de programação.

## Estilo de Codificação

+ Favorecer composição em lugar de herança
+ Se um trecho de código é utilizado 2 ou mais vezes deve estar em um utilitário

### Parâmetros de Funções

A regra de limitação de parâmetros se aplica exclusivamente a **UseCases**:

+ UseCases com mais de 2 parâmetros devem receber um **DTO específico** (Command/Query)
+ Models de domínio (records) podem ter quantos campos forem necessários para representar a entidade
+ Services recebem parâmetros simples (primitivos, UUIDs, strings) sem limite rígido


## Padrões

### 001 - Vertical Slice Architecture

+ Funcionalidades separadas em pacotes, podendo ser compostas de sub-pacotes.
+ Todo o código de negócio da funcionalidade deve estar em seu pacote
+ Comunicação entre **feature** e **context** deve ser feita pela **facade** do context

### 002 - Hexagonal Architecture

Ainda dentro do VSA (#001 - Vertical Slice Architecture) deve haver a separação do modelo de domínio
de suas portas de entrada/saída.

O frontend (Vanilla JS) aplica o mesmo princípio em camadas:

+ `_1_domain`: modelos e regras de negócio, sem dependência de infraestrutura.
+ `_2_application`: serviços, orquestração e estado (ex.: `self-service`, `preferences-service`, `session-service`).
+ `_3_infrastructure`: adaptadores **primary** (`router`, `sidebar`, `theme`, `ui`) e **secondary** (`http-client`, `*-repository`, `auth-store`, `sse-client`).

### 003 - Request Tracing

Ao enviar uma requisição para o backend o frontend deve incluir o cabeçalho X-request-id com um
valor "YYYYMMDD" + "HH24MMSS" + "micros com 5 caracteres" + "user name".

O backend deve incluir no log como primeiro registro da mensagem o conteúdo do cabeçalho x-request-id.

+ O frontend monta o header no `http-client` (`reqId`); sem usuário autenticado usa `anonymous`.

### 004 - Token Rotativo de Acesso

O backend emite um novo `X-Access-Token` a cada resposta não-SSE bem-sucedida; o cliente deve
**substituir** o token armazenado a cada resposta.

+ `POST /login` retorna `X-Access-Token` e `X-User-Id` em headers (corpo vazio); o token vive em `sessionStorage` (`auth-store`).
+ Requisições não-SSE são serializadas numa fila (`http-client`) para o token rotativo não embaralhar.
+ O fluxo SSE (`/api/{uuid}/stream`) valida o token **sem** rotacionar.
+ `401` → limpa o token e dispara o handler de não-autorizado (retorna ao login).

### 005 - Namespace de Usuário & Guarda de Propriedade

Rotas de dados vivem sob `/api/{uuid}/…`, onde `{uuid}` é o `X-User-Id` retornado no login.

+ `http-client.withUser` prefixa o id automaticamente; rotas globais (`/login`, `/api/me`, `/api/version`, `/api/cost-center`) saem via `http.global.*`.
+ O backend valida o dono (`OwnershipInterceptor`): `{uuid}` ≠ usuário autenticado → `403`.
+ `/api/me` é escopado pela identidade autenticada (sem id no path) → sem risco de IDOR.

### 006 - Preferências Server-Owned (Write-through)

Preferências do usuário (`theme`, `language`, `locale`, `sidebarCollapsed`) têm o **servidor como dono**;
o `localStorage` é apenas **cache espelho** para boot sem flash.

+ Toda mudança é write-through: aplica no espelho na hora e envia `PATCH /api/me` com **debounce** (`preferences-service`).
+ Offline mantém pendências; re-sincroniza no evento `online`.
+ No login, `applyServer` reconcilia: **o servidor vence**; se o `theme` do servidor é nulo, o valor do cliente é ensinado de volta via `PATCH`.
+ `theme.js` e `sidebar.js` leem/gravam via `preferences-service`, nunca `localStorage` cru.
+ Local-only (não sincronizado): última tela, grupos da sidebar, ajustes do dashboard.