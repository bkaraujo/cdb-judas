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

O frontend (Vanilla JS) aplica o mesmo princípio em camadas, numeradas igual ao backend (`fNNN`):

+ `_0_domain`: modelos e regras de negócio, sem dependência de infraestrutura.
+ `_1_application`: serviços, orquestração e estado (ex.: `self-service`, `preferences-service`, `session-service`).
+ `_2_infrastructure`: adaptadores **primary** (`router`, `sidebar`, `theme`, `ui`) e **secondary** (`http-client`, `*-repository`, `auth-store`, `sse-client`).

Ver padrão 008 para onde essas 3 camadas vivem fisicamente (kernel vs. fatia de feature).

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

### 007 — Seleção de Categoria/Tag

Ver `docs/frontend/category-tag-pickers.md`. Regras:

+ Categoria: `window.categoryPickerHtml(...)` (`pickers.js`) — encapsula `<select>` escondido + `searchSelectHtml`.
+ Tag: `window.tagsDropdownHtml(tagIds, key)` — estado no array JS do chamador.
+ Elegibilidade: sempre via `flatCategories(nature, excludeRoots, keepId)` — sem exceção.
+ Placeholder: `<option value="" selected>Selecione</option>` explícito quando não há seleção válida.
+ Quick-create: inserir opção na mão (`refreshSearchSelect` / `appendTagRow`), não esperar SSE.
+ Dentro de `overflow:scroll`: passar `floating: true` — o painel é ancorado em `position:fixed`.
+ Exceções: seletor de categoria-pai, alvos de MOVE do `linkedDeleteDialog`.

### 008 — Fatias de Feature (Vertical Slice físico)

Espelha, sem bundler, o mesmo conceito de `br.cdb.feature.fNNN` do backend, adaptado à realidade
de Vanilla JS sem módulos: kernel e composition-root ficam em `web/core/` (renomeado de
`web/js/`); cada fatia de feature vira **um único arquivo** `web/feature/<slice>.js`, sobe pra
fora de `core/`, direto em `web/` — sem subpasta, sem barrel próprio. Dentro do arquivo, os
blocos (domain → application → infrastructure/secondary → infrastructure/primary) continuam
como IIFEs independentes concatenados na mesma ordem que um barrel garantiria, cada bloco com o
comentário do arquivo original preservado como separador de seção.

```
web/
├── core/                    equivalente a f000 (kernel) + f999 (composition-root)
│   ├── boot.js               único <script> referenciado por index.html
│   ├── kernel/                _0_domain/_1_application/_2_infrastructure/{primary,secondary}
│   │   └── kernel.barrel.js   sequencia os arquivos do kernel (equivalente ao F000Module)
│   ├── composition-root/      wiring de DI, shell, login-modal, bootstrap
│   │   └── composition-root.barrel.js
│   └── _1_domain/, _2_application/, _3_infrastructure/   legado, encolhe a cada fatia migrada
├── feature/                  cada fatia é 1 arquivo, sem subpasta
│   ├── cost-centers.js
│   ├── tags.js
│   ├── categories.js
│   ├── settings.js
│   ├── import-rules.js
│   ├── reports.js
│   └── accounts.js
└── pages/                    legado, encolhe a cada fatia migrada
```

+ **`web/core/kernel/`** — equivalente ao `f000`: código transversal (usado por 3+ domínios não
  relacionados — `http-client`, `sse-client`, `cache-store`, `ui`, `format`, `pickers`, `router`,
  `sidebar`, etc), organizado nas 3 camadas do padrão 002. Todas as fatias podem depender do
  kernel; o kernel não depende de nenhuma fatia.
+ **`web/core/composition-root/`** — equivalente ao `f999`: único lugar que faz o wiring de DI
  (`composition-root.js`, injeta `Infra.*Repository` em `App.*Service`) e conhece mais de uma
  fatia ao mesmo tempo. Roda por último no boot.
+ **Regra**: uma fatia (`web/feature/<slice>.js`) nunca referencia `window.*` definido por outra
  fatia, a menos que venha do kernel ou de um `web/feature/<slice>.api.js` (mecanismo público
  explícito, equivalente ao `FNNNApi` — só existe nas fatias com consumidor cross-slice
  comprovado). Enforced heuristicamente por `node web/tools/check-slices.js` (regex, não AST —
  roda manual antes de cada commit de fase, não trava CI).
+ **`web/core/boot.js`** — único `<script>` referenciado por `index.html`; injeta
  `kernel/kernel.barrel.js` → cada `feature/<slice>.js` (injeção direta, sem barrel — um arquivo
  só) → barrels legados por camada (`core/_1_domain.js` etc., encolhem a cada fatia migrada) →
  `composition-root/composition-root.barrel.js`.
+ **Vocabulário puro sobe pro kernel, mesmo quando a fatia "dona" existe**: `Domain.Category` e
  `Domain.Tag` vivem em `core/kernel/_0_domain` (não em `feature/categories.js`/`feature/tags.js`)
  porque os widgets genéricos de picker do padrão 007 (`flatCategories`, `categoryPickerHtml`,
  `tagsDropdownHtml`) — usados por toda fatia — dependem da forma pura desses modelos. Mesmo
  critério que já valia para `money.js`/`period.js`. O *serviço*/CRUD continua na fatia.
+ **Kernel precisando de uma ação de fatia** (ex.: quick-create de categoria/tag a partir de um
  picker do kernel, ou o botão de fechamento contábil na sidebar): kernel expõe um slot vazio
  (`window.configureQuickCreate(kind, provider)` em `pickers.js`; `Sidebar.configureClosing(...)`)
  e `composition-root.js` injeta o provider real depois que a fatia carregou — kernel nunca
  referencia `App.<Slice>Service`/função de outra fatia por nome.
+ Migração em andamento, fatia por fatia — estado atual e roadmap das fatias com coupling
  cruzado (ainda não migradas) em `.claude/frontend-refactor.md` (local, gitignored).