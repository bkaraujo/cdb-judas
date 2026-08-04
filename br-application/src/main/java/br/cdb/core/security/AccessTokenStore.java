package br.cdb.core.security;

import br.cdb.core.web.HTTPSession;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Índice das {@link HTTPSession} vivas. A sessão é criada no login e carrega tudo sobre si — as duas
 * identidades, o token vivo e os efêmeros; aqui só existem os mapas que a encontram por cada uma
 * dessas chaves, todos apontando para a mesma instância.
 *
 * <p>{@code @Singleton} e todo método {@code synchronized}: os mapas nunca são tocados fora do
 * monitor, o que é o que torna {@link HashMap} seguro. Nada de {@link HTTPSession} escapa mutável —
 * é um record, então devolvê-la ao filtro de autenticação não expõe estado que possa rasgar.
 *
 * <h2>Uma sessão por usuário (política, não acidente)</h2>
 * {@link #open} revoga a sessão anterior <b>daquele</b> {@code userId}: logar de novo derruba a
 * antiga, inclusive em outro dispositivo. Usuários diferentes não interferem entre si — o despejo é
 * escopado por identidade. Trocar isto por N sessões simultâneas é fazer {@code byUser} apontar para
 * um conjunto; enquanto a política for sessão única, o mapa 1:1 é o que a garante.
 *
 * <h2>Expiração</h2>
 * Sessão caduca por <b>ociosidade</b> ({@link #SESSION_IDLE_TTL_MILLIS}), com a janela renovada a
 * cada uso autenticado — {@link #rotate} (toda requisição normal, pelo token rotativo) e
 * {@link #validate} (handshake do SSE, que valida sem rotacionar). Sem isto os mapas só cresciam:
 * token de usuário que nunca mais voltou ficava válido enquanto o processo vivesse.
 *
 * <p>A limpeza é preguiçosa e em dois níveis, para não pagar O(n) em toda requisição: o caminho
 * quente ({@code rotate}/{@code validate}/{@code consumeEphemeral}) derruba só a entrada que
 * consultou, em O(1); a varredura completa roda nos caminhos raros de emissão ({@link #open},
 * {@link #ephemeral}), o que basta para o total ficar limitado — uma sessão abandonada sobrevive no
 * máximo até a próxima emissão.
 *
 * <p>Os quatro índices só são escritos por {@link #index}/{@link #unindex}/{@link #replace}. Mexer
 * num mapa direto é como eles saem de sincronia.
 */
@Singleton
@NullMarked
public class AccessTokenStore {

    /** Ociosidade tolerada antes de a sessão do navegador caducar. Renovada a cada uso. */
    private static final long SESSION_IDLE_TTL_MILLIS = 30 * 60 * 1000L;

    /** Generoso o bastante pra uma chamada loopback, curto o bastante pra não valer a pena vazar. */
    private static final long EPHEMERAL_TTL_MILLIS = 10_000;

    /** Caminho quente: toda requisição autenticada chega por aqui. */
    private final Map<String, HTTPSession> byToken = new HashMap<>();

    /** Identidade de login — é o que garante a sessão única. */
    private final Map<String, HTTPSession> byUser = new HashMap<>();

    /** Identidade de negócio — {@code InternalApi} minta o efêmero tendo só o {@code personId}. */
    private final Map<String, HTTPSession> byPerson = new HashMap<>();

    /** Dona de cada efêmero em voo, para o consumo ser O(1). */
    private final Map<String, HTTPSession> byEphemeral = new HashMap<>();

    /** Abre sessão no login, revogando a anterior do mesmo usuário (sessão única — ver javadoc). */
    public synchronized HTTPSession open(String userId, String personId, String username) {
        purgeExpired();
        unindex(byUser.get(userId));

        val session = HTTPSession.opened(userId, personId, username, generate(),
                expiryFrom(SESSION_IDLE_TTL_MILLIS));
        index(session);
        return session;
    }

    /** Consome o token e emite o próximo; devolve a sessão já com o token novo dentro. */
    public synchronized Optional<HTTPSession> rotate(String token) {
        val session = liveSession(token);
        if (session == null) return Optional.empty();

        return Optional.of(replace(session,
                session.rotated(generate(), expiryFrom(SESSION_IDLE_TTL_MILLIS))));
    }

    /**
     * Valida <b>sem</b> rotacionar — é o que o canal SSE usa. Renova a janela mesmo assim: validar é
     * uso autenticado, e deixar de renovar aqui faria a sessão de quem só mantém o stream aberto
     * envelhecer como se estivesse ociosa.
     */
    public synchronized Optional<HTTPSession> validate(String token) {
        val session = liveSession(token);
        if (session == null) return Optional.empty();

        return Optional.of(replace(session, session.renewed(expiryFrom(SESSION_IDLE_TTL_MILLIS))));
    }

    /**
     * Token de uso único para chamada HTTP interna fatia→fatia ({@code InternalApi}, f000) — nunca
     * reaproveitar o token de sessão aqui: ele rotaciona, e emitir um durante uma requisição em
     * andamento apagaria o token recém-rotacionado do navegador antes de ele chegar na resposta.
     *
     * <p>Pendurado na sessão de quem o pediu, encontrada pelo {@code personId} (única identidade que
     * as features enxergam, via {@code HTTPRequest.personId()}). Chamada aninhada resolve para a
     * mesma sessão: a requisição loopback se autentica com o efêmero e segue carregando o mesmo
     * {@code personId}. Sem sessão viva não há o que pendurar — é bug de chamador, e falha alto.
     */
    public synchronized String ephemeral(String personId) {
        purgeExpired();
        val session = byPerson.get(personId);
        if (session == null) {
            throw new IllegalStateException("Sem sessão viva para a pessoa " + personId
                    + " — token efêmero só existe dentro de uma requisição autenticada");
        }

        val token = generate();
        replace(session, session.withEphemeral(token, expiryFrom(EPHEMERAL_TTL_MILLIS)));
        return token;
    }

    /** Consome (uso único) o efêmero, devolvendo a sessão dona se ele ainda valia. */
    public synchronized Optional<HTTPSession> consumeEphemeral(String token) {
        val session = byEphemeral.get(token);
        if (session == null) return Optional.empty();

        val live = session.hasLiveEphemeral(token, System.currentTimeMillis());
        replace(session, session.withoutEphemeral(token));
        return live ? Optional.of(session) : Optional.empty();
    }

    // ── Interno ────────────────────────────────────────────────────

    /** Sessão viva do token, ou {@code null} — expirada sai do índice na consulta (limpeza O(1)). */
    private @Nullable HTTPSession liveSession(String token) {
        val session = byToken.get(token);
        if (session == null) return null;
        if (session.isExpired(System.currentTimeMillis())) {
            unindex(session);
            return null;
        }
        return session;
    }

    /** Troca a sessão por sua versão nova em todos os índices. */
    private HTTPSession replace(HTTPSession current, HTTPSession updated) {
        unindex(current);
        index(updated);
        return updated;
    }

    private void index(HTTPSession session) {
        byToken.put(session.token(), session);
        byUser.put(session.userId(), session);
        byPerson.put(session.personId(), session);
        session.ephemeralTokens().keySet().forEach(token -> byEphemeral.put(token, session));
    }

    private void unindex(@Nullable HTTPSession session) {
        if (session == null) return;
        byToken.remove(session.token(), session);
        byUser.remove(session.userId(), session);
        byPerson.remove(session.personId(), session);
        session.ephemeralTokens().keySet().forEach(token -> byEphemeral.remove(token, session));
    }

    /** Varredura completa — só nos caminhos de emissão, que são raros (ver javadoc da classe). */
    private void purgeExpired() {
        val now = System.currentTimeMillis();
        for (val session : new ArrayList<>(byToken.values())) {
            if (session.isExpired(now)) {
                unindex(session);
                continue;
            }
            val cleaned = session.withoutExpiredEphemerals(now);
            if (cleaned != session) replace(session, cleaned);
        }
    }

    private static long expiryFrom(long ttlMillis) {
        return System.currentTimeMillis() + ttlMillis;
    }

    private String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
