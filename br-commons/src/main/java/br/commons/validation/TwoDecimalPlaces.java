package br.commons.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.*;

@NullMarked
@Documented
@Constraint(validatedBy = TwoDecimalPlacesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TwoDecimalPlaces {
    String message() default "{cdb.validation.twoDecimalPlaces}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
