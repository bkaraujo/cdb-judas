package br.cdb.core.security;

import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
@NullMarked
public class AccessTokenStore {

    @NullMarked
    public record RotationResult(String userId, String nextToken) {}

    @NullMarked
    private record EphemeralToken(String personId, long expiresAtMillis) {}

    /** Generoso o bastante pra uma chamada loopback, curto o bastante pra não valer a pena vazar. */
    private static final long EPHEMERAL_TTL_MILLIS = 10_000;

    private final Map<String, String> tokenToUser = new HashMap<>();
    private final Map<String, String> userToToken = new HashMap<>();
    private final Map<String, EphemeralToken> ephemeralTokens = new HashMap<>();

    public synchronized String issue(String userId) {
        val old = userToToken.remove(userId);
        if (old != null) tokenToUser.remove(old);

        val token = generate();
        userToToken.put(userId, token);
        tokenToUser.put(token, userId);
        return token;
    }

    public synchronized Optional<RotationResult> rotate(String incomingToken) {
        val userId = tokenToUser.remove(incomingToken);
        if (userId == null) return Optional.empty();

        val nextToken = generate();
        tokenToUser.put(nextToken, userId);
        userToToken.put(userId, nextToken);
        return Optional.of(new RotationResult(userId, nextToken));
    }

    public synchronized Optional<String> validate(String token) {
        return Optional.ofNullable(tokenToUser.get(token));
    }

    /**
     * Token de uso único para chamada HTTP interna fatia→fatia ({@code InternalApi}, f000) — nunca
     * reaproveitar {@link #issue} aqui: aquele mapa é 1 token por usuário, então mintar um durante
     * uma requisição em andamento apagaria o token de sessão recém-rotacionado do navegador antes de
     * ele chegar na resposta. Identificado direto pelo personId (única identidade que as features
     * enxergam, via {@code HTTPRequest.personId()}) — sem volta a userId.
     */
    public synchronized String issueEphemeral(String personId) {
        val token = generate();
        ephemeralTokens.put(token, new EphemeralToken(personId, System.currentTimeMillis() + EPHEMERAL_TTL_MILLIS));
        return token;
    }

    /** Consome (uso único) o token efêmero, devolvendo o personId se ainda válido e não expirado. */
    public synchronized Optional<String> consumeEphemeral(String token) {
        val entry = ephemeralTokens.remove(token);
        if (entry == null || entry.expiresAtMillis() < System.currentTimeMillis()) return Optional.empty();
        return Optional.of(entry.personId());
    }

    private String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
