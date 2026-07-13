package br.cdb.feature.user.accounts.cards;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardStatusRequest(boolean active) {}
