package br.commons.business;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface BusinessError {
    
    @NullMarked
    record NotFound(String message) implements BusinessError {}
    
    @NullMarked
    record BusinessRule(String message) implements BusinessError {}

    @NullMarked
    record Validation(String message) implements BusinessError {

        public Validation(String message, Object ... args) {
            this(String.format(message, args));
        }

    }

    @NullMarked
    record Conflict(String message) implements BusinessError {
    }

}
