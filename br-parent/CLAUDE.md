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

## ⚠️ `<resources>` do SPA — hoje não copia nada

O bloco:
```xml
<resource>
    <directory>web</directory>
    <targetPath>META-INF/resources</targetPath>
    <excludes><exclude>CLAUDE.md</exclude></excludes>
</resource>
```
tem a intenção de empacotar o frontend (`web/`, na raiz do repo) no classpath do backend para o Quarkus servir como estático (`META-INF/resources`, convenção servlet) — comentário no próprio pom confirma essa intenção, e `Dockerfile.frontend` assume o mesmo ("Mesma fonte que o backend empacota via `<resource>` do pom"). **Mas** `<directory>web</directory>` é um caminho relativo resolvido contra o `basedir` de **cada módulo filho** — e nenhum módulo (`br-application` incluído) tem um subdiretório `web/` próprio; só a raiz do repo tem. Build verificado (`br-application/target/classes` sem `META-INF/resources`, sem `index.html` em lugar nenhum do `target/`) confirma que **hoje esse bloco não copia arquivo nenhum, em nenhum módulo**.

Provável resíduo de quando `web/` ficava lado a lado de `src/` num layout single-module, antes do split em `br-parent`/`br-commons`/`br-context-*`/`br-application`. Na prática, quem serve o frontend hoje é só o container `frontend` do `docker-compose.yaml` (nginx, `Dockerfile.frontend`, `COPY web/ /usr/share/nginx/html/` a partir da raiz do repo) — o caminho "backend serve o próprio SPA" descrito no `@CLAUDE.md` raiz e no `README.md` **não está em vigor no build atual**. Se for pra valer, o fix é apontar o `<directory>` para `${maven.multiModuleProjectDirectory}/web` (só teria efeito relevante em `br-application`, que é quem tem `META-INF/resources` servido pelo Quarkus).

## ⚠️ `docker-compose.yaml` aponta para Dockerfile inexistente

`docker-compose.yaml` (raiz) referencia `dockerfile: src/main/docker/Dockerfile.backend` com `context: .` (raiz do repo) — mas não há `src/` na raiz; os Dockerfiles reais estão em `br-application/src/main/docker/{Dockerfile.backend,Dockerfile.frontend}`. Além disso, `Dockerfile.backend` faz `COPY pom.xml .` + `COPY src ./src` + `mvn package` assumindo layout single-module — não copia `br-parent/`, `br-commons/`, `br-context-people/`, `br-context-monetary/`, então o build do reactor multi-módulo falharia mesmo com o caminho corrigido. Nenhum destes dois pontos foi validado fim-a-fim desde o split em módulos; não assuma que `docker-compose up` funciona sem antes corrigir e testar.

## Módulos (mirror do `<modules>` da raiz)

`br-parent` (este) · `br-commons` · `br-context-people` · `br-context-monetary` · `br-application`. `web/` (frontend) não é módulo Maven — ver `@web/CLAUDE.md`.
