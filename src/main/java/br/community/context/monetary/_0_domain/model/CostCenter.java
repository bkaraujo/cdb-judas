package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record CostCenter(
        UUID id,
        String description,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    /** Centro de custo padrão "Fixo" (semente global). */
    public static final UUID FIXO_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");

    /** Centro de custo padrão "Variável" (semente global).*/
    public static final UUID VARIAVEL_ID = UUID.fromString("d0000000-0000-0000-0000-000000000002");

    public CostCenter(UUID id, String description) {
        this(id, description, null, null);
    }
}
