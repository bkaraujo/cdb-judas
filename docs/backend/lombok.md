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
