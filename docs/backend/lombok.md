# Lombok

Este documento descreve o padrão de uso da biblioteca Lombok no projeto.

## 1. `val`
Em toda variável local que pode ser tratada como `final`, deve-se utilizar a keyword `val` do Lombok em vez de declarar o tipo explicitamente.
- Reduz o ruído visual e aumenta a legibilidade.

Exemplo:
```java
val account = accountRepository.findById(id);
```

## 2. `@RequiredArgsConstructor`
Utilizado para injeção de dependências via construtor, especialmente em classes como Services, Resources e UseCases que declaram suas dependências como `private final`.
- Evita a necessidade de criar construtores manualmente ou usar o `@Autowired` nos atributos.

Exemplo:
```java
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository repository;
}
```

## 3. Outras anotações suportadas
- `@Getter`: Para gerar getters automaticamente, quando necessário.
- `@Builder`: Para simplificar a construção de objetos complexos com múltiplos atributos.
- `@AccessLevel`: Para definir restrições de visibilidade em anotações (ex: `@RequiredArgsConstructor(access = AccessLevel.PACKAGE)`).

## 4. O que evitar
- Evitar o uso de `@Data`, `@Setter` ou `@AllArgsConstructor` no modelo de domínio de negócio.
- O projeto adota `record` (Java 14+) como o padrão principal para modelos de domínio, entidades e DTOs, por fornecer imutabilidade nativamente.

## 5. Integração com Null-Safety (JSpecify)
Para garantir a compatibilidade com a política de Null-Safety baseada em JSpecify (descrita em `null-safety.md`), o projeto utiliza a configuração no `lombok.config` para propagar as anotações nos métodos gerados:
```properties
lombok.addLombokGeneratedAnnotation = true
lombok.copyableAnnotations += org.jspecify.annotations.Nullable
lombok.copyableAnnotations += org.jspecify.annotations.NonNull
```

---

## 6. Exceções ao `val` — manter tipo explícito

A regra de usar `val` em todo `final` tem exceções: como `val` infere o tipo do inicializador, alguns casos **quebram** a compilação ou degradam a null-safety e exigem `final <Tipo>` explícito.

### 6.1 Retornos genéricos com target-type
Métodos genéricos cujo tipo é resolvido pelo destino (`<T> T readValue(...)`, `convertValue`, `Collections.emptyList()`) inferem `Object` sob `val` — o uso seguinte (`.stream()`, etc.) falha com `cannot find symbol`. Manter o tipo:
```java
final List<MonetaryTransaction> parsed = mapper.readValue(bytes, listType);
```

### 6.2 Primitivo a partir de retorno/fábrica boxed
Primitivo lido de um retorno `@Nullable Integer` (`line.installmentTotal()`) ou de uma fábrica boxed (`Integer.valueOf(s, 16)`) infere `Integer` sob `val` — muda `int`→`Integer`, quebra comparações `==` e espalha avisos de unboxing do NullAway. Manter `final int`:
```java
final int total = line.installmentTotal();
```

### 6.3 Diamantes sem tipo de elemento
`new ArrayList<>()`, `new HashSet<>()`, `new ArrayList<>(n)` (sem argumento que carregue o tipo) inferem `<Object>` sob `val`. Preencher o elemento antes de converter (`new ArrayList<Element>()`). Diamantes com argumento tipado (`new ArrayList<>(readAll())`) são ok.

### 6.4 `@Nullable val` não compila
`@Nullable` (JSpecify) é `TYPE_USE`; antes de `val` ela cai em posição de declaração → *"annotation interface not applicable"*. Para um local anulável, usar `final @Nullable Type`:
```java
final @Nullable String raw = rs.getString("COD_CATEGORY").get();
```

### 6.5 Onde `val` não é válido
Sem inicializador (`String x;` atribuído em ramos), local reatribuído (acumuladores, walkers `klass = klass.getSuperclass()`), `catch (...)` e parâmetros de método. **Válido** em for-each (`for (val x : coll)`). Cada arquivo precisa do próprio `import lombok.val;` (sem star import). Convenção do repo: `var` apenas para mutáveis e `val` para finais — um `var` encontrado quase sempre é reatribuído; confirme antes de "promover" a `val`.
