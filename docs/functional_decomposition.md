# Decomposição Funcional

## 1. Segurança e Core
- [Autenticação] Autentica usuários [Proteger acesso]. Usa `CurrentUser`, `LoginRequest`, JWT/Tokens (`AccessTokenStore`).
- [Persistência] Salva dados [Manter estado]. Baseado em JSON (`JsonStorageConfig`, repositórios JSON).
- [Web/API] Trata requisições [Prover endpoints]. Contém filtros (`AuthorizationFilter`, `RequestLoggingFilter`) e OpenAPI.

## 2. Gestão Financeira
- [Contas] Gerencia contas [Manter saldos]. (`Account`, `AccountResource`).
- [Transações] Registra entradas e saídas [Rastrear fluxo financeiro]. (`Transaction`, transferências).
- [Fechamentos] Consolida períodos [Gerar relatórios de faturamento/balanço]. (`Closing`).
- [Extratos] Exibe histórico [Facilitar conferência]. (`StatementService`).
- [Importação de Extratos] Lê arquivos de bancos [Automatizar lançamentos]. Suporta BTG, Santander (`BankStatementParserRegistry`, leitura de cartão de crédito e conta corrente).

## 3. Classificação e Organização
- [Categorias] Agrupa transações [Permitir análise macro]. (`Category`).
- [Tags] Marca transações [Permitir análise micro]. (`Tag`).

## 4. Dashboard
- [Dashboard] Agrega informações [Exibir visão geral]. (`DashboardService`).

## 5. Sistema
- [Centro de Custo] Define centros de custo [Organizar rateios]. (`CostCenter`).
- [Streaming/SSE] Envia eventos em tempo real [Atualizar interface]. (`SseService`, `SseController`).
