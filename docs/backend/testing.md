# Testes (backend)

A suíte de `br.cdb` tem **exatamente três tipos de teste**. Não existe teste de unidade de classe
interna: parser, expansor de parcelas, casador de cartão, assinatura de grupo, serviço — nenhum deles
tem teste próprio. Cada um é verificado pelo que a borda expõe. A única exceção é `f999` (ver abaixo).

| Tipo | Nome/forma | O que exercita | Base |
|---|---|---|---|
| **1. Use case** | `*UseCaseTest` / `*UseCasesTest` | o par CQRS da fatia, com repositórios em memória | `AbstractUseCaseTest` |
| **2. Interface Web** | qualquer classe que herde `BaseHttpTest` | a fatia inteira pela rota HTTP real, contra H2 in-memory | `BaseHttpTest` (`@QuarkusTest` + RestAssured) |
| **3. Arquitetura** | `*ArchitectureTest` | as regras de pacote/dependência, via ArchUnit | — |

Classes de apoio (`AbstractUseCaseTest`, `BaseHttpTest`, `AbstractProfileTest`, `AbstractImportTest`,
`InMemoryRepositories`, `PdfFixtures`) não declaram `@Test` e por isso não são teste de tipo nenhum.

## Por quê

Um teste que constrói `new BTGInvoiceParser()` congela um detalhe de implementação: mover a lógica de
lugar quebra o teste sem que nada tenha regredido para o usuário. Em contrapartida um `POST` no
endpoint de preview afirma a mesma coisa (a linha `(9/10)15 Jul` vira dez parcelas com a data original
certa) sem citar nenhuma classe interna — o mesmo motivo pelo qual a fatia expõe use case e Resource, e
não os seus serviços.

O corolário costuma ser que **comportamento inalcançável pela borda é código morto**. Foi assim que se
descobriu o `CategoryGuesser`: `StatementImportProcessor.preview` sempre lhe passava histórico vazio, a
sugestão de categoria nunca saía no preview, e a classe foi apagada (o `categoryId` da linha de preview
continua no contrato, sempre nulo — é o campo que a tela preenche quando o usuário escolhe).

## Exceção: `f999`

O composition root não tem borda nenhuma — nem rota HTTP nem use case — e ainda assim carrega
comportamento crítico que só roda em processo: a fila de retry de exclusão
(`DeletionQueueService.runOnce()`, chamada pelo `@Scheduled`). Aí o corolário acima não vale: não é
código morto, é código sem porta de fora. **Teste de unidade em `f999` é permitido** e a trava o isenta
por pacote. Não é licença para voltar a testar classe interna de fatia que tem borda.

## Trava

`br.cdb.TestSuiteArchitectureTest` (ArchUnit, importando só as classes de teste) mantém o formato:

1. toda classe com método `@Test` é `*UseCaseTest`/`*UseCasesTest`, herda `BaseHttpTest`, ou é
   `*ArchitectureTest` — salvo em `..feature.f999..`, isento por não ter borda;
2. teste que herda `BaseHttpTest` não pode `@Inject` um bean de `.._1_application..` — teste de
   interface Web fala com a fatia por HTTP, e injetar o serviço é voltar a testá-lo por dentro.

É trava **estrutural**: pega classe de teste fora do formato e teste Web que contorna o HTTP, mas não
sabe dizer se um teste Web de fato chama uma rota.

## Importação de extrato/fatura pela borda

O endpoint de import (`POST /api/{uuid}/accounts/transactions/import/preview`) só aceita PDF, e os
fixtures anonimizados do repositório (`src/test/resources/faturas`, `.../extratos`) são texto — o que
o extrator produziria a partir do PDF do banco. `br.cdb.PdfFixtures` fecha esse vão rerenderizando o
texto em PDF:

```java
preview(fixturePdf("/faturas/fatura-btg-abril.txt"))
        .statusCode(200)
        .body("last4s", contains("0020", "9822"));
```

O round-trip é fiel linha a linha, **inclusive espaços à esquerda** (o extrato Santander marca linha de
continuação com eles). Isso depende da geometria do PDF gerado, não é acidente: a página é larga o
bastante (2000pt) para nenhuma linha de extrato quebrar, e o texto é paginado a 76 linhas para nada
ficar desenhado abaixo da margem. Linha em branco é a única diferença — some, e todo parser já pula
linha vazia antes de casar.

`PdfFixtures` também fabrica os PDFs que os fixtures de texto não conseguem representar: cifrado
(`encrypted`), sem camada de texto (`imageOnly`) e com páginas demais (`withText(texto, 51)`) — os três
caminhos de erro do extrator, verificados pelo código que sai na resposta (`PASSWORD_REQUIRED`,
`WRONG_PASSWORD`, `NO_TEXT_LAYER`, `TOO_MANY_PAGES`).

Onde o comportamento depende do relógio (status `confirmed` × `scheduled` é decidido por mês corrente),
o teste monta o documento com o período calculado na hora — `btgInvoice(YearMonth.now(), …)` — em vez de
fixar uma data que envelhece.

## Fora desta política

`br-commons` é biblioteca de framework: não tem use case nem interface Web, e os seus testes
(concorrência do pool JDBC, propagação de transação, logger, YAML, `Result`, `MessageBus`) continuam
sendo testes de unidade comuns. A regra dos três tipos vale para `br.cdb`.
