package br.cdb.feature.f010._2_infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record ImportRuleRequest(
        @NotBlank @Size(min = 3, max = 255) String name,
        @Nullable UUID accountId,
        @Nullable UUID categoryId,
        @Nullable UUID costCenterId
) {}
