package br.cdb.feature.f005._2_infrastructure.web.response;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Corpo mínimo (só {@code id}) do endpoint interno {@code GET /categories/transfer} — consumido
 *  via {@code InternalApi} por {@code f006.TransactionUseCase}, que não importa este tipo (declara
 *  o próprio DTO local de deserialização; zero tipo cross-slice, mesma regra dos antigos ports). */
@NullMarked
public record TransferCategoryResponse(UUID id) {}
