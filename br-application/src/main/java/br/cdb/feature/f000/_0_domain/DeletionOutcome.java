package br.cdb.feature.f000._0_domain;

import br.cdb.feature.f000._1_application.Deletions;
import org.jspecify.annotations.NullMarked;

/**
 * Resultado de uma exclusão orquestrada pelo {@code *UseCase} da fatia dona do recurso
 * ({@code f002}, {@code f004} e {@code f005}, todos em {@code WriteUseCase}):
 * ou completou, ou foi bloqueada
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
