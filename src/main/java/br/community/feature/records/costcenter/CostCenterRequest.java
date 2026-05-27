package br.community.feature.records.costcenter;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CostCenterRequest(
        @NotBlank String description
) {
}
