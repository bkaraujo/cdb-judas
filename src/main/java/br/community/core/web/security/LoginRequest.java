package br.community.core.web.security;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LoginRequest(String username, String password) {}
