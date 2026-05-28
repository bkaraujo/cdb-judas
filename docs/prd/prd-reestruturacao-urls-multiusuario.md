# PRD — Reestruturação de URLs por usuário (namespace `/api/{uuid}`)

## Problem Statement

Hoje as URLs da API (`/api/accounts`, `/api/transactions`, `/api/statement`, ...) não carregam nenhuma identificação do usuário que está acessando. Quando o operador precisa analisar o log de acesso para entender o comportamento de um usuário específico — o que ele acessou, em que ordem, com que frequência — a linha de log (método + caminho) não diz a quem pertence a requisição. A identidade só existe em campos auxiliares (MDC, `X-request-id`), que muitos formatos de access-log não registram por padrão. Isso torna a análise de acesso por usuário trabalhosa e pouco confiável.

Além disso, a modelagem de algumas entidades não reflete o domínio real:
- **Contas a Pagar/Receber (Payables)** é, na prática, apenas uma projeção de transações pendentes — mas existe como recurso/endpoint separado, duplicando conceito.
- **Cartões de Crédito** são um subtipo de Conta, mas têm um recurso de topo próprio.
- **Centro de Custo** é um dado fixo do sistema (sempre "Fixo" e "Variável"), porém é exposto com CRUD completo e armazenado por usuário, permitindo edições/exclusões indevidas.

## Solution

Toda rota de dados de um usuário passa a viver sob um namespace que identifica o usuário na própria URL: `/api/{uuid}/...`, onde `{uuid}` é o identificador imutável do usuário autenticado. Assim a linha do access-log já carrega o dono da requisição, tornando a análise por usuário direta.

O `{uuid}` é **autoritativo**: um interceptor central valida que o `{uuid}` da rota é igual ao usuário autenticado e responde **403** em caso de divergência. A seleção de dados continua sendo feita pelo token (cada usuário só enxerga o próprio arquivo), e o `{uuid}` na rota funciona como guarda adicional + rótulo de log.

Aproveita-se a mudança para corrigir a modelagem:
- **Payables deixa de existir** como recurso; "A Pagar e Receber" passa a ser um filtro sobre transações (`status=pending` + `type=expense|income`).
- **Cartões de Crédito** são absorvidos por Contas (`type=card`); o recurso separado é removido.
- **Centro de Custo** vira dado fixo do sistema, somente leitura, global (sem `{uuid}`), servido a partir de um arquivo global.
- **Extratos (Statements)** ganham uma forma agregada por conta (resumo do mês por conta) além do extrato detalhado de uma conta.
- O recurso **Contas** passa a ser o namespace pai de transações, extratos, fechamento e cartões.

A virada é **completa (hard cutover)**: front-end e back-end mudam juntos no mesmo deploy, sem rotas antigas em paralelo. **Não há migração de dados**: os dados existentes são descartados e o sistema parte de um estado limpo no novo esquema baseado em `uuid` (um seed inicial recria o usuário e a fonte global de centros de custo).

## User Stories

### Identificação e análise de acesso
1. Como operador do sistema, quero que a linha do access-log contenha o identificador do usuário no caminho da requisição, para analisar o acesso por usuário sem depender de campos auxiliares.
2. Como operador, quero que todas as rotas de dados de usuário sigam o padrão `/api/{uuid}/...`, para filtrar logs por um prefixo único e consistente.
3. Como operador, quero que rotas globais (centro de custo, login) fiquem claramente fora do namespace de usuário, para distinguir tráfego global de tráfego por usuário.

### Segurança / isolamento
4. Como usuário autenticado, quero que o sistema rejeite (403) qualquer tentativa de acessar o namespace de outro usuário, para garantir que meus dados não sejam acessíveis por terceiros.
5. Como usuário, quero que minha sessão continue selecionando automaticamente apenas os meus dados (via token), para que a troca de URL não exponha dados de outras pessoas mesmo se eu manipular o caminho.
6. Como mantenedor, quero que a validação `{uuid} == usuário autenticado` seja centralizada, para que nenhum endpoint novo fique sem proteção por esquecimento.

### Identidade do usuário
7. Como usuário, quero receber meu identificador imutável ao efetuar login, para que o cliente monte as URLs `/api/{uuid}/...` corretamente.
8. Como mantenedor, quero um registro central de usuários com `id`, `username` e senha, para resolver `username → id` no login e `id → usuário` quando necessário.
9. Como operador, quero que o `username` continue presente nos logs (junto ao `uuid`), para manter a legibilidade humana além do identificador opaco.

### Contas (incluindo cartões)
10. Como usuário, quero listar e gerenciar minhas contas em `/api/{uuid}/accounts`, para manter o controle das minhas contas bancárias.
11. Como usuário, quero criar, editar e excluir cartões de crédito como contas do tipo cartão, para tratar cartões como o subtipo de conta que eles são.
12. Como usuário, quero listar apenas meus cartões filtrando contas por tipo cartão, para visualizar somente cartões quando necessário.
13. Como usuário, quero consultar o saldo de uma conta por período (mês ou ano), para acompanhar a evolução do saldo.

### Transações
14. Como usuário, quero listar todas as minhas transações (de todas as contas) em uma coleção, para ter uma visão consolidada.
15. Como usuário, quero filtrar transações por intervalo de datas, limite, status e tipo, para encontrar lançamentos específicos.
16. Como usuário, quero listar as transações de uma conta específica, para analisar uma conta isoladamente.
17. Como usuário, quero criar uma transação em uma conta indicando a conta pelo caminho, para que o vínculo conta↔transação fique explícito na URL.
18. Como usuário, quero editar e excluir uma transação, para corrigir lançamentos.
19. Como usuário, quero excluir uma transação escolhendo o modo (ex.: parcela única ou grupo), para tratar parcelamentos corretamente.
20. Como usuário, quero alterar o status de uma transação (ex.: confirmar pagamento) informando a data, para registrar quando a obrigação foi quitada.
21. Como usuário, quero transferir valores entre duas contas em uma operação dedicada, para registrar transferências sem duplicar lançamentos manualmente.
22. Como usuário, quero pré-visualizar e confirmar a importação de transações a partir de uma fatura (PDF), para registrar gastos de cartão em lote.

### Contas a Pagar e Receber (como filtro de transações)
23. Como usuário, quero ver minhas contas "A Pagar" como transações de despesa pendentes, para acompanhar obrigações futuras sem um cadastro separado.
24. Como usuário, quero ver minhas contas "A Receber" como transações de receita pendentes, para acompanhar recebimentos previstos.
25. Como usuário, quero confirmar o pagamento/recebimento de um item pendente alterando o status da transação, para baixar a obrigação no mesmo modelo de dados.

### Extratos (Statements)
26. Como usuário, quero um resumo do mês por conta (saldo inicial, saldo final, total de entradas e saídas), para comparar rapidamente o desempenho de todas as contas no período.
27. Como usuário, quero o extrato detalhado de uma conta no mês, com saldo corrente linha a linha, para conferir os lançamentos e a evolução do saldo.
28. Como usuário, quero filtrar o extrato detalhado por status, para ver, por exemplo, apenas lançamentos confirmados.
29. Como usuário, quero informar o período do extrato no formato ano-mês no próprio caminho, para que a URL seja autoexplicativa e fácil de buscar em logs.

### Fechamento (Closing)
30. Como usuário, quero consultar, definir e limpar o período de fechamento em `/api/{uuid}/accounts/closing`, para controlar até quando os lançamentos estão consolidados.

### Categorias e Tags
31. Como usuário, quero gerenciar minhas categorias (criar, editar, excluir), para classificar receitas e despesas.
32. Como usuário, quero que categorias de sistema permaneçam imutáveis, para não quebrar classificações padrão.
33. Como usuário, quero gerenciar minhas tags, para marcar transações de forma flexível.

### Dashboard
34. Como usuário, quero acessar meu dashboard sob `/api/{uuid}/dashboard`, para ver agregações das minhas finanças com a identidade na URL.

### Centro de Custo (fixo, somente leitura)
35. Como usuário, quero consultar a lista fixa de centros de custo do sistema em uma rota global somente leitura, para classificar lançamentos pelos valores padrão.
36. Como mantenedor, quero impedir criação, edição e exclusão de centros de custo via API, para garantir que esse dado permaneça fixo e consistente entre usuários.
37. Como mantenedor, quero que os centros de custo sejam servidos de uma fonte global única, para que todos os usuários compartilhem exatamente os mesmos valores e identificadores.

### Virada
38. Como mantenedor, quero descartar todos os dados existentes e iniciar o sistema limpo no novo esquema baseado em `uuid`, para evitar a complexidade e o risco de uma migração.
39. Como mantenedor, quero um seed inicial que recrie o usuário (com `uuid`) no registro central e a fonte global de centros de custo, para que a aplicação suba utilizável a partir do estado limpo.
40. Como mantenedor, quero uma virada completa (sem rotas antigas em paralelo), para evitar manutenção de dois esquemas e ambiguidade nos logs.

### Eventos em tempo real (SSE)
41. Como usuário, quero que minha assinatura de eventos em tempo real seja escopada ao meu `uuid`, para receber apenas notificações das minhas próprias alterações de dados e nunca de outro usuário.
42. Como operador, quero que o stream use uma URL que explicite o usuário (`/api/{uuid}/stream`), para identificar no log de acesso a quem pertence cada assinatura.

## Implementation Decisions

### Identidade e namespace
- O identificador do usuário (`uuid`) é **autoritativo**: validado contra o usuário autenticado. A seleção de dados permanece pelo token (cada usuário só acessa o próprio arquivo); o `uuid` na rota é guarda + rótulo de log.
- O segmento de identidade é um **UUID novo do usuário** (não o `username`). Ownership passa a ser chaveado por `uuid`.
- **Registro central de usuários** como fonte única de identidade: contém `id`, `username` e hash de senha. Login resolve `username → usuário`; o token passa a mapear para o `id` do usuário. A senha sai dos arquivos de dados por usuário e passa a viver no registro.
- O **prefixo `/api` é mantido**: as rotas ficam `/api/{uuid}/...`. Isso preserva a distinção estático-vs-API existente e os filtros atuais sem alteração.

### Guarda de propriedade (interceptor)
- Um **interceptor central** atua sobre `/api/{uuid}/**`, extrai o segmento de identidade e compara com o id do usuário autenticado; em divergência responde **403 Forbidden**.
- Os controladores **não leem** o segmento de identidade (não fazem binding dele); permanecem agnósticos ao `uuid`.
- Rotas globais (centro de custo, login) ficam fora do namespace e não passam pela guarda de identidade.
- O stream de eventos (SSE) vive sob o namespace do usuário (`/api/{uuid}/stream`) e passa pela guarda como qualquer rota de usuário; a autenticação do stream continua por token validado sem rotação (stream longo).

### Mapa de rotas (alvo)
- **Contas (com cartões):** coleção e item de conta sob `/api/{uuid}/accounts`; criação de cartão via conta do tipo cartão; listagem de cartões por filtro de tipo; saldo por período (mês/ano).
- **Transações (espelhando extratos):** coleção entre contas sob o namespace de contas (lista com filtros de data/limite/status/tipo; transferência; importação preview/confirm) e forma por conta (listar; criar com a conta vinda do caminho; editar/excluir; alterar status). A exclusão preserva o parâmetro de modo.
- **A Pagar/Receber:** sem recurso próprio; obtido por filtro de transações (`status=pending` + `type=expense|income`). Os rótulos "A Pagar"/"A Receber" são responsabilidade do front-end.
- **Extratos:** forma agregada por conta (resumo do mês por conta) e forma detalhada por conta (extrato com saldo corrente, com filtro de status). O período vai no caminho no formato ano-mês compacto (`yyyyMM`).
- **Fechamento:** consultar/definir/limpar sob o namespace de contas.
- **Categorias e Tags:** CRUD por usuário; categorias de sistema permanecem imutáveis.
- **Dashboard:** sob o namespace do usuário.
- **Eventos em tempo real (SSE):** stream sob `/api/{uuid}/stream`; a assinatura é registrada e escopada pelo `uuid` do usuário (cada usuário recebe apenas os próprios eventos).
- **Centro de Custo:** rota global somente leitura; sem mutações.

### Modelagem / contratos
- **Payables eliminado:** recurso, serviço e DTOs removidos; comportamento coberto por transações.
- **Cartões absorvidos por Contas:** recurso de cartão removido; os campos específicos de cartão (ex.: últimos 4 dígitos, dia de fechamento, dia de vencimento, limite, conta vinculada) trafegam como atributos da conta do tipo cartão.
- **Resumo de extrato (novo contrato):** por conta, com saldo inicial, saldo final, total de entradas e total de saídas no período.
- **Centro de Custo:** contrato somente leitura; conjunto fixo com os identificadores e descrições já existentes ("Fixo", "Variável"), servidos de fonte global.

### Login e cliente
- O login passa a retornar o `uuid` do usuário em **cabeçalho de resposta** (junto ao token), além do token já existente.
- O cliente armazena o `uuid` e o **prepende automaticamente** às chamadas de rotas de usuário, incluindo o stream de eventos (`/api/{uuid}/stream`); apenas chamadas globais (centro de custo) usam exceção explícita (sem prefixo de usuário).
- Repositórios e páginas do front-end são ajustados: contas absorvem operações de cartão; "A Pagar/Receber" usa filtro de transações; extratos usam os novos caminhos e o resumo; centro de custo vira somente leitura (sem UI de CRUD e sem canal de eventos de mutação).

### Eventos em tempo real (SSE)
- O stream passa a viver sob o namespace do usuário em `/api/{uuid}/stream` (substitui a rota global anterior).
- A **assinatura é escopada pelo `uuid`**: o registro de assinantes é mantido por usuário e os eventos de domínio são entregues apenas aos assinantes do usuário dono do dado alterado — um usuário nunca recebe eventos de outro.
- A guarda de propriedade valida o `uuid` do caminho; a autenticação do stream permanece por token validado sem rotação (stream longo).

### Persistência
- O nome do arquivo de dados por usuário passa a ser derivado do `uuid` do usuário autenticado.
- **Sem migração de dados:** os dados existentes são descartados. O sistema parte de um estado limpo; não há renomeação de arquivos nem código de migração (runtime ou avulso).
- **Seed inicial:** cria o registro central de usuários com o usuário e seu `uuid` (a senha vive no registro) e provê a fonte global de centros de custo.
- **Hard cutover:** sem rotas antigas em paralelo; front-end e back-end mudam no mesmo deploy.

### Logs
- O MDC mantém o `username` para legibilidade e passa a registrar também o `uuid`, para correlacionar a linha de access-log (que contém o `uuid`) com o usuário humano.

## Testing Decisions

Um bom teste verifica **comportamento externo observável**, não detalhes de implementação: dado um estado de entrada e uma requisição, qual a resposta/efeito. Testes não devem depender de nomes internos de métodos, estrutura de arquivos ou passos intermediários, de modo a sobreviver a refatorações.

Prior art no repositório:
- **Testes de borda web:** classes `*ResourceTest` que estendem a base HTTP, usando MockMvc, exercitando os endpoints com JSON real através de toda a pilha.
- **Testes de caso de uso/serviço:** usam repositórios em memória (fakes reais, não mocks) para exercitar lógica de domínio.
- **Testes unitários puros:** parsers, matchers e afins, isolados.
- **ArchUnit:** regras de arquitetura.
- Não há uso de mocks (Mockito); o padrão é objetos reais + fakes em memória + MockMvc.

Módulos com testes dedicados (decisão do desenvolvedor):
- **Interceptor de propriedade (guarda de `uuid`)** — crítico de segurança. Casos: `uuid` igual ao autenticado passa; divergente retorna 403; rotas globais (centro de custo, login) ignoram a guarda; o stream de eventos sob `/api/{uuid}/stream` é guardado como rota de usuário (uuid divergente retorna 403); `uuid` ausente/malformado é rejeitado. Estilo MockMvc via base HTTP.
- **Resumo de extrato (StatementSummary)** — computação nova. Casos: saldo inicial/final e totais de entrada/saída por conta; mês vazio (inicial == final); presença de transferências; filtro de status. Estilo caso de uso com repositórios em memória.
- **Registro de usuários + token** — resolução de identidade. Casos: `findByUsername`/`findById`; emissão e rotação de token mapeando para o `id` do usuário; `username` preservado para logs.

Atualização obrigatória (não opcional): todas as classes `*ResourceTest` existentes precisam refletir os novos caminhos `/api/{uuid}/...` e a base HTTP precisa semear um usuário e construir as URLs com o `uuid`. Os testes de Payables e de Cartões de Crédito são removidos; os testes de Centro de Custo passam a cobrir somente leitura.

## Out of Scope

- **Multi-tenancy real / compartilhamento de dados entre usuários.** O isolamento por arquivo (cada usuário só acessa o próprio) é mantido; não há recursos de acesso cruzado, papéis ou permissões granulares.
- **Reescrita do mecanismo de autenticação.** O fluxo de token rotativo permanece; muda apenas o que o token mapeia (passa a referenciar o `id`) e o retorno do `uuid` no login.
- **Budget/Orçamento e Relatórios no back-end.** Não há recurso de back-end para esses hoje (apenas chamadas de front-end sem servidor correspondente); o caminho do cliente pode ser ajustado para consistência, mas implementar o back-end está fora do escopo.
- **Migração de dados.** Os dados existentes são descartados; não há conversão, renomeação ou preservação dos arquivos atuais.
- **Versionamento de API / compatibilidade retroativa.** A virada é completa; não há rotas antigas em paralelo nem período de depreciação.

## Further Notes

- A persistência já isola dados por usuário (o nome do arquivo deriva do usuário autenticado). Por isso o `uuid` na rota não é o seletor de dados — é guarda + rótulo de log. Como os dados são descartados, não há renomeação de arquivos: o estado limpo já nasce no esquema por `uuid`.
- Centro de custo não é referenciado por nenhuma transação ou categoria no código nem nos dados atuais; torná-lo fixo/somente leitura não gera órfãos.
- A página de extrato já lista contas à esquerda e detalha uma conta à direita; o novo resumo por conta alimenta naturalmente essa visão de "panorama → detalhe".
- O roteador do front-end é baseado em hash, então as rotas de navegação do SPA não colidem com as rotas de servidor `/api/{uuid}/...`.
- Cartão de crédito já é, no domínio, uma conta do tipo cartão; o recurso separado era apenas um invólucro de conveniência, o que torna a absorção em Contas de baixo risco.
- O formato de período do extrato (`yyyyMM`) foi escolhido para casar com o parâmetro de período já usado na consulta de saldo de conta.
