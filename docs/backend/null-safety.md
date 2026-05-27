# Null-Safety — Contrato de Nullability com JSpecify

Este documento descreve a política de null-safety adotada no projeto, baseada nas anotações do **JSpecify** e na exigência de `@NullMarked` em toda classe e interface do código-fonte.

---

## 1. O Problema do `null` em Java

`null` é a fonte de bugs mais comum em código Java. Sem uma convenção explícita, qualquer referência pode ser `null` a qualquer momento, e o compilador não emite nenhum aviso. O resultado são `NullPointerException`s em produção que poderiam ter sido detectadas em tempo de compilação.

### Problemas do status quo sem política

- **Ambiguidade de contrato**: a assinatura `User findById(String id)` não diz se pode retornar `null`.
- **Ausência de verificação estática**: o compilador aceita `user.getName()` sem qualquer garantia de que `user` não é `null`.
- **Proliferação de checagens defensivas**: `if (x != null)` espalhados pelo código sem motivo documentado.
- **Dificuldade em ferramentas de análise estática**: sem anotações, IntelliJ, NullAway e outros analisadores não conseguem raciocinar sobre nullability.

---

## 2. A Solução: JSpecify + `@NullMarked`

O projeto adota o [JSpecify](https://jspecify.dev/) como vocabulário único de nullability. A dependência já está declarada no `pom.xml`:

```xml
<dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
</dependency>
```

### Anotações relevantes

| Anotação | Onde usar | Significado |
|---|---|---|
| `@NullMarked` | Nível de pacote ou de tipo | Dentro deste escopo, toda referência é **non-null por padrão** |
| `@NullUnmarked` | Nível de tipo (exceção pontual) | Opt-out explícito — raramente necessário |
| `@Nullable` | Campo, parâmetro, retorno | Este valor **pode** ser `null` |
| `@NonNull` | Campo, parâmetro, retorno | Este valor **nunca** é `null` (redundante dentro de `@NullMarked`, mas aceito para clareza) |

---

## 3. Regra Central

> **Toda classe e toda interface devem estar anotadas com `@NullMarked`.**  
> A única exceção é `enum` — enums não carregam referências anuláveis e não precisam da anotação.

### Por que não usar `package-info.java`?

Anotar o pacote em `package-info.java` é possível, mas apresenta dois problemas práticos:

1. **Invisibilidade**: ao abrir uma classe, o desenvolvedor não vê imediatamente o contrato ativo — precisa encontrar o `package-info.java`.
2. **Risco de ausência**: um pacote novo criado sem `package-info.java` fica implicitamente sem contrato.

Anotar diretamente na declaração da classe torna o contrato **local, explícito e auditável** — a revisão de código detecta imediatamente uma classe sem `@NullMarked`.

---

## 4 Enums — exceção à regra

Enums **não recebem** `@NullMarked`. Não há referências anuláveis em constantes de enum, e a anotação não acrescenta valor:

```java
// correto — sem @NullMarked
public enum WorkloadKind {
    DEPLOYMENT,
    STATEFUL_SET,
    DAEMON_SET,
    JOB;
}
```

---

## 5. Interação com Lombok

Lombok gera código que o compilador vê como não-anotado. Para manter a compatibilidade, configure o `lombok.config` na raiz do módulo:

```properties
# lombok.config
lombok.addLombokGeneratedAnnotation = true
lombok.copyableAnnotations += org.jspecify.annotations.Nullable
lombok.copyableAnnotations += org.jspecify.annotations.NonNull
```

Com isso, campos anotados com `@Nullable` ou `@NonNull` têm as anotações propagadas para os métodos gerados (`getter`, construtor, `builder`).

---

## 6. Interação com NullAway (análise estática em build)

O projeto pode adotar **NullAway** como processador de anotações do Maven para verificar violações de null-safety em tempo de compilação (sem custo em runtime).

Configuração mínima no `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>com.uber.nullaway</groupId>
                <artifactId>nullaway</artifactId>
                <version>${nullaway.version}</version>
            </path>
            <path>
                <groupId>com.google.errorprone</groupId>
                <artifactId>error_prone_core</artifactId>
                <version>${errorprone.version}</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-XepOpt:NullAway:AnnotatedPackages=br.com.funpresp.k8sole</arg>
            <arg>-Xplugin:ErrorProne</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Quando NullAway está ativo, uma violação como a abaixo gera **erro de compilação**:

```java
@NullMarked
class Example {
    String process(@Nullable String input) {
        return input.toUpperCase(); // ERRO: input pode ser null
    }
}
```

---

## 7. Checklist para cada nova classe

Antes de submeter código para revisão, confirme:

- [ ] A declaração da classe ou interface começa com `@NullMarked` (acima de `@Component`, `@Service`, etc., se houver).
- [ ] Parâmetros e retornos que **podem** ser `null` estão anotados com `@Nullable`.
- [ ] Parâmetros e retornos que **nunca** podem ser `null` não têm anotação (o padrão non-null cobre).
- [ ] Nenhuma verificação defensiva `if (x != null)` existe sem que `x` seja `@Nullable` na assinatura.
- [ ] Enums **não** têm `@NullMarked`.

---

## 8. Integração com ArchUnit

O projeto usa ArchUnit para impor regras arquiteturais. A regra de `@NullMarked` pode ser codificada como teste:

```java
@ArchTest
static final ArchRule nullMarkedOnAllTypes =
    classes()
        .that().areNotEnums()
        .and().resideInAPackage("br..")
        .should().beAnnotatedWith(NullMarked.class)
        .because("todo tipo deve declarar explicitamente seu contrato de nullability");
```

Isso garante que a ausência de `@NullMarked` quebre o build de testes, tornando a regra **não-opt-out** na prática.

---

> [!IMPORTANT]
> `@NullMarked` não gera verificação em runtime por si só — é uma anotação de metadados para ferramentas (compilador, IDEs, analisadores estáticos). A proteção real vem da combinação de `@NullMarked` + NullAway (build-time) + ArchUnit (test-time).

> [!TIP]
> Quando um valor externo pode ser `null` (JSON desserializado, resultado de Backend Kubernetes, valor de ConfigMap), anote-o com `@Nullable` na fronteira (adapter) e trate o caso antes de passar ao domínio. O domínio deve receber sempre valores non-null ou `Optional<T>`.
