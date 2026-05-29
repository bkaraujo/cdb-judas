package br.community.feature.user.accounts.closing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ClosingResponse(@Nullable String period) {}
