package br.community.feature.system.auth;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

@NullMarked
@Getter
public final class AuthUserDetails extends User {

    private final String id;

    public AuthUserDetails(String id, String username, String password) {
        super(username, password, Collections.emptyList());
        this.id = id;
    }
}
