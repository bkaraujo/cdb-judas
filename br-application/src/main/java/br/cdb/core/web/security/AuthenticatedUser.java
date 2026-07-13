package br.cdb.core.web.security;

import org.jspecify.annotations.NullMarked;

import java.security.Principal;

@NullMarked
public record AuthenticatedUser(String id, String username) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
