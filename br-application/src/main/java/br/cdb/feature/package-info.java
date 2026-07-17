/**
 * Raiz da camada de features da aplicação.
 *
 * <p>As features representam as capacidades funcionais entregues pelo backend ao cliente HTTP.
 * Cada subpacote agrupa um conjunto coeso de endpoints REST e seus artefatos de suporte
 * (DTOs, módulos de configuração, listeners de domínio).
 *
 * <h2>Decomposição de alto nível</h2>
 * <pre>
 * feature
 * ├── auth       POST /login — fatia-base sem namespace de usuário; as demais podem depender
 * │              dela, nunca o contrário (ver ArchitectureTest)
 * ├── dashboard  GET  /api/{uuid}/dashboard/result
 * ├── finance    — fatias financeiras escopadas por usuário (ver {@link br.cdb.feature.finance})
 * │   ├── accounts     CRUD /api/{uuid}/accounts  + sub-recursos
 * │   │   ├── balance      GET  …/value?period=|year=
 * │   │   ├── cards        CRUD …/{accId}/cards
 * │   │   ├── closing      GET/POST/DELETE …/closing
 * │   │   ├── statement    parsing de extrato/fatura PDF (suporte, sem rota)
 * │   │   └── transactions CRUD …/{accId}/transactions
 * │   │       ├── transfer  POST …/transactions/transfer
 * │   │       └── importer  POST …/transactions/import/preview|confirm
 * │   ├── categories   CRUD /api/{uuid}/categories
 * │   ├── tags         CRUD /api/{uuid}/tags
 * │   ├── costcenter   GET  /api/cost-center — sem namespace de usuário
 * │   └── deletion     contrato de exclusão compartilhado (accounts/cards/categories/tags)
 * ├── stream     GET  /api/{uuid}/stream  (Server-Sent Events)
 * ├── version    GET  /api/version — sem namespace de usuário
 * └── user       — fatia do agregado {@code User}; pacote raiz tem o {@code UserUseCase}
 *                (único use case da fatia, orquestra as demais features acima)
 *     ├── profile    GET/PATCH /api/me — self-service
 *     │   ├── api         DTOs HTTP (request/response)
 *     │   └── preference  {@code Preferences}/{@code PreferencesRepository}
 *     └── seed       provisionamento inicial (usuário + categorias default)
 * </pre>
 */
@NullMarked
package br.cdb.feature;

import org.jspecify.annotations.NullMarked;