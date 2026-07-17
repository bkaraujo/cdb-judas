package br.cdb.feature.finance.accounts.closing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ClosingResponse(@Nullable String period) {}
