package br.cdb.feature.f005._2_infrastructure.web.response;

import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;

/** Corpo mínimo (só {@code nature}) do endpoint interno {@code GET /categories/{id}/nature} —
 *  consumido pelo cliente {@code F005Api} da própria fatia (sobre {@code InternalApi}), que declara o
 *  próprio DTO local de deserialização em vez de expor este tipo ao chamador. */
@NullMarked
public record CategoryNatureResponse(Nature nature) {}
