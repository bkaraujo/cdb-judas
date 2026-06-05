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
 * ├── system   — recursos transversais, sem namespace de usuário ({uuid})
 * │   ├── costcenter   GET /api/cost-center
 * │   └── stream       GET /api/{uuid}/stream  (Server-Sent Events)
 * └── user     — recursos escopados por usuário/workspace ({uuid})
 *     ├── accounts     CRUD /api/{uuid}/accounts  + sub-recursos
 *     │   ├── balance       GET  …/balance?period=|year=
 *     │   ├── closing       GET/POST/DELETE …/closing
 *     │   ├── statement     GET  …/statements/{yyyyMM}
 *     │   │   └── importer  POST …/transactions/import/preview|confirm
 *     │   └── transactions  CRUD …/{accId}/transactions
 *     ├── categories   CRUD /api/{uuid}/categories
 *     ├── dashboard    GET  /api/{uuid}/dashboard/result
 *     └── tags         CRUD /api/{uuid}/tags
 * </pre>
 */
package br.community.feature;