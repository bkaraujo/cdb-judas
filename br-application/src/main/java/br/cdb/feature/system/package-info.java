/**
 * Features de sistema — recursos globais sem namespace de usuário.
 *
 * <p>Endpoints aqui expostos não carregam o segmento {@code {uuid}} de workspace,
 * pois representam dados ou canais de infraestrutura compartilhados.
 *
 * <ul>
 *   <li>{@link br.cdb.feature.system.auth} — autenticação: login e emissão de sessão ({@code POST /login})</li>
 *   <li>{@link br.cdb.feature.system.costcenter} — catálogo somente-leitura de centros de custo</li>
 *   <li>{@link br.cdb.feature.system.VersionResource} — versão da aplicação ({@code GET /api/version})</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.system;

import org.jspecify.annotations.NullMarked;
