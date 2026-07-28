# br-parent — POM Pai

Módulo Maven `parent` (`br.cdb:parent`, packaging `pom`). Sem código-fonte — só `pom.xml`. Centraliza versões, dependências herdadas e configuração de build/qualidade para **todos** os módulos (`br-commons`, `br-context-people`, `br-context-monetary`, `br-application`). Não crie `src/` aqui.

> Índice operacional deste módulo. Gotchas de build detalhados em `@docs/backend/quality-and-build.md` — leia antes de mexer em qualquer `<plugin>` deste pom.

## O que todo módulo herda automaticamente

Declarado em `<dependencies>` (não `<dependencyManagement>`) — por isso é herança **direta**, não apenas gerenciamento de versão:

* `slf4j-api`, `jspecify`, `lombok` (`provided`).
* Testes: `quarkus-junit`, `junit-jupiter-params`, `archunit-junit5`, `mockito-core` — inclusive em módulos sem runtime Quarkus (`br-commons`, `br-context-*`), porque o Surefire precisa do `LogManager` do JBoss mesmo ali.

Não redeclare essas dependências nos poms filhos.

`quarkus-bom` (versão em `quarkus.platform.version`) é importado em `<dependencyManagement>` — governa versão de qualquer artefato Quarkus que um filho declare, sem precisar fixar versão.

## Build compartilhado (roda em todo módulo, não só `br-application`)

* **Compilador**: Java 25 (`maven.compiler.target/source`), `-XDcompilePolicy=simple --should-stop=ifError=FLOW`, ErrorProne + NullAway (`-XepOpt:NullAway:AnnotatedPackages=br.cdb,br.commons`) plugados como `-Xplugin` do `javac`. Exige os `--add-exports` de `.mvn/jvm.config` — **só a JVM do Maven CLI os lê**; a IDE não, então "Make" pode falhar antes de rodar. Detalhes e o bug de codegen do javac 25.0.2 (`VerifyError`/`Lookup method resolution failed`) em `@docs/backend/quality-and-build.md`.
* **PMD/CPD** (`maven-pmd-plugin`, fase `verify`, `failOnViolation=true`): ruleset único em `config/pmd/ruleset.xml`, resolvido via `${maven.multiModuleProjectDirectory}` (funciona a partir de qualquer módulo). `pmd-core`/`pmd-java` sobrescritos para `7.13.0` + `asm 9.10.1` (bytecode Java 25, class major 69). `CyclomaticComplexity` falha método com CC > 7; duplicação via `CPD` (`minimumTokens=100`); exclui `**/jdbc/primitives/*.java` (delegação forçada da própria API JDBC, não cópia-cola real).
* **`quarkus-maven-plugin`** com goals `build`/`generate-code`/`generate-code-tests` está no `<plugins>` real (não `pluginManagement`) — roda em **todo** módulo filho, inclusive os que não são apps Quarkus (`br-commons`, `br-context-*`, packaging `jar` puro).
* **Enforcer**: Maven ≥ 3.9, Java = `maven.compiler.target`, sem versão duplicada de dependência entre poms.

## `<resources>` — aqui só `src/main/resources`; o SPA é problema do filho

Este pom declara **apenas**:
```xml
<resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>
</resource>
```

A cópia do frontend (`web/` → `META-INF/resources`, convenção servlet que faz o Quarkus servir a SPA como estático) **não mora mais aqui**: vive no `<build><resources>` de `br-application/pom.xml`, ancorada em `${maven.multiModuleProjectDirectory}/web`.

> Histórico: o bloco já esteve neste pom com `<directory>web</directory>` — caminho relativo resolvido contra o `basedir` de **cada** filho, e nenhum módulo tem `web/` próprio (só a raiz do repo), então não copiava arquivo nenhum. Se reintroduzir um `<resource>` compartilhado aqui, lembre que caminho relativo quebra dessa forma; use `${maven.multiModuleProjectDirectory}`.

**Cuidado ao mexer:** declarar `<resources>` num filho **substitui** (não soma) o bloco herdado. Por isso `br-application/pom.xml` repete `src/main/resources` junto do bloco do `web/` — remover essa repetição derruba `application.properties` do classpath.

## Empacotamento Docker

`docker-compose.yaml` (raiz) já aponta para os Dockerfiles reais — `br-application/src/main/docker/{Dockerfile.backend,Dockerfile.frontend}` com `context: .` (raiz do repo) — e `Dockerfile.backend` copia o reactor inteiro (`br-parent`, `br-commons`, `br-context-people`, `br-context-monetary`, `br-application`, `web`, `.mvn`) antes do `mvn package`.

⚠️ **Ainda não validado fim-a-fim.** O próprio `Dockerfile.backend` documenta que o `mvn package` dentro do container não resolveu o BOM do Quarkus no ambiente de build usado (mesmas coordenadas que `mvn verify` resolve fora) — indício de restrição de rede do sandbox, não do Dockerfile. Valide num Docker com rede irrestrita antes de depender disto em CI/produção.

## Módulos (mirror do `<modules>` da raiz)

`br-parent` (este) · `br-commons` · `br-context-people` · `br-context-monetary` · `br-application`. `web/` (frontend) não é módulo Maven — ver `@web/CLAUDE.md`.
