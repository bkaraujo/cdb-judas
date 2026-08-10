/**
 * Raiz da camada de features da aplicação.
 *
 * <p>Cada feature é uma fatia numerada {@code fNNN} (hoje f000–f007, f009, f010, f999), hexágono auto-contido:
 * {@code _0_domain} (modelos/overlays + portas {@code *Repository} + eventos de domínio),
 * {@code _1_application} ({@code *Service}/{@code *UseCase} + commands + {@code @MessageListener}
 * best-effort), {@code _2_infrastructure} ({@code *Resource}, DTOs HTTP, {@code *JDBCRepository},
 * {@code FNNNModule} CDI). Fatia de negócio nunca importa fatia de negócio irmã (ver ArchitectureTest,
 * regra {@code feature_slices_must_not_depend_on_sibling_slices}) — cross-feature é sempre evento
 * ({@code br.commons.MessageBus}, record em {@code f000._0_domain.event}, efeito sem retorno),
 * {@code f000.InternalApi} (HTTP real contra o endpoint público da fatia dona, leitura síncrona com
 * retorno), ou adapter em {@code f999._2_infrastructure.adapter} implementando porta declarada pelo
 * consumidor (único lugar que conhece os dois lados — hoje sem instância ativa). Histórico completo
 * da migração fatias-planas→fNNN e da inversão de dependências em {@code .claude/refactor.md}.
 *
 * <h2>URLs exportadas por feature</h2>
 * <pre>
 * feature
 * ├── f000  fatia-base — todas as demais podem depender dela, ela de nenhuma
 * │         (par CQRS ReadUseCase/WriteUseCase: pessoa + centro de custo, consumido por f001/UserService)
 * │   ├── CostCenterResource   GET  /api/cost-center                          (sem namespace de usuário)
 * │   ├── LoginResource        POST /login
 * │   ├── SseResource          GET  /api/{uuid}/stream                        (Server-Sent Events)
 * │   └── VersionResource      GET  /api/version                              (sem namespace de usuário)
 * ├── f001  self-service (par CQRS ReadUseCase/WriteUseCase, sem *UseCase de fronteira)
 * │   └── SelfResource         GET/PATCH /api/me                              (nome + preferências write-through)
 * ├── f002  accounts (+ balance e closing fundidos: sem overlay próprio; cards[] embutido é projeção
 * │         somente-leitura de f003 — ver f003.WriteUseCase para a mutação)
 * │   ├── ClosingResource        GET/POST/DELETE /api/{uuid}/accounts/closing
 * │   ├── AccountResource        GET    /api/{uuid}/accounts
 * │   │                          GET    /api/{uuid}/accounts/{id}
 * │   │                          POST   /api/{uuid}/accounts
 * │   │                          PATCH  /api/{uuid}/accounts/{id}
 * │   │                          DELETE /api/{uuid}/accounts/{id}?strategy=&amp;targetId=
 * │   └── AccountBalanceResource GET    /api/{uuid}/accounts/balance?period=yyyyMM
 * │                              GET    /api/{uuid}/accounts/{id}/balance?period=yyyyMM|year=yyyy
 * ├── f003  cards (extraída de f002 — ver .claude/refactor.md; par CQRS ReadUseCase/WriteUseCase,
 * │         sem *UseCase de fronteira, como f002/f006)
 * │   └── AccountCardResource    GET    /api/{uuid}/accounts/{accountId}/cards
 * │                              POST   /api/{uuid}/accounts/{accountId}/cards
 * │                              PATCH  /api/{uuid}/accounts/{accountId}/cards/{cardId}
 * │                              DELETE /api/{uuid}/accounts/{accountId}/cards/{cardId}?strategy=&amp;targetId=
 * ├── f004  tags (par CQRS ReadUseCase/WriteUseCase, sem *UseCase de fronteira)
 * │   └── TagResource          GET    /api/{uuid}/tags
 * │                            POST   /api/{uuid}/tags
 * │                            PATCH  /api/{uuid}/tags/{id}
 * │                            DELETE /api/{uuid}/tags/{id}?strategy=&amp;targetId=  (reage a {@code TransactionsDeleted}, evento de f000/f006)
 * ├── f005  categories (par CQRS ReadUseCase/WriteUseCase, sem *UseCase de fronteira)
 * │   └── CategoryResource     GET    /api/{uuid}/categories
 * │                            POST   /api/{uuid}/categories
 * │                            PATCH  /api/{uuid}/categories/{id}
 * │                            DELETE /api/{uuid}/categories/{id}?strategy=&amp;targetId=
 * │                            GET    /api/{uuid}/categories/transfer?nature=            (interno — InternalApi de f006.ReadUseCases)
 * ├── f006  transactions + transfer
 * │   ├── TransferResource     POST   /api/{uuid}/accounts/transactions/transfer
 * │   └── TransactionResource  GET    /api/{uuid}/accounts/transactions?limit=&amp;dateFrom=&amp;dateTo=&amp;status=&amp;type=       (cross-account)
 * │                            GET    /api/{uuid}/accounts/transactions/by-category?categoryIds=     (interno — InternalApi de f005.WriteUseCase)
 * │                            GET    /api/{uuid}/accounts/{accId}/transactions?limit=&amp;dateFrom=&amp;dateTo=&amp;status=&amp;type=
 * │                            POST   /api/{uuid}/accounts/{accId}/transactions
 * │                            PATCH  /api/{uuid}/accounts/{accId}/transactions/{txId}
 * │                            PATCH  /api/{uuid}/accounts/{accId}/transactions/{txId}/status
 * │                            DELETE /api/{uuid}/accounts/{accId}/transactions/{txId}?mode=          (publica {@code TransactionsDeleted})
 * ├── f007  importação de extrato/fatura (extraída de f006 — ver .claude/plan.md; escrita via porta
 * │         TransactionWriter/adapter em f999, leitura cross-slice via F002Api/F003Api/F004Api/F006Api)
 * │   └── ImportResource POST /api/{uuid}/accounts/transactions/import/preview  (multipart: file/password/accountId)
 * │                      POST /api/{uuid}/accounts/transactions/import/confirm  (parsers BTG/Santander, casamento de cartão, sugestão de categoria)
 * ├── f009  dashboard (só ReadUseCase: fatia somente-leitura)
 * │   └── DashboardResource    GET /api/{uuid}/dashboard?month=&amp;year=
 * ├── f010  regras de nomenclatura (par CQRS ReadUseCase/WriteUseCase, sem *UseCase de fronteira;
 * │         CRUD puro, sem consumidor cross-slice — casamento de texto é 100% client-side)
 * │   └── ImportRuleResource   GET    /api/{uuid}/accounts/transaction/rules
 * │                            POST   /api/{uuid}/accounts/transaction/rules
 * │                            PATCH  /api/{uuid}/accounts/transaction/rules/{id}
 * │                            DELETE /api/{uuid}/accounts/transaction/rules/{id}
 * ├── f999  Initialization routines                                                       (sem HTTP)
 * </pre>
 */
@NullMarked
package br.cdb.feature;

import org.jspecify.annotations.NullMarked;
