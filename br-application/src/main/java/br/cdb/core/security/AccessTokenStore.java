package br.cdb.core.security;

import br.cdb.core.CoreModule;
import br.cdb.core.web.HTTPSession;
import br.cdb.core.web.InternalCall;
import br.commons.Logger;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Índice das {@link HTTPSession} vivas. A sessão é criada no login e carrega tudo sobre si — as duas
 * identidades, o token vivo e os efêmeros; aqui só existem os mapas que a encontram por cada uma
 * dessas chaves, todos apontando para a mesma instância.
 *
 * <p>{@code @Singleton} e todo método {@code synchronized}: os mapas nunca são tocados fora do
 * monitor, o que é o que torna {@link HashMap} seguro. Nada de {@link HTTPSession} escapa mutável —
 * é um record, então devolvê-la ao filtro de autenticação não expõe estado que possa rasgar.
 *
 * <h2>Múltiplas sessões por usuário</h2>
 * {@link #open} cria uma nova sessão sem revogar as anteriores do mesmo {@code userId} — o que
 * permite abas simultâneas no browser ou o mesmo usuário em dispositivos diferentes. {@code byUser}
 * aponta para um {@link java.util.Set} de sessões; a revogação por identidade (ex: "derrubar todas
 * as sessões deste usuário") pode ser reintroduzida iterando sobre esse conjunto.
 *
 * <h2>Expiração</h2>
 * Sessão caduca por <b>ociosidade</b> ({@value #SESSION_IDLE_KEY} em {@code application.yaml}), com a
 * janela renovada a cada uso autenticado — {@link #rotate} (toda requisição normal, pelo token
 * rotativo) e {@link #validate} (handshake do SSE, que valida sem rotacionar). Sem isto os mapas só
 * cresciam: token de usuário que nunca mais voltou ficava válido enquanto o processo vivesse.
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

    /** Chave em {@code application.yaml}; ausente, vale {@link #DEFAULT_SESSION_IDLE_MINUTES}. */
    private static final String SESSION_IDLE_KEY = "cdb.security.session.idle-timeout-minutes";

    private static final long DEFAULT_SESSION_IDLE_MINUTES = 30;

    /** Generoso o bastante pra uma chamada loopback, curto o bastante pra não valer a pena vazar. */
    private static final long EPHEMERAL_TTL_MILLIS = 10_000;

    /**
     * Ociosidade tolerada antes de a sessão do navegador caducar, renovada a cada uso. Lido uma vez
     * ({@code @Singleton}) de {@value #SESSION_IDLE_KEY} — {@code CoreModule.yaml} é estático, então
     * já está carregado quando o CDI constrói este bean.
     */
    private final long sessionIdleTtlMillis = readSessionIdleTtlMillis();

    /** Prefixo do {@code userId} sintético das sessões internas — ver {@link #openInternal}. */
    private static final String INTERNAL_USER_PREFIX = "internal:";

    /** Caminho quente: toda requisição autenticada chega por aqui. */
    private final Map<String, HTTPSession> byToken = new HashMap<>();

    /** Identidade de login — suporta N sessões simultâneas (múltiplas abas/dispositivos). */
    private final Map<String, Set<HTTPSession>> byUser = new HashMap<>();

    /** Identidade de negócio — {@code InternalApi} minta o efêmero tendo só o {@code personId}. */
    private final Map<String, HTTPSession> byPerson = new HashMap<>();

    /** Dona de cada efêmero em voo, para o consumo ser O(1). */
    private final Map<String, HTTPSession> byEphemeral = new HashMap<>();

    /** Abre sessão no login; não revoga sessões anteriores do mesmo usuário (ver javadoc da classe). */
    public synchronized HTTPSession open(String userId, String personId, String username) {
        purgeExpired();

        val session = HTTPSession.opened(userId, personId, username, generate(),
                expiryFrom(sessionIdleTtlMillis));
        index(session);
        br.commons.MessageBus.submit(new SessionEvents.Login(personId));
        return session;
    }

    /** Consome o token e emite o próximo; devolve a sessão já com o token novo dentro. */
    public synchronized Optional<HTTPSession> rotate(String token) {
        val session = liveSession(token);
        if (session == null) return Optional.empty();

        return Optional.of(replace(session,
                session.rotated(generate(), expiryFrom(sessionIdleTtlMillis))));
    }

    /**
     * Valida <b>sem</b> rotacionar — é o que o canal SSE usa. Renova a janela mesmo assim: validar é
     * uso autenticado, e deixar de renovar aqui faria a sessão de quem só mantém o stream aberto
     * envelhecer como se estivesse ociosa.
     */
    public synchronized Optional<HTTPSession> validate(String token) {
        val session = liveSession(token);
        if (session == null) return Optional.empty();

        return Optional.of(replace(session, session.renewed(expiryFrom(sessionIdleTtlMillis))));
    }

    /**
     * Token de uso único para chamada HTTP interna fatia→fatia ({@code InternalApi}, f000) — nunca
     * reaproveitar o token de sessão aqui: ele rotaciona, e emitir um durante uma requisição em
     * andamento apagaria o token recém-rotacionado do navegador antes de ele chegar na resposta.
     *
     * <p>Pendurado na sessão de quem o pediu, encontrada pelo {@code personId} (única identidade que
     * as features enxergam, via {@code HTTPRequest.personId()}). Chamada aninhada resolve para a
     * mesma sessão: a requisição loopback se autentica com o efêmero e segue carregando o mesmo
     * {@code personId}. Sem sessão de navegador viva, só quem declarou explicitamente o ator via
     * {@link InternalCall#as} pode receber uma sessão sintética aqui ({@link #openInternal}) — para
     * qualquer outro {@code personId} sem sessão viva, falha alto: é bug de chamador (sessão expirou
     * numa corrida, ou {@code personId} nunca existiu), não caso a mascarar com uma autenticação
     * silenciosa.
     */
    public synchronized String ephemeral(String personId) {
        purgeExpired();
        val existing = byPerson.get(personId);
        val session = existing != null ? existing : openInternalFor(personId);

        val token = generate();
        replace(session, session.withEphemeral(token, expiryFrom(EPHEMERAL_TTL_MILLIS)));
        return token;
    }

    private HTTPSession openInternalFor(String personId) {
        if (!personId.equals(InternalCall.personId())) {
            throw new IllegalStateException("Sem sessão viva para a pessoa " + personId
                    + " — token efêmero só existe dentro de uma requisição autenticada ou de "
                    + "InternalCall.as(" + personId + ", ..)");
        }
        return openInternal(personId);
    }

    /**
     * Sessão sem navegador nenhum atrás, para quem chama uma fatia fora de uma requisição HTTP
     * ({@code InternalCall.as(personId, ..)} no boot, num job {@code @Scheduled} ou num listener do
     * {@code MessageBus}) — só alcançada depois que {@link #openInternalFor} confirma que
     * {@code personId} é exatamente o ator declarado por {@link InternalCall#as}. Antes isto era
     * sempre um {@link IllegalStateException} — o que amarrava todo {@code FNNNApi} a uma sessão de
     * navegador viva e derrubava o boot, que recomputa saldos antes de existir qualquer login.
     *
     * <p>Vive o mesmo {@value #EPHEMERAL_TTL_MILLIS}ms do efêmero que ela existe para emitir, e não
     * o ocioso da sessão de navegador: fora dessa janela não sobra nada de aproveitável, e a
     * {@link #purgeExpired} da próxima emissão a recolhe. O {@code userId} é sintético
     * ({@value #INTERNAL_USER_PREFIX} + personId) para nunca colidir com o {@code SYS_USER.ID} de um
     * login real no índice {@code byUser}.
     *
     * <p>Emite {@link SessionEvents.Login} para trigger cache populate (D1-a).
     */
    private HTTPSession openInternal(String personId) {
        val session = HTTPSession.opened(INTERNAL_USER_PREFIX + personId, personId, "internal",
                generate(), expiryFrom(EPHEMERAL_TTL_MILLIS));
        index(session);
        br.commons.MessageBus.submit(new SessionEvents.Login(personId));
        return session;
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
            forget(session);
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

    /** Remove a sessão e emite {@link SessionEvents.Logout} se ninguém da pessoa está vivo. */
    private void forget(HTTPSession session) {
        unindex(session);
        if (!byPerson.containsKey(session.personId())) {
            br.commons.MessageBus.submit(new SessionEvents.Logout(session.personId()));
        }
    }

    private void index(HTTPSession session) {
        byToken.put(session.token(), session);
        byUser.computeIfAbsent(session.userId(), k -> new HashSet<>()).add(session);
        byPerson.put(session.personId(), session);
        session.ephemeralTokens().keySet().forEach(token -> byEphemeral.put(token, session));
    }

    private void unindex(@Nullable HTTPSession session) {
        if (session == null) return;
        byToken.remove(session.token(), session);
        val userSessions = byUser.get(session.userId());
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) byUser.remove(session.userId());
        }
        byPerson.remove(session.personId(), session);
        session.ephemeralTokens().keySet().forEach(token -> byEphemeral.remove(token, session));
    }

    /** Varredura completa — só nos caminhos de emissão, que são raros (ver javadoc da classe). */
    private void purgeExpired() {
        val now = System.currentTimeMillis();
        for (val session : new ArrayList<>(byToken.values())) {
            if (session.isExpired(now)) {
                forget(session);
                continue;
            }
            val cleaned = session.withoutExpiredEphemerals(now);
            if (cleaned != session) replace(session, cleaned);
        }
    }

    /**
     * Minutos de {@value #SESSION_IDLE_KEY}, em milissegundos. Valor não-positivo cai no default e
     * avisa: aceitá-lo faria toda sessão nascer vencida — ninguém mais conseguiria usar o sistema, e
     * a causa (uma linha de YAML) não apareceria em lugar nenhum.
     */
    private static long readSessionIdleTtlMillis() {
        val minutes = CoreModule.yaml.asLong(SESSION_IDLE_KEY, DEFAULT_SESSION_IDLE_MINUTES);
        if (minutes <= 0) {
            Logger.warn("%s inválido (%d): usando o padrão de %d minutos",
                    SESSION_IDLE_KEY, minutes, DEFAULT_SESSION_IDLE_MINUTES);
            return TimeUnit.MINUTES.toMillis(DEFAULT_SESSION_IDLE_MINUTES);
        }
        Logger.debug("Sessão expira por ociosidade em %d minutos", minutes);
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    private static long expiryFrom(long ttlMillis) {
        return System.currentTimeMillis() + ttlMillis;
    }

    private String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
