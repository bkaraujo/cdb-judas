package br.community.core.web.security;

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
    public record RotationResult(String username, String nextToken) {}

    private final Map<String, String> tokenToUser = new HashMap<>();
    private final Map<String, String> userToToken = new HashMap<>();

    public synchronized String issue(String username) {
        val old = userToToken.remove(username);
        if (old != null) tokenToUser.remove(old);

        val token = generate();
        userToToken.put(username, token);
        tokenToUser.put(token, username);
        return token;
    }

    public synchronized Optional<RotationResult> rotate(String incomingToken) {
        val username = tokenToUser.remove(incomingToken);
        if (username == null) return Optional.empty();

        val nextToken = generate();
        tokenToUser.put(nextToken, username);
        userToToken.put(username, nextToken);
        return Optional.of(new RotationResult(username, nextToken));
    }

    public synchronized Optional<String> validate(String token) {
        return Optional.ofNullable(tokenToUser.get(token));
    }

    private String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
