package br.cdb.feature.f000._2_infrastructure;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LoginRequest(String username, String password) {}
