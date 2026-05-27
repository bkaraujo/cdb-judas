package br.community.context.shared._0_domain.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface DomainError {
    
    @NullMarked
    record NotFound(String message) implements DomainError {}
    
    @NullMarked
    record BusinessRule(String message) implements DomainError {}

    @NullMarked
    record Validation(String message) implements DomainError {}

    @NullMarked
    record Conflict(String message) implements DomainError {}

}
