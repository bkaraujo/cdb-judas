package br.commons.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TwoDecimalPlacesValidatorTest {

    private final TwoDecimalPlacesValidator validator = new TwoDecimalPlacesValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @ParameterizedTest
    @CsvSource({
            "10, true",
            "10.0, true",
            "10.00, true",
            "10.5, true",
            "10.55, true",
            "10.550, true",
            "10.555, false",
            "0.0001, false"
    })
    void testIsValid(String value, boolean expected) {
        assertEquals(expected, validator.isValid(new BigDecimal(value), context));
    }

    @Test
    void testNullIsValid() {
        assertTrue(validator.isValid(null, context));
    }
}
