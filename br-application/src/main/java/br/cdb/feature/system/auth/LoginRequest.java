package br.cdb.feature.system.auth;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LoginRequest(String username, String password) {}
