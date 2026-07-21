/**
 * Fatia-base: transversais que todas as demais features (fNNN) podem consumir e que não
 * dependem de nenhuma feature (ArchUnit {@code base_slice_must_not_depend_on_features}).
 *
 * <p>Consolida:
 * <ul>
 *   <li><b>stream/SSE</b> — {@code SSE}/{@code SseService}/{@code SseController}
 *       ({@code GET /api/{uuid}/stream}); features despacham eventos diretamente pós-mutação</li>
 *   <li><b>deletion</b> — vocabulário {@code DeletionStrategy}/{@code DeletionOutcome}/{@code Deletions},
 *       contrato uniforme de exclusão (accounts, cards, categories, tags)</li>
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
