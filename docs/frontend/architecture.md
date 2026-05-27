# Architecture

Condutores arquiteturais independente de linguagem de programação.

## Estilo de Codificação

+ Favorecer composição em lugar de herança
+ Se um trecho de código é utilizado 2 ou mais vezes deve estar em um utilitário

### Parâmetros de Funções

A regra de limitação de parâmetros se aplica exclusivamente a **UseCases**:

+ UseCases com mais de 2 parâmetros devem receber um **DTO específico** (Command/Query)
+ Models de domínio (records) podem ter quantos campos forem necessários para representar a entidade
+ Services recebem parâmetros simples (primitivos, UUIDs, strings) sem limite rígido


## Padrões

### 001 - Vertical Slice Architecture 

+ Funcionalidades separadas em pacotes, podendo ser compostas de sub-pacotes.
+ Todo o código de negócio da funcionalidade deve estar em seu pacote
+ Comunicação entre **feature** e **context** deve ser feita pela **facade** do context

### 002 - Hexagonal Architecture

Ainda dentro do VSA (#001 - Vertical Slice Architecture) deve haver a separação do modelo de domínio
de suas portas de entrada/saída.

### 003 - Request Tracing

Ao enviar uma requisição para o backend o frontend deve incluir o cabeçalho X-request-id com um
valor "YYYYMMDD" + "HH24MMSS" + "micros com 5 caracteres" + "user name".

O backend deve incluir no log como primeiro registro da mensagem o conteúdo do cabeçalho x-request-id.