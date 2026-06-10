# API Web — Exemplos de Request/Response JSON

Este documento cataloga os contratos de dados (JSON) da API Web do sistema, organizados por recurso. Os contratos foram extraídos diretamente dos `*Resource` (Spring) e seus DTOs.

---

## 0. Convenções e Autenticação

### Login e token rotativo
- **Endpoint:** `POST /login`
- **Request:**
```json
{
  "username": "usuario",
  "password": "senha"
}
```
- **Response (200 OK):** corpo vazio. Credenciais retornam em **headers**:
  - `X-Access-Token`: token de acesso a enviar nas próximas requisições.
  - `X-User-Id`: identificador do usuário, usado no segmento `{uuid}` das rotas.
- **Response (401 Unauthorized):** usuário inexistente ou senha inválida.

### Rotas com namespace de usuário
A maioria dos recursos vive sob `/api/{uuid}/...`, onde **`{uuid}` é o `X-User-Id`** retornado no login.

- Toda requisição (exceto `/login` e estáticos) envia o header `X-Access-Token`.
- O token é **rotativo**: a cada requisição (não-SSE) bem-sucedida o servidor devolve um novo `X-Access-Token` no header da resposta — o cliente deve substituir o token armazenado. O fluxo SSE valida sem rotacionar.
- **Guarda de propriedade** (`OwnershipInterceptor`): se o `{uuid}` da rota não corresponder ao usuário autenticado → `403 Forbidden`. Sem autenticação válida → `401 Unauthorized`.

### Rotas globais (sem `{uuid}`)
`POST /login`, `GET /api/me`, `GET /api/version`, `GET /api/cost-center`.

### Formatos
- Datas: ISO `yyyy-MM-dd`. Período mensal em path/param: `yyyyMM` (ex.: `202403`); `YearMonth` no corpo de resposta serializa como `"2024-03"`.
- Valores monetários: decimais com 2 casas (`@TwoDecimalPlaces`).
- Cores: `#RRGGBB` (regex `#[0-9A-Fa-f]{6}`).

---

## 1. Perfil / Self (`/api/me`)

### Obter usuário atual
- **Endpoint:** `GET /api/me`
- **Response (200 OK):**
```json
{
  "id": "u-123",
  "username": "usuario",
  "name": "Bruno",
  "preferences": {
    "theme": "dark",
    "language": "pt-BR",
    "locale": "pt-BR",
    "sidebarCollapsed": false
  }
}
```
> `name` pode ser `null`. `theme` pode ser `null` (não definido → cliente usa tema local/sistema).

### Atualizar (PATCH parcial)
- **Endpoint:** `PATCH /api/me`
- **Request:** campos ausentes/`null` não são alterados (merge):
```json
{
  "name": "Bruno Araújo",
  "preferences": {
    "theme": "light",
    "sidebarCollapsed": true
  }
}
```
- **Response (200 OK):** mesmo formato do `GET /api/me`.

---

## 2. Contas (`/api/{uuid}/accounts`)

`type` ∈ `CHECKING` | `INVESTMENT` | `CREDIT_CARD`.

### Listar Todas
- **Endpoint:** `GET /api/{uuid}/accounts`
- **Query opcional:** `?type=card` (ou `credit-card`) filtra apenas cartões de crédito.
- **Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Conta Corrente",
    "balance": 1250.50,
    "type": "CHECKING",
    "color": "#007AFF",
    "active": true,
    "linkedAccountId": null,
    "additionalInfo": {
      "bank": "Banco do Brasil"
    },
    "currentBalance": 1100.25
  }
]
```
> `balance` é o saldo de abertura; `currentBalance` = abertura + soma das transações da conta. Cartões carregam dados extras em `additionalInfo` (ex.: `last4`).

### Buscar por ID
- **Endpoint:** `GET /api/{uuid}/accounts/{id}`
- **Response (200 OK):** um objeto `Account` (mesmo formato acima).

### Criar Conta
- **Endpoint:** `POST /api/{uuid}/accounts`
- **Response:** `201 Created` com o `Account`.
- **Request:**
```json
{
  "name": "Nova Conta",
  "balance": 100.00,
  "type": "CHECKING",
  "color": "#FF5733",
  "active": true,
  "linkedAccountId": null,
  "additionalInfo": {}
}
```
> Obrigatórios: `name`, `balance`, `type`, `color`. `linkedAccountId` e `additionalInfo` são opcionais.

### Atualizar Conta
- **Endpoint:** `PATCH /api/{uuid}/accounts/{id}` — mesma estrutura do POST.

### Excluir Conta
- **Endpoint:** `DELETE /api/{uuid}/accounts/{id}` → `204 No Content`.

### Saldo Mensal/Anual
- **Endpoint:** `GET /api/{uuid}/accounts/{id}/balance?period=202401` ou `?year=2024`
- **Response (200 OK — `period`):**
```json
{
  "id": "uuid-balance",
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "period": "2024-01",
  "balance": 5000.00
}
```
- **Response (200 OK — `year`):** lista de objetos acima (um por mês).
- Sem `period` nem `year` → `422`.

---

## 3. Transações (`/api/{uuid}/accounts/...`)

`status` ∈ `"confirmed"` | `"scheduled"`. `type` ∈ `"income"` | `"expense"`.

### Listar (todas as contas)
- **Endpoint:** `GET /api/{uuid}/accounts/transactions`
- **Query opcional:** `limit`, `dateFrom`, `dateTo` (ISO), `status`, `type`.
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-trans",
    "description": "Mercado",
    "amount": -150.25,
    "date": "2024-03-15",
    "categoryId": "uuid-category",
    "accountId": "uuid-account",
    "status": "confirmed",
    "type": "expense",
    "costCenterId": "d0000000-0000-0000-0000-000000000001",
    "paymentDate": "2024-03-15",
    "groupId": null,
    "installmentNumber": null,
    "totalInstallments": null,
    "notes": null
  }
]
```

### Listar (por conta)
- **Endpoint:** `GET /api/{uuid}/accounts/{accId}/transactions` — mesmas queries e formato de resposta.

### Criar Transação
- **Endpoint:** `POST /api/{uuid}/accounts/{accId}/transactions`
- **Response:** `201 Created` com a `Transaction`.
- **Request:**
```json
{
  "description": "Pagamento Aluguel",
  "amount": 2500.00,
  "date": "2024-04-01",
  "categoryId": "uuid-category",
  "accountId": null,
  "costCenterId": "d0000000-0000-0000-0000-000000000001",
  "status": "scheduled",
  "type": "expense",
  "installments": 1,
  "editMode": "single",
  "deleteMode": null,
  "notes": "opcional, máx 250 chars"
}
```
> Obrigatórios: `description`, `amount`, `date`, `categoryId`, `costCenterId`, `status`, `type`. `accountId` no corpo é opcional (a conta vem do path).

### Atualizar Transação
- **Endpoint:** `PATCH /api/{uuid}/accounts/{accId}/transactions/{txId}` — mesmo corpo do POST.

### Atualizar Status
- **Endpoint:** `PATCH /api/{uuid}/accounts/{accId}/transactions/{txId}/status`
- **Request:**
```json
{
  "status": "confirmed",
  "paymentDate": "2024-04-02"
}
```

### Excluir Transação
- **Endpoint:** `DELETE /api/{uuid}/accounts/{accId}/transactions/{txId}?mode=single` → `204 No Content`.
> `mode` (opcional) controla parceladas/recorrentes (ex.: `single`, `future`, `all`).

### Transferência entre contas
- **Endpoint:** `POST /api/{uuid}/accounts/transactions/transfer`
- **Response:** `201 Created` com a `Transaction` gerada.
- **Request:**
```json
{
  "fromAccountId": "uuid-origem",
  "toAccountId": "uuid-destino",
  "date": "2024-04-01",
  "amount": 300.00
}
```

---

## 4. Extrato Bancário (`/api/{uuid}/accounts/...`)

### Detalhe do Extrato (por conta)
- **Endpoint:** `GET /api/{uuid}/accounts/{accountID}/statements/{yyyyMM}?status=confirmed`
- **Response (200 OK):**
```json
[
  {
    "date": "2024-03-01",
    "description": "Saldo Anterior",
    "amount": 0.0,
    "status": "confirmed",
    "runningBal": 1000.00,
    "categoryId": null
  },
  {
    "date": "2024-03-05",
    "description": "Compra Amazon",
    "amount": -250.00,
    "status": "confirmed",
    "runningBal": 750.00,
    "categoryId": "uuid-category"
  }
]
```

### Resumo do Extrato (todas as contas)
- **Endpoint:** `GET /api/{uuid}/accounts/statements/{yyyyMM}?status=confirmed`
- **Response (200 OK):**
```json
[
  {
    "accountId": "uuid-account",
    "accountName": "Conta Corrente",
    "openingBalance": 1000.00,
    "closingBalance": 1500.00,
    "totalIn": 800.00,
    "totalOut": -300.00
  }
]
```

---

## 5. Importação de Extrato/Fatura (`/api/{uuid}/accounts/statement/import`)

### Pré-visualização (upload)
- **Endpoint:** `POST /api/{uuid}/accounts/statement/import/preview`
- **Content-Type:** `multipart/form-data`
- **Campos:** `file` (PDF, obrigatório), `password` (opcional), `accountId` (opcional).
- **Response (200 OK):** discriminada por `documentType`.

**Fatura de cartão — `documentType: "CREDIT_CARD_INVOICE"`:**
```json
{
  "documentType": "CREDIT_CARD_INVOICE",
  "issuer": "SANTANDER",
  "last4s": ["1234"],
  "rows": [
    {
      "last4": "1234",
      "date": "2024-03-10",
      "originalDate": "2024-03-10",
      "description": "Loja X",
      "amount": -120.00,
      "installmentNumber": null,
      "installmentTotal": null,
      "status": "confirmed",
      "duplicate": false,
      "categoryId": "uuid-category"
    }
  ],
  "matchedCardId": "uuid-card",
  "candidateCards": [
    { "id": "uuid-card", "name": "Cartão Black", "last4": "1234" }
  ]
}
```

**Extrato bancário — `documentType: "BANK_STATEMENT"`:**
```json
{
  "documentType": "BANK_STATEMENT",
  "issuer": "BTG",
  "candidateAccounts": [
    { "id": "uuid-account", "name": "Conta Corrente" }
  ],
  "selectedAccountId": "uuid-account",
  "rows": [
    {
      "date": "2024-03-05",
      "description": "PIX recebido",
      "amount": 500.00,
      "type": "income",
      "state": "NEW",
      "categoryId": null,
      "reconcileDescription": null
    }
  ]
}
```
> `state` ∈ `NEW` | `DUPLICATE` | `RECONCILE`. `reconcileDescription` só vem em `RECONCILE`.

- **Erros de preview:** `ProblemDetail` com propriedade extra `code` (ex.: `FILE_REQUIRED`, `PASSWORD_REQUIRED`, `WRONG_PASSWORD`, `UNKNOWN_ISSUER`, `NO_TEXT_LAYER`, `TOO_MANY_PAGES` → 422; `FILE_TOO_LARGE` → 413).

### Confirmação
- **Endpoint:** `POST /api/{uuid}/accounts/statement/import/confirm`
- **Content-Type:** `application/json`
- **Request (fatura de cartão):**
```json
{
  "type": "CREDIT_CARD_INVOICE",
  "cardId": "uuid-card",
  "accountId": null,
  "rows": [
    {
      "description": "Loja X",
      "amount": -120.00,
      "date": "2024-03-10",
      "originalDate": "2024-03-10",
      "installmentNumber": null,
      "installmentTotal": null,
      "transactionType": null,
      "categoryId": "uuid-category"
    }
  ]
}
```
- **Request (extrato bancário):** `type: "BANK_STATEMENT"` + `accountId` (em vez de `cardId`); cada row usa `transactionType` (`income`/`expense`).
- **Response (200 OK):**
```json
{
  "created": 8,
  "reconciled": 2,
  "skipped": 1
}
```
> `cardId` obrigatório para `CREDIT_CARD_INVOICE`; `accountId` obrigatório para `BANK_STATEMENT` (senão `422`).

---

## 6. Fechamento de Período (`/api/{uuid}/accounts/closing`)

### Consultar período de fechamento
- **Endpoint:** `GET /api/{uuid}/accounts/closing`
- **Response (200 OK):**
```json
{ "period": "2024-02" }
```
> `period` é `null` quando não há fechamento definido.

### Definir fechamento
- **Endpoint:** `POST /api/{uuid}/accounts/closing`
- **Request:**
```json
{ "period": "2024-02" }
```
- **Response (200 OK):** `{ "period": "2024-02" }`

### Limpar fechamento
- **Endpoint:** `DELETE /api/{uuid}/accounts/closing` → `204 No Content`.

---

## 7. Categorias (`/api/{uuid}/categories`)

`nature` ∈ `REVENUE` | `EXPENSE` (árvore via `parentId`).

### Listar
- **Endpoint:** `GET /api/{uuid}/categories`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-cat",
    "name": "Alimentação",
    "nature": "EXPENSE",
    "parentId": null,
    "isSystem": false
  }
]
```
> `isSystem: true` marca categorias de sistema (semente).

### Criar
- **Endpoint:** `POST /api/{uuid}/categories` → `201 Created`.
- **Request:**
```json
{
  "name": "Restaurantes",
  "nature": "EXPENSE",
  "parentId": "uuid-alimentacao"
}
```

### Atualizar
- **Endpoint:** `PATCH /api/{uuid}/categories/{id}`
- **Request:** (a `nature` é preservada — não é alterável via update)
```json
{
  "name": "Restaurantes e Bares",
  "parentId": "uuid-alimentacao"
}
```

### Excluir
- **Endpoint:** `DELETE /api/{uuid}/categories/{id}` → `204 No Content`.

---

## 8. Tags (`/api/{uuid}/tags`)

### Listar
- **Endpoint:** `GET /api/{uuid}/tags`
- **Response (200 OK):**
```json
[
  { "id": "uuid-tag", "name": "Viagem", "color": "#00FF00" }
]
```

### Criar / Atualizar
- **Endpoints:** `POST /api/{uuid}/tags` (`201 Created`) · `PATCH /api/{uuid}/tags/{id}`
- **Request:**
```json
{ "name": "Urgente", "color": "#FF0000" }
```

### Excluir
- **Endpoint:** `DELETE /api/{uuid}/tags/{id}` → `204 No Content`.

---

## 9. Dashboard (`/api/{uuid}/dashboard`)

### Resultado Mensal
- **Endpoint:** `GET /api/{uuid}/dashboard/result?month=3&year=2024`
- **Response (200 OK):**
```json
{
  "incomes": 5000.0,
  "expenses": 3200.0,
  "result": 1800.0,
  "history": [
    { "month": "S1", "incomes": 1250.0, "expenses": 800.0 }
  ]
}
```
> `history` agrupa por semana (`S1`..`S5`); considera apenas transações `confirmed` do mês.

---

## 10. Centros de Custo (`/api/cost-center`)

Dado fixo do sistema: rota **global** e **somente leitura**.

### Listar
- **Endpoint:** `GET /api/cost-center`
- **Response (200 OK):**
```json
[
  { "id": "d0000000-0000-0000-0000-000000000001", "description": "Fixo" },
  { "id": "d0000000-0000-0000-0000-000000000002", "description": "Variável" }
]
```

---

## 11. Stream de Eventos (`/api/{uuid}/stream`)

- **Endpoint:** `GET /api/{uuid}/stream`
- **Content-Type:** `text/event-stream` (Server-Sent Events).
- Conexão de longa duração; o token é validado sem rotação. Empurra atualizações de domínio (contas/transações) para o cliente.

---

## 12. Versão (`/api/version`)

- **Endpoint:** `GET /api/version` (global)
- **Response (200 OK):**
```json
{ "version": "0.9.0" }
```

---

## 13. Estrutura de Erros (RFC 7807)

O sistema usa `ProblemDetail` para reportar erros. Mapeamento de domínio → HTTP:

| Erro de domínio | Status HTTP |
|---|---|
| `NotFound` | `404 Not Found` |
| `Conflict` | `409 Conflict` |
| `Validation` | `422 Unprocessable Content` |
| `BusinessRule` | `400 Bad Request` |

Outros: `401` (não autenticado), `403` (guarda de propriedade), `405` (método não suportado), `413` (arquivo grande), `500` (erro interno).

### Exemplo de Erro de Validação
- **Response (422 Unprocessable Content):**
```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "name: must not be blank; balance: must not be null",
  "instance": "/api/u-123/accounts"
}
```

### Exemplo de Recurso não Encontrado
- **Response (404 Not Found):**
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Account not found",
  "instance": "/api/u-123/accounts/uuid"
}
```

### Erro de importação (com `code`)
- **Response (422 Unprocessable Content):**
```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Selecione um arquivo PDF.",
  "code": "FILE_REQUIRED",
  "instance": "/api/u-123/accounts/statement/import/preview"
}
```
