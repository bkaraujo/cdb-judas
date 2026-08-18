# CDB Finance

O **CDB Finance** é uma aplicação completa para gestão financeira pessoal. Desenvolvida com foco em manutenibilidade e escalabilidade, adota uma arquitetura moderna que separa responsabilidades de forma clara e segue boas práticas de desenvolvimento em Java e JavaScript.

> **Versão:** `1.0.0` — versão única para todos os módulos Maven. O frontend não tem versionamento próprio: `GET /api/version` devolve `quarkus.application.version` (herdado do `<version>` do pom) e a sidebar exibe esse valor.

## 🚀 Funcionalidades

O sistema cobre as necessidades de acompanhamento e planejamento financeiro:

- **Dashboard:** Agrega o resultado mensal (receitas, despesas e líquido) por categoria.
- **Contas e Cartões:** Gestão de múltiplas contas bancárias e cartões de crédito (cartão é entidade própria da conta), com consulta de saldo por período (mês/ano) e fechamento de competência.
- **Transações:** Registro de créditos, débitos, transferências e parcelas, com filtros, alteração de status e exclusão unitária, em grupo ou de transferência inteira.
- **Importação de Extratos:** Leitura de faturas e extratos em PDF com fluxo *preview → confirmação*. Detecta tipo de documento e emissor, expande parcelas, sugere categorias e casa lançamentos de cartão. Parsers para **BTG** e **Santander** (cartão e conta).
- **Extratos (Statements):** Histórico mensal por conta, com filtro por status.
- **Categorização:** Classificação de receitas e despesas por Categorias (macro/micro), Centros de Custo e Tags.
- **Perfil e Preferências:** Atualização self-service do próprio usuário (nome, tema, idioma, *locale*, estado da sidebar) via `/api/me`.
- **Atualização em tempo real:** Push de eventos por **SSE**, mantendo a interface sincronizada sem *polling*.
- **API documentada:** Especificação OpenAPI com Swagger UI em `/swagger`.

> Algumas telas do frontend (`budget`, `accounts-payable`, `reports`) ainda não têm endpoint correspondente no backend — são protótipos de UI, não funcionalidades entregues.

## 🎯 Público Alvo

O CDB Finance é destinado a **indivíduos e famílias** que desejam um controle rigoroso de suas finanças pessoais. É ideal para pessoas que:
- Precisam consolidar informações de múltiplos bancos e cartões em um só lugar.
- Querem estabelecer e acompanhar orçamentos para não estourar gastos.
- Buscam relatórios detalhados para entender seus hábitos de consumo.
- Preferem ferramentas robustas, com processamento local que garante a privacidade dos dados financeiros, fugindo de planilhas complexas.

## 📐 Escopo e Arquitetura

O projeto abrange frontend e backend, com forte separação arquitetural interna. A arquitetura é **híbrida**: **Vertical Slice** nas features de entrega HTTP (`br.cdb.feature.fNNN`) sobre **Hexagonal** nos contextos de negócio (`br.cdb.context.*`). Features se comunicam com os contextos exclusivamente via **Facade**.

### Módulos Maven

`br-parent` (pom pai: versões, plugins, gate de qualidade) · `br-commons` (framework comum, sem `br.cdb.*`) · `br-context-people` e `br-context-monetary` (contextos hexagonais, dependem só de `br-commons`) · `br-application` (borda HTTP/CDI, fast-jar Quarkus). `web/` é copiado para dentro do jar de `br-application`, mas não é módulo Maven.

### Backend (Java 25 + Quarkus)

- **Vertical Slice Architecture (VSA):** Cada funcionalidade vive isolada numa fatia numerada `br.cdb.feature.fNNN` (hoje `f000`–`f007`, `f009`, `f010`, `f999`), que é um hexágono auto-contido — `_0_domain` (modelos/overlays + portas + eventos), `_1_application` (`*Service`/`*UseCase` + commands + listeners), `_2_infrastructure` (`*Resource`, DTOs HTTP, `*JDBCRepository`) e um módulo CDI `FNNNModule` próprio (algumas fatias finas, como `f003`/`f009`, não têm `_0_domain` nem módulo próprio). O número expressa ordem de criação, não mais dependência: fatia de negócio nunca importa fatia irmã (regra ArchUnit `feature_slices_must_not_depend_on_sibling_slices`); `f000` é o kernel compartilhado, `f999` o composition root.
- **Arquitetura Hexagonal:** Dentro de cada contexto de negócio, livre de framework e com DI por `Context`:
  - `_0_domain` — modelos, portas (`*Repository`) e eventos de domínio.
  - `_1_application` — *commands*, *services*, *use cases* e escutadores de eventos (lógica pura).
  - Os adaptadores concretos (`*JDBCRepository`) ficam **fora** do contexto, em `br-application` (`br.cdb.infra.persistence`).
- **Contextos:** `monetary` (lógica financeira; facade `MonetaryUseCases`) e `people` (identidade mínima; facade `PeopleContext`). Não existe contexto `security` nem `shared` — login/preferências são agregados de `br-application`, e o vocabulário comum de erro/evento vive em `br.commons.business`.
- **Núcleo / Plataforma (`br.cdb.core`):** Autenticação e autorização (token opaco rotativo, `OwnershipFilter`), observabilidade (log de requisições + MDC), erro HTTP (`ProblemDetail`), `CoreModule` (costura CDI↔`Context`) e `SpaFallbackRoute` (deep-links da SPA caem em `index.html`).
- **Padrão Result:** Fluxo de negócio sem exceções (`Result` Success/Failure).
- **Eventos:** Comunicação cross-feature é best-effort via `br.commons.MessageBus` (nunca import direto entre fatias irmãs). O despacho SSE é responsabilidade exclusiva de `f999`.
- **Persistência:** **100% JDBC/H2** sobre pool próprio (dev: arquivo `./database`; testes: in-memory) — processamento e privacidade locais, sem servidor de banco externo. O schema vive em `Database` e conforma aos diagramas Mermaid em `docs/`.
- **Segurança:** Login com emissão de token, rotas de usuário escopadas por `/api/{uuid}/…` e guarda de propriedade que bloqueia acesso indevido (proteção contra IDOR).
- **Null-Safety & Qualidade:** `NullAway` + `ErrorProne` com anotações JSpecify (`@NullMarked`/`@Nullable`) no ciclo de compilação, falhando o build contra `NullPointerException`; PMD/CPD *enforcing* na fase `verify`.
- **Testes:** Testes unitários com JUnit 5, testes de arquitetura com ArchUnit (ex.: *Resources* não acessam repositórios; *feature* fala com *context* só via *facade*; fatia só depende de fatia anterior) e testes de integração HTTP com `@QuarkusTest` + RestAssured.

### Frontend (HTML / CSS / JS)

- **Single Page Application (SPA):** Abordagem leve (Vanilla JS/CSS + jQuery 4), sem etapa de build no frontend. É servida pelo próprio backend — o pom de `br-application` copia `web/` para `META-INF/resources` no classpath.
- **Arquitetura em Camadas:** O JavaScript replica o modelo de domínio:
  - `_1_domain` — entidades, validações e regras do cliente.
  - `_2_application` — serviços, orquestração e estado.
  - `_3_infrastructure` — adaptadores *primary* (`router`, `sidebar`, `theme`) e *secondary* (`http-client`, `*-repository`, `auth-store`, `sse-client`).
- **Design:** Interface moderna (`app.css`) com Dark Mode nativo (aplicado antes do primeiro *paint*, sem *flash*).
- **Request Tracing:** Cada requisição envia o cabeçalho `X-Request-Id` para correlação fim-a-fim no log do backend.
- **Dependência externa:** o jQuery 4 é carregado de CDN (`code.jquery.com`) — a primeira carga exige rede.

### Frontend TS (`frontend/`, em migração)

Em paralelo ao SPA vanilla acima, uma reescrita 1:1 em **TypeScript + Vite** vive em `frontend/` —
mesma paridade funcional, mesmo servidor Quarkus, arquitetura Hexagonal + Vertical Slice espelhando
o backend (`_0_domain` → `_1_application` → `_2_infrastructure/{primary,secondary}` no kernel,
`feature/<slice>/` nas fatias, cada uma com `index.ts`/`api.ts` como único ponto de acesso
cross-slice). Ainda não é o caminho padrão: o build Maven continua servindo `web/` a menos que o
profile `frontend-ts` seja ativado explicitamente.

```bash
cd frontend
npm install
npm run dev          # servidor de desenvolvimento Vite, com hot reload
npm run typecheck    # tsc --noEmit, strict
npm test             # Vitest
npm run check:arch   # dependency-cruiser — 5 regras (no-cross-slice, no-domain-to-infra, ...)
npm run build        # produção → frontend/dist
```

Para o backend servir `frontend/dist` em vez de `web/`, ative o profile Maven:

```bash
mvn -pl br-application -am quarkus:dev -Pfrontend-ts
```

O cutover (tornar este o caminho padrão e remover `web/`) é decisão futura, não coberta aqui.

## 🛠️ Tecnologias Utilizadas

| Camada | Stack |
|--------|-------|
| **Linguagens** | Java 25 · JavaScript (ES6+) · HTML5 · CSS3 |
| **Backend** | Quarkus 3.37.0 (REST, Hibernate Validator, SmallRye OpenAPI/Health, Elytron Security) — modo JVM |
| **Build** | Maven 3.9+ · Quarkus Maven Plugin |
| **Frontend** | Vanilla JS/CSS · jQuery 4 |
| **Banco** | H2 (embarcado, arquivo) via pool JDBC próprio (`br.commons`) |
| **Utilitários** | Lombok 1.18.42 · SLF4J · PDFBox 3.0.5 (leitura de PDF) · Jackson (YAML/JSR-310) · juniversalchardet (detecção de *charset*) |
| **API Docs** | SmallRye OpenAPI (Swagger UI) |
| **Qualidade & Testes** | JUnit 5 · ArchUnit · JaCoCo · PMD/CPD · NullAway · ErrorProne · JSpecify |

## 🏃 Como Executar

Há dois caminhos: **Maven** (o backend Quarkus serve o frontend estático) ou **Docker Compose** (backend + nginx em containers separados).

### Opção 1 — Maven (desenvolvimento)

```bash
git clone <url-do-repositorio>
cd cdb-judas
mvn -pl br-application -am quarkus:dev
```

Acesse `http://localhost:8080`. O frontend estático é servido pelo próprio backend.

> O `-pl br-application -am` é necessário: o `pom.xml` da raiz é apenas o agregador do reator multi-módulo, e `quarkus:dev` precisa apontar para o módulo de aplicação (`-am` constrói `br-parent`/`br-commons`/`br-context-*` antes).

Build completo com o gate de qualidade: `mvn verify`.

### Opção 2 — Docker Compose

```bash
docker compose up --build
```

- **Frontend (nginx):** `http://localhost:8081` — serve o estático e faz proxy reverso para a API.
- **Backend (Quarkus):** `http://localhost:8080` — acessível diretamente também.

> O build de imagem ainda não foi validado fim-a-fim (ver nota no `br-application/src/main/docker/Dockerfile.backend`); trate o caminho Docker como não verificado.

### Acesso

A aplicação semeia um usuário inicial no primeiro arranque (`f999`, que também dispara a criação das categorias padrão em `f005`):

| Usuário | Senha |
|---------|-------|
| `admin` | `admin` |

- **Swagger UI:** `/swagger` (ex.: `http://localhost:8080/swagger`).
- **Health:** `/q/health`.
- **Console do banco (dev):** `./db-console.sh` abre o H2 Console contra `./database`. Pare a aplicação antes — o H2 file-based trava o arquivo.

## ⚙️ Configuração

| Chave | Variável de ambiente | Descrição | Padrão |
|-------|----------------------|-----------|--------|
| `datasource.jdbc.url` | `DATASOURCE_JDBC_URL` | URL JDBC do H2 | `jdbc:h2:file:./database;DB_CLOSE_DELAY=-1` (dev) · `jdbc:h2:mem:cdb` (perfil `%test`) |
| `datasource.jdbc.username` / `.password` | `DATASOURCE_JDBC_USERNAME` / `_PASSWORD` | Credenciais do banco | `sa` / vazio |
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | Porta HTTP | `8080` |
| `cdb.security.session.idle-timeout-minutes` | — | Ociosidade tolerada antes de a sessão cair. A janela é renovada a cada requisição autenticada, então só conta tempo parado; valor ausente ou `<= 0` cai no padrão (com aviso no log) | `30` |
| — | `APP_LOGLEVEL_ROOT` | Nível do logger da aplicação (`br.commons`). Também aceita `APP_LOGLEVEL_<PACOTE>` para nível por pacote | `INFO` |
| `storage.json.path` | `STORAGE_JSON_PATH` | **Vestigial.** Diretório do stack JSON, que não persiste mais nenhum agregado (tudo migrou para JDBC); só o bean `Storage` sobrevive, sem consumidores | `/data` (container) |

> ⚠️ **Persistência no Docker:** o `docker-compose.yaml` monta `./data:/data`, mas o banco H2 grava em `./database.mv.db` relativo ao diretório de trabalho do container (`/app`) — **fora do volume**. Para persistir dados entre execuções, aponte `DATASOURCE_JDBC_URL` para `jdbc:h2:file:/data/database;DB_CLOSE_DELAY=-1`.

## 📚 Documentação

A documentação técnica completa mora na **WikiJS** (migrada de `docs/`/`CLAUDE.md` em 2026-08-12):

- **[Backend](http://localhost:3000/pt-br/secular/profissao/projetos/judas/backend)** — arquitetura (VSA + Hexagonal), módulos Maven, fatias `fNNN`, Result, Lombok, Null-Safety, persistência JDBC/H2, qualidade & build, testes, diagramas.
- **[Frontend](http://localhost:3000/pt-br/secular/profissao/projetos/judas/frontend)** — padrões 001–009, contratos de API (request/response JSON), diagramas.
- **[Regras de Negócio](http://localhost:3000/pt-br/secular/profissao/projetos/judas/regras-negocio)** — funcionalidades, domínio, ciclo de fatura de cartão, fechamento de período.
- **[Índice do projeto](http://localhost:3000/pt-br/secular/profissao/projetos/judas)** — overview, tecnologias, como executar.

O `CLAUDE.md` (raiz) mantém um apontador mínimo para essas páginas. Endpoints por fatia (sempre atual): [`br/cdb/feature/package-info.java`](br-application/src/main/java/br/cdb/feature/package-info.java).
