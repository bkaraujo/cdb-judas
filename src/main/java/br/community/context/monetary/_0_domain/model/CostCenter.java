package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record CostCenter(
        UUID id,
        String description
) {
    /** Centro de custo padrão "Fixo" (semente global). */
    public static final CostCenter FIXO = new CostCenter(UUID.fromString("d0000000-0000-0000-0000-000000000001"), "Fixo");

    /** Centro de custo padrão "Variável" (semente global).*/
    public static final CostCenter VARIAVEL = new CostCenter(UUID.fromString("d0000000-0000-0000-0000-000000000002"), "Variável");
}
