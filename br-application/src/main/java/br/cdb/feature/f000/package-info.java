/**
 * Fatia-base: transversais que todas as demais features (fNNN) podem consumir e que não
 * dependem de nenhuma feature — alvo sempre permitido na regra ArchUnit
 * {@code feature_slices_must_not_depend_on_sibling_slices}.
 *
 * <p>Consolida:
 * <ul>
 *   <li><b>stream/SSE</b> — {@code SSE}/{@code SseService}/{@code SseResource}
 *       ({@code GET /api/{uuid}/stream}); as fatias publicam eventos de domínio pós-mutação e nunca
 *       chamam {@code SSE.dispatch} direto — {@code f999} é o único dono do dispatch</li>
 *   <li><b>deletion</b> — vocabulário {@code DeletionStrategy}/{@code DeletionOutcome}/{@code Deletions},
 *       contrato uniforme de exclusão (accounts, cards, categories, tags); evento cross-feature
 *       {@code TransactionsDeleted} ({@code _0_domain.event}), publicado por quem apaga transações
 *       (f006 ou cascata de conta/categoria/tag) e reagido best-effort por f006/f004</li>
 *   <li><b>auth</b> — {@code LoginResource} ({@code POST /login}, emissão de token)</li>
 *   <li><b>guards</b> — {@code UserGuards}, checagem de propriedade/anti-IDOR</li>
 *   <li><b>costcenter</b> — {@code CostCenterResource} ({@code GET /api/cost-center}, catálogo fixo)</li>
 *   <li><b>version</b> — {@code VersionResource} ({@code GET /api/version})</li>
 *   <li><b>closing</b> — {@code ClosingService}/{@code ClosingRepository}/{@code ClosingResource}
 *       ({@code /api/{uuid}/accounts/closing}); gate de política síncrono ({@code validateDate})
 *       consumido por transactions/transfer</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.f000;

import org.jspecify.annotations.NullMarked;
