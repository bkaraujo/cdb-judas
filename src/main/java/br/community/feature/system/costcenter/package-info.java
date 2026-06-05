/**
 * Catálogo de centros de custo do sistema.
 *
 * <p>Expõe uma única rota somente-leitura sem namespace de usuário:
 * <pre>GET /api/cost-center</pre>
 *
 * <p>Os centros de custo são dados fixos de configuração da plataforma (ex.: departamentos,
 * projetos, naturezas de despesa). Não há operações de escrita por HTTP; a carga é feita
 * via {@link br.community.feature.system.costcenter.CostCenterCatalog}.
 */
package br.community.feature.system.costcenter;
