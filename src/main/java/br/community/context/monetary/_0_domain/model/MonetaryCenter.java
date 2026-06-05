package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record MonetaryCenter(
        UUID id,
        String description
) {
    /** Centro de custo padrão "Fixo" (semente global). */
    public static final UUID FIXO_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");

    /** Centro de custo padrão "Variável" (semente global).*/
    public static final UUID VARIAVEL_ID = UUID.fromString("d0000000-0000-0000-0000-000000000002");
}
