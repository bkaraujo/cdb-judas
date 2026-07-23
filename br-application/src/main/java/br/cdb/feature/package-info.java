/**
 * Raiz da camada de features da aplicação.
 *
 * <p>Cada feature é uma fatia numerada {@code fNNN} (f000–f010), hexágono auto-contido:
 * {@code _0_domain} (modelos/overlays + portas {@code *Repository} + eventos de domínio),
 * {@code _1_application} ({@code *Service}/{@code *UseCase} + commands + {@code @MessageListener}
 * best-effort), {@code _2_infrastructure} ({@code *Resource}, DTOs HTTP, {@code *JDBCRepository},
 * {@code FNNNModule} CDI). Cross-feature é só via evento ({@code br.commons.MessageBus}) ou, onde a
 * migração de eventos ainda não terminou, chamada direta documentada como transitória — nunca
 * import de {@code _1_application}/{@code _2_infrastructure} de fatia irmã (ver ArchitectureTest,
 * regra {@code application_must_not_access_infrastructure}). Histórico completo da migração
 * fatias-planas→fNNN em {@code .claude/refactor.md}.
 *
 * <h2>Decomposição de alto nível</h2>
 * <pre>
 * feature
 * ├── f000  fatia-base — todas as demais podem depender dela, ela de nenhuma
 * │   ├── stream/SSE   GET  /api/{uuid}/stream (Server-Sent Events)
 * │   ├── deletion     contrato de exclusão compartilhado (accounts/cards/categories/tags)
 * │   ├── auth         POST /login — token opaco rotativo
 * │   ├── UserGuards   guarda de propriedade/anti-IDOR
 * │   ├── costcenter   GET  /api/cost-center — sem namespace de usuário
 * │   ├── version      GET  /api/version — sem namespace de usuário
 * │   └── closing      GET/POST/DELETE /api/{uuid}/accounts/closing
 * ├── f001  self-service — GET/PATCH /api/me (nome + preferências write-through)
 * ├── f002  accounts — CRUD /api/{uuid}/accounts (+ cards e balance fundidos: sem overlay próprio)
 * │   ├── cards     CRUD …/{accId}/cards
 * │   └── balance   GET  …/balance?period=|year=
 * ├── f003  tags — CRUD /api/{uuid}/tags; reage a {@code TransactionsDeleted} (evento da base f000)
 * ├── f004  categories — CRUD /api/{uuid}/categories
 * ├── f005  transactions + transfer — CRUD …/{accId}/transactions, POST …/transactions/transfer;
 * │         publica {@code TransactionsDeleted} após excluir transações
 * ├── f006  importação de extrato/fatura — POST …/transactions/import/preview|confirm
 * │         (parsers BTG/Santander, casamento de cartão, sugestão de categoria)
 * ├── f009  dashboard — GET /api/{uuid}/dashboard/result
 * ├── f999  Initialization routines
 * </pre>
 */
@NullMarked
package br.cdb.feature;

import org.jspecify.annotations.NullMarked;
