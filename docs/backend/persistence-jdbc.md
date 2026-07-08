# Persistência (JDBC / H2)

As portas de repositório do domínio/feature são implementadas por adaptadores JDBC sobre H2.

> Pré-requisitos: [Padrão Result](result-pattern.md) (desembrulho com `.get()` e falha fatal), [Null-Safety](null-safety.md) e [Lombok](lombok.md) (`@Nullable val` não compila).

## 1. Visão geral

- As portas (`*Repository` no domínio/feature) são implementadas por `*JDBCRepository` em `br.community.infra.persistence`, estendendo `JDBCRepository<T>`.
- Os adaptadores usam o `DataSource` H2 publicado no `br.commons.Registry`; os primitivos JDBC ficam em `br.commons.framework.persistence.jdbc`.
- O schema (DDL) vive em `Database`: tabelas planas; enums como string referenciando tabelas de lookup (`MON_ACCOUNT_TYPE` / `TRANSACTION_NATURE` / `MON_STATUS`) via FK; mapas livres (`additionalInfo` / `preferences`) em coluna JSON. As FKs conformam a **todas** as relações dos diagramas Mermaid — além das lookups, as tabelas de dados referenciam-se entre si, inclusive entre contextos (ex.: `SEC_USER→PEP_PERSON`, `USER_TRANSACTION→MON_TRANSACTION`); por isso a ordem de criação (`model()`) e de limpeza (`reset()`) respeita a dependência pai→filho.
- Nem tudo é JDBC: a feature `Closing` e o catálogo global de centros de custo ainda persistem em JSON (`Storage` / `LocalFileStorage`).

## 2. O schema canônico são os diagramas Mermaid

Os diagramas ER em `docs/db-ctx-people.mermaid`, `docs/db-ctx-monetary.mermaid` e `docs/db-features.mermaid` são a **fonte da verdade** do schema. Quando o código/DDL (`Database`) divergir do diagrama, **o código está errado** — conformar o código ao diagrama, nunca o contrário.

- Tabela/coluna presente no código mas **ausente** do diagrama → remover o código (caso típico: repositório órfão fazendo SQL numa tabela que o schema já removeu).
- Nunca ressuscitar DDL para casar com código órfão.

## 3. Banco de desenvolvimento é file-based — ciclo de vida

| Perfil | URL | Onde |
|---|---|---|
| dev | `jdbc:h2:file:./database;DB_CLOSE_DELAY=-1` | `src/main/resources/application.properties` |
| test | `jdbc:h2:mem:cdb;DB_CLOSE_DELAY=-1` | `src/test/resources/application-test.properties` |

O dev usa um **arquivo** (`database.mv.db` na raiz, fora do git via `.gitignore`). `Database` emite `CREATE TABLE` **sem** `IF NOT EXISTS`: num banco que já existe os `CREATE` falham ("Table already exists", só logado) e **colunas novas nunca são adicionadas**. No primeiro `SELECT` da coluna nova → `Column "X" not found` → tratado como fatal → `Meta.exit(99)` mata o fork do surefire ("Process Exit Code: 99", `Tests run: 0`).

**Ao mudar o DDL:** apagar `database.mv.db` e `database.trace.db` na raiz do projeto antes de rodar a aplicação. O perfil de teste usa `mem` (schema novo a cada run, sem esse problema). Reports antigos do surefire persistem e podem parecer "passando" — confirme pelo `Tests run` do run atual. Diagnóstico real com `-DforkCount=0` (exceção in-process, em vez de `exit(99)` no fork).

## 4. Colunas anuláveis nos mappers `map(rs)`

Colunas **sem** `NOT NULL` no `Database` (`COD_CATEGORY`, `COD_PARENT`, `DAT_PAYMENT`, `COD_LINKED_ACCOUNT`, `TXT_ADDITIONAL_INFO`, …) lidas via `rs.getString(col).get()` retornam `null` para SQL NULL. Chamar `.isBlank()` / `.toLocalDate()` / `UUID.fromString()` direto no null gera NPE → o `Result.get()` que embrulha vira `Logger.fatal` → `Meta.exit(99)` mata o fork (sem report de teste).

Guarde a leitura anulável (note `final @Nullable`, **não** `@Nullable val` — ver [Lombok §6.4](lombok.md)):
```java
final @Nullable String categoryRaw = rs.getString("COD_CATEGORY").get();
final @Nullable UUID categoryId =
        (categoryRaw == null || categoryRaw.isBlank()) ? null : UUID.fromString(categoryRaw);
```
Colunas `NOT NULL` (timestamps `TMS_CREATE_AT`, etc.) são seguras: `rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime()`. Padrão real em `UserTransactionJDBCRepository.map`.

## 5. Migrações automáticas one-shot

Como o dev é file-based e `Database` nunca evolui schema existente (§3), toda mudança de schema que precise transformar dados já persistidos (não só DDL novo aditivo) ganha uma classe `*Migration` própria em `br.community.infra.persistence`, chamada em sequência em `ContextBridge.dataSource(...)` **antes** do loop de `Database.model()`:

```java
LegacyCardMigration.apply(datasource);
AccountLimitMigration.apply(datasource);
FeatureSchemaMigration.apply(datasource);
```

Cada uma é independentemente idempotente (detecção própria via `INFORMATION_SCHEMA`) — a ordem importa só porque um banco de dev muito antigo passa por todas em sequência na mesma inicialização; um banco já em dia pula todas.

### 5.1 `LegacyCardMigration`

Remodelagem do cartão: de `MON_ACCOUNT` tipo `CREDIT_CARD` para entidade própria `MON_CARD` (+ limites, na época, em `MON_ACCOUNT_LIMIT` — ver §5.2 para onde os limites foram depois).

- **Detecção**: `SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='USER_ACCOUNT' AND COLUMN_NAME='TXT_CARD_LAST4'`. `0` → banco novo ou já migrado → não faz nada (idempotente).
- **Backup**: `BACKUP TO './database-pre-card-remodel.zip'` (backup online do H2, seguro com o arquivo aberto) antes de qualquer alteração — pulado em bancos em memória (`:mem:`, perfil de teste), que `BACKUP TO` rejeita por não serem persistentes. Falha de backup num banco real é fatal por design (mesma convenção de `Result.get()`/infraestrutura do restante do projeto — ver [Result Pattern](result-pattern.md)): a migração aborta em vez de prosseguir sem rede de segurança.
- **Dados**: por conta-cartão, resolve a conta real (`COD_LINKED_ACCOUNT` ou a própria, convertida para `CHECKING`), migra last4 para `MON_CARD`, funde limites/dias em `MON_ACCOUNT_LIMIT` (múltiplos overlays no mesmo alvo → `MAX` nos limites, primeiro dia não-nulo), reaponta `MON_TRANSACTION` para a conta real com `COD_CARD` preenchido, e descarta os `USER_ACCOUNT_BALANCE` da conta-cartão apagada e da conta real que recebeu transações (o listener de eventos recalcula no próximo lançamento).
- **Corte final**: remove as 6 colunas legadas de `USER_ACCOUNT` e o seed do tipo `CREDIT_CARD` em `MON_ACCOUNT_TYPE`.

Teste `LegacyCardMigrationTest` cobre o cenário (cartão vinculado + cartão sem vínculo + fusão de limites + idempotência) contra um H2 em memória isolado com o schema legado montado à mão — não depende do arquivo de dev real.

### 5.2 `AccountLimitMigration`

Follow-up da §5.1: `MON_ACCOUNT_LIMIT` (linha própria por conta) deixou de existir — os 4 campos viram colunas direto em `MON_ACCOUNT`, já que a relação sempre foi 1:1.

- **Detecção**: tabela `MON_ACCOUNT_LIMIT` existe → migra; ausente → no-op.
- **Backup**: mesmo padrão (`./database-pre-account-limit-merge.zip`, skip em `:mem:`).
- **Dados**: `ALTER TABLE MON_ACCOUNT ADD` as 4 colunas (nullable); cópia 1:1 por `COD_ACCOUNT` (sem acumulador — ao contrário da §5.1, aqui não há múltiplas contas colidindo no mesmo destino); `DROP TABLE MON_ACCOUNT_LIMIT`.

Teste `AccountLimitMigrationTest`, mesmo molde (schema legado à mão em H2 isolado, idempotência na 2ª chamada).

### 5.3 `FeatureSchemaMigration`

Quatro tabelas de `docs/db-features.mermaid` que evoluíram juntas: `SEC_USER` ganha `FLG_ACTIVE`/timestamps (usuários existentes viram ativos); `USER_ACCOUNT` perde `FLG_ACTIVE` (redundante com `MON_ACCOUNT.FLG_ACTIVE`) e `DEC_OPENING_BALANCE`; `USER_CATEGORY.BOL_SYSTEM` vira `FLG_SYSTEM` (só rename); `USER_TRANSACTION` ganha `COD_ACCOUNT` e troca a PK de `(COD_TRANSACTION, COD_USER)` para `(COD_USER, COD_ACCOUNT, COD_TRANSACTION)`.

- **Detecção**: ausência de `SEC_USER.FLG_ACTIVE` → migra; presente → no-op.
- **Backup**: mesmo padrão (`./database-pre-feature-schema.zip`, skip em `:mem:`).
- **Saldo inicial → transação sintética**: antes de derrubar `DEC_OPENING_BALANCE`, cada conta com saldo inicial ≠ 0 ganha uma `MON_TRANSACTION` ("Saldo inicial", data-âncora `2000-01-01`, status `CONFIRMED`, centro de custo "Fixo", sem categoria) — preserva o valor histórico sem inventar um substituto pro conceito de saldo de abertura; `AccountResponse.currentBalance` passa a ser sempre a soma pura das transações da conta.
- **`USER_TRANSACTION.COD_ACCOUNT`**: retropreenchido via subquery correlacionada em `MON_TRANSACTION` (`WHERE ID = COD_TRANSACTION`) antes da PK trocar.

Teste `FeatureSchemaMigrationTest`, mesmo molde, cobrindo as 4 tabelas + idempotência.
