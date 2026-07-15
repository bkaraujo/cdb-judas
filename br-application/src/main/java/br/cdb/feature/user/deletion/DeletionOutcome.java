package br.cdb.feature.user.deletion;

import org.jspecify.annotations.NullMarked;

/**
 * Resultado de uma exclusão orquestrada pelo {@code UserUseCase}: ou completou, ou foi bloqueada
 * por transações vinculadas (exclusão simples, sem estratégia) — nesse caso o Resource traduz para
 * o 409 rico de {@link Deletions#linkedConflict(String, int, String)}.
 */
@NullMarked
public sealed interface DeletionOutcome {

    /** Exclusão sem estratégia bloqueada: há {@code count} transações vinculadas ao recurso. */
    @NullMarked
    record Linked(int count) implements DeletionOutcome {}

    @NullMarked
    record Completed() implements DeletionOutcome {}
}
