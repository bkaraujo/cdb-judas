/**
 * Raiz da camada de features da aplicação.
 *
 * <p>Cada feature é uma fatia numerada {@code fNNN} (hoje f000–f006, f009, f999), hexágono auto-contido:
 * {@code _0_domain} (modelos/overlays + portas {@code *Repository} + eventos de domínio),
 * {@code _1_application} ({@code *Service}/{@code *UseCase} + commands + {@code @MessageListener}
 * best-effort), {@code _2_infrastructure} ({@code *Resource}, DTOs HTTP, {@code *JDBCRepository},
 * {@code FNNNModule} CDI). Cross-feature é só via evento ({@code br.commons.MessageBus}) ou, onde a
 * migração de eventos ainda não terminou, chamada direta documentada como transitória — nunca
 * import de {@code _1_application}/{@code _2_infrastructure} de fatia irmã (ver ArchitectureTest,
 * regra {@code application_must_not_access_infrastructure}). Histórico completo da migração
 * fatias-planas→fNNN em {@code .claude/refactor.md}.
 *
 * <h2>URLs exportadas por feature</h2>
 * <pre>
 * feature
 * ├── f000  fatia-base — todas as demais podem depender dela, ela de nenhuma
 * │   ├── ClosingResource      GET/POST/DELETE /api/{uuid}/accounts/closing
 * │   ├── CostCenterResource   GET  /api/cost-center                          (sem namespace de usuário)
 * │   ├── LoginResource        POST /login
 * │   ├── SseResource          GET  /api/{uuid}/stream                        (Server-Sent Events)
 * │   └── VersionResource      GET  /api/version                              (sem namespace de usuário)
 * ├── f001  self-service
 * │   └── SelfResource         GET/PATCH /api/me                              (nome + preferências write-through)
 * ├── f002  accounts (+ cards e balance fundidos: sem overlay próprio)
 * │   ├── AccountResource        GET    /api/{uuid}/accounts
 * │   │                          GET    /api/{uuid}/accounts/{id}
 * │   │                          POST   /api/{uuid}/accounts
 * │   │                          PATCH  /api/{uuid}/accounts/{id}
 * │   │                          DELETE /api/{uuid}/accounts/{id}?strategy=&amp;targetId=
 * │   ├── AccountBalanceResource GET    /api/{uuid}/accounts/balance?period=yyyyMM
 * │   │                          GET    /api/{uuid}/accounts/{id}/balance?period=yyyyMM|year=yyyy
 * │   └── AccountCardResource    GET    /api/{uuid}/accounts/{accountId}/cards
 * │                              POST   /api/{uuid}/accounts/{accountId}/cards
 * │                              PATCH  /api/{uuid}/accounts/{accountId}/cards/{cardId}
 * │                              DELETE /api/{uuid}/accounts/{accountId}/cards/{cardId}?strategy=&amp;targetId=
 * ├── f003  tags
 * │   └── TagResource          GET    /api/{uuid}/tags
 * │                            POST   /api/{uuid}/tags
 * │                            PATCH  /api/{uuid}/tags/{id}
 * │                            DELETE /api/{uuid}/tags/{id}?strategy=&amp;targetId=  (reage a {@code TransactionsDeleted}, evento de f000/f005)
 * ├── f004  categories
 * │   └── CategoryResource     GET    /api/{uuid}/categories
 * │                            POST   /api/{uuid}/categories
 * │                            PATCH  /api/{uuid}/categories/{id}
 * │                            DELETE /api/{uuid}/categories/{id}?strategy=&amp;targetId=
 * ├── f005  transactions + transfer
 * │   ├── TransferResource     POST   /api/{uuid}/accounts/transactions/transfer
 * │   └── TransactionResource  GET    /api/{uuid}/accounts/transactions?limit=&amp;dateFrom=&amp;dateTo=&amp;status=&amp;type=       (cross-account)
 * │                            GET    /api/{uuid}/accounts/{accId}/transactions?limit=&amp;dateFrom=&amp;dateTo=&amp;status=&amp;type=
 * │                            POST   /api/{uuid}/accounts/{accId}/transactions
 * │                            PATCH  /api/{uuid}/accounts/{accId}/transactions/{txId}
 * │                            PATCH  /api/{uuid}/accounts/{accId}/transactions/{txId}/status
 * │                            DELETE /api/{uuid}/accounts/{accId}/transactions/{txId}?mode=          (publica {@code TransactionsDeleted})
 * ├── f006  importação de extrato/fatura
 * │   └── StatementImportResource POST /api/{uuid}/accounts/transactions/import/preview  (multipart: file/password/accountId)
 * │                                POST /api/{uuid}/accounts/transactions/import/confirm  (parsers BTG/Santander, casamento de cartão, sugestão de categoria)
 * ├── f009  dashboard
 * │   └── DashboardResource    GET /api/{uuid}/dashboard?month=&amp;year=
 * ├── f999  Initialization routines                                                       (sem HTTP)
 * </pre>
 */
@NullMarked
package br.cdb.feature;

import org.jspecify.annotations.NullMarked;
