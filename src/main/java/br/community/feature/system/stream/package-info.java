/**
 * Canal de eventos em tempo real via Server-Sent Events (SSE).
 *
 * <p>Rota exposta:
 * <pre>GET /api/{uuid}/stream  (Content-Type: text/event-stream)</pre>
 *
 * <p>Permite que o frontend mantenha uma conexão persistente e receba notificações
 * de eventos de domínio sem polling — ex.: importação concluída, sincronização de dados.
 * Cada conexão é registrada no {@link br.community.feature.system.stream.SseService}
 * e descartada automaticamente ao término ou timeout.
 *
 * <p>O segmento {@code {uuid}} está presente na rota por convenção de roteamento,
 * mas o controlador é agnóstico a ele; a guarda de segurança valida a propriedade
 * externamente.
 */
@NullMarked
package br.community.feature.system.stream;

import org.jspecify.annotations.NullMarked;
