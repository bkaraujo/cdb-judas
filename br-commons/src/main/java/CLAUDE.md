# br-commons — Framework Comum

Módulo Maven `commons` (`br.cdb:commons`). Toolkit técnico **agnóstico de framework e de domínio**: nenhuma classe aqui conhece `br.cdb.context.*`, `br.cdb.feature.*`, Quarkus/CDI ou Spring. É a base sobre a qual os contextos de negócio (`br-context-monetary`, `br-context-people`) e a aplicação (`br-application`) são construídos — raiz da árvore de dependências Maven do projeto.

> Índice operacional deste módulo. Filosofia de cada padrão em `@docs/backend/` — consulte antes de implementar.

## Regra de dependência

`br-commons` não depende de nenhum outro módulo do projeto. Não importe `br.cdb.*` aqui — se uma classe precisa saber de contexto ou feature, ela não pertence a `br-commons`.

## Mapa de pacotes

| Pacote / Classe | Conteúdo |
|---|---|
| `Result` / `Result.Success` / `Result.Failure` | Railway Oriented Programming — `map`/`flatMap`/`recover`/`ifSuccess`/`ifFailure`. `.get()` desembrulha e é **fatal** em falha (chama `Logger.fatal` antes de lançar); `.getOrThrow()` só lança, sem fatal (uso reservado a `ResultTest`). Ver `@docs/backend/result-pattern.md`. |
| `business` | Vocabulário de erro/evento **compartilhado entre contextos**: `BusinessError` (sealed: `NotFound`/`BusinessRule`/`Validation`/`Conflict`), `BusinessEvent` (marker de `Message` para o `MessageBus`), `BusinessException` (runtime, carrega um `BusinessError`). Isto é, na prática, o "núcleo comum" que o pacote reservado `context..shared..` (citado como exceção nas regras ArchUnit) ainda não implementa — hoje esse papel é cumprido por `br.commons.business`, não por um pacote `shared` dentro de cada contexto. |
| `Registry` | Service locator manual thread-safe (`get`/`set`/`tryGet`/`remove`/`clear`) — mecanismo de DI dos contextos, sem framework. Cada `*Bootstrap` (composition root de um contexto) publica sua Facade aqui; `br.cdb.core.ContextBridge` é a única costura entre CDI e `Registry`. |
| `annotation` | Marcadores/estereótipos usados por convenção e pelo ArchUnit — **não** são anotações CDI: `Facade`, `Context` (`extends Lifecycle`), `Factory`, `UseCase`, `Specification`, `State`, `Configurable`, `Lifecycle`. |
| `MessageBus` | Pub/sub em memória por reflexão: `subscribe(Object/Class)` varre métodos anotados `@MessageListener` (assinatura única, retorno `MessageResult`, parâmetro assinável a `Message`) e monta `MethodHandle`s; `submit(Message)` despacha por hierarquia de classes (base→concreta) com cache de dispatch invalidado a cada novo `subscribe`. |
| `framework.message` | Contratos do `MessageBus`: `Message`, `MessageListener`, `MessageProcessor`, `MessageResult`. |
| `framework.logger` | Logger próprio (não é wrapper fino do SLF4J): canais (`ConsoleChannel`, `FileChannel`, `RollingFileChannel`), forwarders por nível (`trace/verbose/debug/info/warn/error/fatal`), `MDC`, ponte para SLF4J (`bridge/`) e para `java.util.logging` (`JULBridgeHandler`). `Logger.fatal` **aborta a JVM** (`Meta.exit(99)`) — por isso um `throw new RuntimeException(...)` logo após `Logger.fatal` em outro código é inalcançável, só existe para satisfazer o compilador. |
| `framework.persistence.jdbc` | Framework JDBC próprio (não é um ORM): `DataSource` (pool nomeado), `pool/` (`ConnectionPool`, `PooledConnection`, `ConnectionWrapper`), `primitives/` (wrappers `JDBCConnection`/`JDBCStatement`/`JDBCPreparedStatement`/`JDBCCallableStatement`/`JDBCResultSet`/`JDBCMetaData` — excluídos do gate CPD por serem delegação forçada da própria API JDBC), `JDBCTransaction`, `JDBCProperties`, `JDBCRepository` (base abstrata dos adaptadores `*JDBCRepository`), `Results`. Consumido pelos adaptadores em `br-application` (`br.cdb.infra.persistence.*`). Ver `@docs/backend/persistence-jdbc.md`. |
| `framework.persistence.json` / `Storage` | `Storage` (porta) + `LocalFileStorage` (adaptador em arquivo) + `Repository<T, ID>` (CRUD genérico — `findAll`/`findById`/`save`/`deleteById`/`clearCache`) + `EntityDiff`. Apesar do nome do pacote, `Repository<T, ID>` é **agnóstica de tecnologia**: é reaproveitada como porta até por contextos com adaptador JDBC (ex.: `PersonRepository` em `br-context-people`). `Storage`/`LocalFileStorage` propriamente ditos seguem em uso só pela feature `Closing` e pelo catálogo global de centros de custo. |
| `framework.persistence.inmemory` | `InMemoryStorage` — implementação de `Storage` para testes. |
| `framework.serializer` | Leitor/escritor YAML (`YamlReader`, `YamlWriter`, `YamlNavigator`, `YamlEntry`, `YamlRoot`) por trás da fachada `br.commons.Yaml`. |
| `pdf` | `PdfTextExtractor` (porta) + `PdfBoxTextExtractor` (adaptador Apache PDFBox) + `ExtractionFailure` — usado pela importação de extrato (BTG/Santander) em `br-application`. |
| `platform` | Abstração de SO (`OS`, `FileSystem`, `Network`, `Terminal`) com providers `linux`/`windows` (`provider/windows` inclui bindings JNA para a Win32 API). `br.commons.Platform` é a fachada. |
| `tools` | `Meta` (reflexão — instanciação, stack frame via `RT.packages`, `exit`), `Strings`, `Parser`, `Threads` (`locked(...)`), `Tuple`, `chrono/` (`Dates`, `Time`). |
| `RT` | Estado de runtime global mínimo: `RT.running` e `RT.packages` (prefixos tratados como "framework" para `Meta` filtrar stack frames). |
| `validation` | `@TwoDecimalPlaces` — constraint Bean Validation (Hibernate Validator) para valores monetários. |

## Convenções

* Toda classe/interface `@NullMarked` (exceto `enum`) — ver `@docs/backend/null-safety.md`.
* `val` em variável local `final`-like — ver `@docs/backend/lombok.md` (e suas exceções).
* `record` para modelos de valor imutáveis; evitar `@Data`/`@Setter`/`@AllArgsConstructor`.
* Testes unitários em `src/test/java` (JUnit 5) cobrindo cada utilitário público (`ResultTest`, `MessageBusTest`, `MetaTest`, `DataSourceTransactionConcurrencyTest`, `ConnectionPoolConcurrencyTest`, etc.).

## Dependências (pom)

Empacotado como `jar` puro. Dependências próprias: `jackson-dataformat-yaml`, `jakarta.validation-api`, `juniversalchardet`, `pdfbox`; `h2` só em `test` (para exercitar o pool JDBC genérico contra um driver real). `slf4j-api`, `jspecify` e `lombok` (provided) vêm herdados de `br-parent` — não redeclare.
