package br.commons.business;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

@Getter
@NullMarked
public class BusinessException extends RuntimeException {
    private final BusinessError error;

    public BusinessException(BusinessError error) {
        super(switch (error) {
            case BusinessError.NotFound(var m) -> m;
            case BusinessError.BusinessRule(var m) -> m;
            case BusinessError.Validation(var m) -> m;
            case BusinessError.Conflict(var m) -> m;
        });
        this.error = error;
    }
}
