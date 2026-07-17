/**
 * Catálogo de centros de custo do sistema.
 *
 * <p>Expõe uma única rota somente-leitura sem namespace de usuário:
 * <pre>GET /api/cost-center</pre>
 *
 * <p>Os centros de custo são valores fixos do domínio monetário ({@code CostCenter.FIXO}/
 * {@code CostCenter.VARIAVEL}, em {@code br.cdb.context.monetary._0_domain.model}), não uma
 * tabela configurável. Não há operações de escrita por HTTP.
 */
@NullMarked
package br.cdb.feature.finance.costcenter;

import org.jspecify.annotations.NullMarked;
