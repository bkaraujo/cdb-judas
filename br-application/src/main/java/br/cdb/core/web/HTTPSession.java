package br.cdb.core.web;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

/**
 * Sessão de um usuário autenticado, criada no ato do login e mantida em memória por
 * {@code AccessTokenStore}. Carrega em si <b>tudo</b> que identifica a sessão — as duas identidades,
 * o token vivo e os efêmeros em voo — de modo que o store seja só índice: os mapas dele apontam
 * todos para a mesma sessão, e cada chave que eles usam é um campo daqui.
 *
 * <h2>As duas identidades</h2>
 * <ul>
 *   <li>{@code userId} — identidade de <b>login</b> ({@code F000_USER}); é a ela que o token se
 *       amarra e por ela que a sessão única é garantida (logar de novo derruba a anterior).</li>
 *   <li>{@code personId} — identidade de <b>negócio</b> ({@code F000_PERSON}); é a chave de todas
 *       as tabelas de dados e o {@code {uuid}} das rotas. É o que as features enxergam, via
 *       {@code HTTPRequest.personId()}.</li>
 * </ul>
 * Guardar as duas aqui é o que dispensa o {@code AuthenticationFilter} de reconsultar o usuário no
 * banco a cada requisição só para voltar de {@code userId} a {@code personId}.
 *
 * <h2>Efêmeros: mapa, não campo único</h2>
 * {@code InternalApi} (f000) minta um token de uso único por leitura cross-slice. Duas requisições
 * concorrentes da mesma sessão mintam dois — um campo único faria a segunda emissão despejar a
 * primeira em silêncio, e o efeito apareceria como um 401 na chamada loopback, longe da causa.
 * Daí o mapa {@code token -> expiração}.
 *
 * <p>Record de propósito: o store devolve a sessão para fora (filtro de autenticação), e sendo
 * imutável não há estado rasgado para observar. Toda mudança — rotação, renovação, efêmero — é uma
 * cópia; quem reindexa é o store.
 */
@NullMarked
public record HTTPSession(
        String userId,
        String personId,
        String username,
        String token,
        long expiresAtMillis,
        Map<String, Long> ephemeralTokens
) {
    public HTTPSession {
        ephemeralTokens = Map.copyOf(ephemeralTokens);
    }

    /** Sessão recém-aberta no login: token vivo, nenhum efêmero. */
    public static HTTPSession opened(String userId, String personId, String username,
            String token, long expiresAtMillis) {
        return new HTTPSession(userId, personId, username, token, expiresAtMillis, Map.of());
    }

    public boolean isExpired(long now) {
        return expiresAtMillis < now;
    }

    /** Token novo e janela renovada — a cada resposta bem-sucedida (token rotativo). */
    public HTTPSession rotated(String nextToken, long expiresAtMillis) {
        return new HTTPSession(userId, personId, username, nextToken, expiresAtMillis, ephemeralTokens);
    }

    /** Só a janela renovada, mesmo token — o handshake do SSE valida sem rotacionar. */
    public HTTPSession renewed(long expiresAtMillis) {
        return new HTTPSession(userId, personId, username, token, expiresAtMillis, ephemeralTokens);
    }

    public HTTPSession withEphemeral(String ephemeralToken, long expiresAtMillis) {
        val updated = new HashMap<>(ephemeralTokens);
        updated.put(ephemeralToken, expiresAtMillis);
        return new HTTPSession(userId, personId, username, token, this.expiresAtMillis, updated);
    }

    /** Sem o efêmero indicado — consumido (uso único) ou expirado. */
    public HTTPSession withoutEphemeral(String ephemeralToken) {
        if (!ephemeralTokens.containsKey(ephemeralToken)) return this;
        val updated = new HashMap<>(ephemeralTokens);
        updated.remove(ephemeralToken);
        return new HTTPSession(userId, personId, username, token, expiresAtMillis, updated);
    }

    /** Sem os efêmeros já vencidos; a própria instância quando não há nada a limpar. */
    public HTTPSession withoutExpiredEphemerals(long now) {
        if (ephemeralTokens.values().stream().noneMatch(expiry -> expiry < now)) return this;
        val updated = new HashMap<String, Long>();
        ephemeralTokens.forEach((ephemeral, expiry) -> {
            if (expiry >= now) updated.put(ephemeral, expiry);
        });
        return new HTTPSession(userId, personId, username, token, expiresAtMillis, updated);
    }

    public boolean hasLiveEphemeral(String ephemeralToken, long now) {
        val expiry = ephemeralTokens.get(ephemeralToken);
        return expiry != null && expiry >= now;
    }
}
