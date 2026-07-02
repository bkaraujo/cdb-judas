# Qualidade & Build

Gotchas não-óbvios do build deste projeto (Java 25 + Maven). Custam horas para re-descobrir.

## 1. Quality gate (PMD / CPD)

`config/pmd/ruleset.xml` + `maven-pmd-plugin` no `pom.xml`, ligado à fase `verify` e **enforcing** (`failOnViolation=true`): o build falha em complexidade ciclomática por método > 7, método longo ou duplicação.

- **PMD precisa ser forçado para Java 25.** O PMD/ASM empacotado rejeita bytecode `class major 69` → `Unsupported class file major version 69` e depois `StackOverflowError`. Fix no pom: `maven-pmd-plugin 3.28.0` + `<dependencies>` do plugin sobrescrevendo `pmd-core` / `pmd-java` para `7.13.0` e `org.ow2.asm:asm:9.10.1`.
- **PMD 7 não expõe contagem de linhas em XPath** (`@BeginLine` / `@EndLine` foram removidos; referenciá-los lança no Saxon e corrompe o report). O gate de "função longa" usa **`NcssCount`** (sentenças, `methodReportLevel=30`), não linhas cruas.
- `CyclomaticComplexity`: `methodReportLevel=8` (falha CC > 7), `classReportLevel=9999` (só a CC por método é gated; classes de delegação ampla não contam como "complexas").
- `targetJdk=21` (o código-fonte não usa sintaxe > 21; o override do ASM cobre o bytecode 25). CPD: `minimumTokens=100`.

## 2. CLI `mvn test` quebra ao compilar os testes

`mvn test` / `test-compile` **não** roda o processador do Lombok nos fontes de **teste**: todo `val` em `src/test` falha com `lombok.val cannot be converted to ...`. O **main** compila bem (`mvn compile` verde) — só o test-compile quebra. Reproduzido idêntico num worktree `/tmp` limpo → é gap de config (`maven-compiler-plugin` `testAnnotationProcessorPaths`), **não** o IDE.

Para verificar o backend sem o test-compile, rode um launcher single-file contra as classes já compiladas:
1. `mvn -q compile` (lombok do main funciona).
2. Escreva um `Prog.java` em Java puro (sem lombok) com `main()` exercitando as classes.
3. `java -cp "target/classes:<h2.jar>:<jspecify.jar>" Prog.java` (single-file source launch do Java 25). H2 em `~/.m2/.../h2-*.jar`.

Mantenha os testes JUnit no repo mesmo assim — eles rodam onde o test-compile do lombok funciona (IDE).

## 3. Verificar um pacote sem mvn (standalone javac)

Compile o pacote alterado contra `target/classes`:
```bash
~/.jdks/<jdk25>/bin/javac -proc:full -d <out> \
  -cp "target/classes:<jspecify.jar>:<lombok.jar>" <pkg>/*.java
```
JDK 23+ só roda processadores de anotação com `-proc:full` (ou `-proc:only`); sem isso o lombok nunca desugara `val` → "cannot find symbol class val". O jar do lombok no classpath fornece o processador; o do jspecify resolve `@NullMarked`. Jars em `~/.m2/repository`. Isso **não** roda NullAway/ErrorProne — é só type-check, não o gate de null-safety.

## 4. Run/debug pela IntelliJ (específico de ambiente)

> Os caminhos abaixo são da máquina do autor; ajuste aos seus.

ErrorProne + NullAway rodam como `-Xplugin` do javac e exigem os `--add-exports=jdk.compiler/...` que vivem em `.mvn/jvm.config` — **só a JVM do Maven CLI os lê**. A IDE compila com compilador próprio, sem esses flags, então o passo "Make" pode falhar antes de subir a aplicação.

A causa de runtime mais traiçoeira, porém, foi um **bug de codegen do javac 25.0.2**: ele emite stackmap frames inválidos em certos `switch` sobre record pattern → `VerifyError` ao carregar a classe (a mensagem exata do wrapper varia por framework — sob Spring aparecia como `Lookup method resolution failed`; sob Quarkus/Arc não confirmado, mas a causa raiz é a mesma). O CLI sempre funcionou porque usa **JDK 25.0.3** (`/usr/lib/jvm/java-25-openjdk-amd64`); a IDE apontava para `25.0.2`.

**Fix:** apontar a IntelliJ para o JDK **25.0.3** (Project Structure → SDKs → Project SDK + JRE da run config). O lint (ErrorProne/NullAway/PMD) continua barrando só no CLI `mvn verify`.
