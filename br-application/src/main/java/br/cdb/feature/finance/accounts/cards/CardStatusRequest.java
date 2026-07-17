package br.cdb.feature.finance.accounts.cards;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardStatusRequest(boolean active) {}
