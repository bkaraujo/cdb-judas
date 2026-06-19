package br.community.context.monetary._1_application.command;

import br.community.context.monetary._0_domain.model.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record CategoryCommand(
        @NotBlank String name,
        @NotNull Transaction.Type nature,
        @Nullable UUID parentId
) {}
