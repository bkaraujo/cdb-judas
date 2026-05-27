package br.community.core;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

@NullMarked
public class TwoDecimalPlacesValidator implements ConstraintValidator<TwoDecimalPlaces, @Nullable BigDecimal> {

    @Override
    public boolean isValid(@Nullable BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.stripTrailingZeros().scale() <= 2;
    }
}
