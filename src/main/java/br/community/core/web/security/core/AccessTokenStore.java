package br.community.core.web.security.core;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@NullMarked
public class AccessTokenStore {

    @NullMarked
    public record RotationResult(String userId, String nextToken) {}

    private final Map<String, String> tokenToUser = new HashMap<>();
    private final Map<String, String> userToToken = new HashMap<>();

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

    private String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
