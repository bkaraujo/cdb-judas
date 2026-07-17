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
 * │   ├── auth         POST /login
 * │   ├── costcenter   GET  /api/cost-center
 * │   └── version      GET  /api/version
 * └── user     — recursos do usuário autenticado (a maioria sob /api/{uuid}/…)
 *     ├── accounts     CRUD /api/{uuid}/accounts  + sub-recursos
 *     │   ├── value       GET  …/value?period=|year=
 *     │   ├── closing       GET/POST/DELETE …/closing
 *     │   ├── statement     parsing de extrato/fatura PDF (suporte, sem rota)
 *     │   └── transactions  CRUD …/{accId}/transactions
 *     │       ├── transfer  POST …/transactions/transfer
 *     │       └── importer  POST …/transactions/import/preview|confirm
 *     ├── categories   CRUD /api/{uuid}/categories
 *     ├── dashboard    GET  /api/{uuid}/dashboard/result
 *     ├── tags         CRUD /api/{uuid}/tags
 *     ├── stream       GET  /api/{uuid}/stream  (Server-Sent Events)
 *     └── profile      GET/PATCH /api/me
 * </pre>
 */
@NullMarked
package br.cdb.feature;

import org.jspecify.annotations.NullMarked;