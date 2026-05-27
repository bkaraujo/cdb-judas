package br.community.context.shared._1_application;

import br.community.context.shared._0_domain.model.DomainError;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

@Getter
@NullMarked
public class DomainException extends RuntimeException {
    private final DomainError error;

    public DomainException(DomainError error) {
        super(switch (error) {
            case DomainError.NotFound(var m) -> m;
            case DomainError.BusinessRule(var m) -> m;
            case DomainError.Validation(var m) -> m;
            case DomainError.Conflict(var m) -> m;
        });
        this.error = error;
    }
}
