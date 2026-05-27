# API Web — Exemplos de Request/Response JSON

Este documento cataloga os contratos de dados (JSON) para a API Web do sistema, organizados por recurso.

---

## 1. Contas (`/api/accounts`)

### Listar Todas
- **Endpoint:** `GET /api/accounts`
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
    }
  }
]
```

### Buscar por ID
- **Endpoint:** `GET /api/accounts/{id}`
- **Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Conta Corrente",
  "balance": 1250.50,
  "type": "CHECKING",
  "color": "#007AFF",
  "active": true,
  "linkedAccountId": null,
  "additionalInfo": null
}
```

### Saldo Mensal/Anual
- **Endpoint:** `GET /api/accounts/{id}/balance?period=202401` ou `?year=2024`
- **Response (200 OK - Período):**
```json
{
  "id": "uuid-balance",
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "period": "2024-01",
  "balance": 5000.00
}
```

### Criar Conta
- **Endpoint:** `POST /api/accounts`
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

### Atualizar Conta
- **Endpoint:** `PATCH /api/accounts/{id}`
- **Request:** (mesma estrutura do POST)

---

## 2. Transações (`/api/transactions`)

### Listar Recentes
- **Endpoint:** `GET /api/transactions?limit=10`
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
    "paymentDate": "2024-03-15",
    "groupId": null,
    "installmentNumber": null,
    "totalInstallments": null
  }
]
```

### Criar Transação
- **Endpoint:** `POST /api/transactions`
- **Request:**
```json
{
  "description": "Pagamento Aluguel",
  "amount": 2500.00,
  "date": "2024-04-01",
  "categoryId": "uuid-category",
  "accountId": "uuid-account",
  "status": "pending",
  "type": "expense",
  "installments": 1,
  "editMode": "single"
}
```

### Atualizar Status
- **Endpoint:** `PATCH /api/transactions/{id}/status`
- **Request:**
```json
{
  "status": "confirmed",
  "paymentDate": "2024-04-02"
}
```

---

## 3. Cartões de Crédito (`/api/credit-cards`)

### Listar Todos
- **Endpoint:** `GET /api/credit-cards`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-card",
    "accountId": "uuid-acc",
    "last4": "1234",
    "limit": 5000.00,
    "closingDay": 5,
    "dueDay": 12,
    "color": "#000000",
    "active": true
  }
]
```

### Criar Cartão
- **Endpoint:** `POST /api/credit-cards`
- **Request:**
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Black Card",
  "last4": "4321",
  "limit": 10000.00,
  "closingDay": 1,
  "dueDay": 10,
  "color": "#333333",
  "active": true
}
```

---

## 4. Categorias (`/api/categories`)

### Listar Árvore
- **Endpoint:** `GET /api/categories`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-cat",
    "name": "Alimentação",
    "nature": "EXPENSE",
    "parentId": null
  }
]
```

### Criar Categoria
- **Endpoint:** `POST /api/categories`
- **Request:**
```json
{
  "name": "Restaurantes",
  "nature": "EXPENSE",
  "parentId": "uuid-alimentacao"
}
```

---

## 5. Centros de Custo (`/api/cost-centers`)

### Listar
- **Endpoint:** `GET /api/cost-centers`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-cost-center",
    "description": "Pessoal"
  }
]
```

### Criar
- **Endpoint:** `POST /api/cost-centers`
- **Request:**
```json
{
  "description": "Trabalho"
}
```

---

## 6. Dashboard (`/api/dashboard`)

### Resultado Mensal
- **Endpoint:** `GET /api/dashboard/result?month=3&year=2024`
- **Response (200 OK):**
```json
{
  "incomes": 5000.0,
  "expenses": 3200.0,
  "result": 1800.0,
  "history": [
    {
      "month": "S1",
      "incomes": 1250.0,
      "expenses": 800.0
    }
  ]
}
```

---

## 7. Contas a Pagar (`/api/operations/payables`)

### Listar Pendentes
- **Endpoint:** `GET /api/operations/payables`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-payable",
    "name": "Internet",
    "due": "2024-03-20",
    "amount": 120.00,
    "accountId": "uuid-account",
    "categoryId": "uuid-category",
    "status": "pending",
    "type": "expense"
  }
]
```

### Confirmar Pagamento
- **Endpoint:** `PUT /api/operations/payables/{id}/confirm`
- **Request:**
```json
{
  "paymentDate": "2024-03-19"
}
```

---

## 8. Fechamento de Período (`/api/operations/closing`)

### Executar Fechamento
- **Endpoint:** `POST /api/operations/closing`
- **Request:**
```json
{
  "period": "2024-02"
}
```
- **Response (200 OK):**
```json
{
  "period": "2024-02"
}
```

---

## 9. Tags (`/api/tags`)

### Listar
- **Endpoint:** `GET /api/tags`
- **Response (200 OK):**
```json
[
  {
    "id": "uuid-tag",
    "name": "Viagem",
    "color": "#00FF00"
  }
]
```

### Criar
- **Endpoint:** `POST /api/tags`
- **Request:**
```json
{
  "name": "Urgente",
  "color": "#FF0000"
}
```

---

## 10. Extrato Bancário (`/api/statement`)

### Listar Itens do Extrato
- **Endpoint:** `GET /api/statement?accountId=uuid&month=3&year=2024`
- **Response (200 OK):**
```json
[
  {
    "date": "2024-03-01",
    "description": "Saldo Anterior",
    "amount": 0.0,
    "status": "confirmed",
    "runningBal": 1000.00
  },
  {
    "date": "2024-03-05",
    "description": "Compra Amazon",
    "amount": -250.00,
    "status": "confirmed",
    "runningBal": 750.00
  }
]
```

---

## 11. Estrutura de Erros (RFC 7807)

O sistema utiliza o padrão `ProblemDetail` para reportar erros.

### Exemplo de Erro de Validação
- **Response (422 Unprocessable Content):**
```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "name: must not be blank; balance: must not be null",
  "instance": "/api/accounts"
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
  "instance": "/api/accounts/uuid"
}
```
