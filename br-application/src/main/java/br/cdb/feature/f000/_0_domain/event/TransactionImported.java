package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Publicado por {@code StatementImportProcessor}/{@code InvoiceImportProcessor} após persistir uma
 * transação importada. Reagido por {@code TransactionOverlayListener} (f006), que grava os vínculos
 * {@code F006_TRANSACTION_CATEGORY} e {@code F006_TRANSACTION_TAG} (de {@code categoryId}/
 * {@code tagIds}) — mantém o 1:1/N:N com {@code F006_TRANSACTION}. Publicador e consumidor moram os
 * dois em f006 desde a fase 6 (a antiga fatia {@code f007} fundiu-se aqui) — evento mantido por
 * simplicidade (import já a criava antes da fusão), não por fronteira de fatia. Mora em {@code f000}
 * (não em quem publica) por convenção de vocabulário compartilhado.
 */
@NullMarked
public record TransactionImported(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId, List<UUID> tagIds) implements BusinessEvent {}
