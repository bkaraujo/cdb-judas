package br.cdb.feature.f005._2_infrastructure.web.response;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Corpo mínimo (só {@code id}) do endpoint interno {@code GET /categories/transfer} — consumido
 *  pelo cliente {@code F005Api} da própria fatia (sobre {@code InternalApi}), que declara o próprio
 *  DTO local de deserialização em vez de expor este tipo ao chamador. */
@NullMarked
public record TransferCategoryResponse(UUID id) {}
