package br.cdb.feature.auth;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LoginRequest(String username, String password) {}
