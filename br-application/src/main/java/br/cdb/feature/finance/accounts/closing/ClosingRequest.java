package br.cdb.feature.finance.accounts.closing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ClosingRequest(
        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Invalid format. Use YYYY-MM")
        String period
) {}
