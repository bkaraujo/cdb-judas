package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Publicado por {@code StatementImportProcessor}/{@code InvoiceImportProcessor} (fatia {@code f007})
 * após persistir uma transação importada, via a porta {@code f007.TransactionWriter}/
 * {@code f999.TransactionWriterAdapter}. Reagido pelo listener de overlay de {@code f006}
 * ({@code F006Module.initialize()}), que grava os vínculos {@code F006_TRANSACTION_CATEGORY} e
 * {@code F006_TRANSACTION_TAG} (de {@code categoryId}/{@code tagIds}) — mantém o 1:1/N:N com
 * {@code F006_TRANSACTION}. É o mecanismo cross-slice sancionado entre publicador ({@code f007}) e
 * consumidor ({@code f006}) desde a extração de {@code .claude/plan.md}. Mora em {@code f000} (não em
 * quem publica) por convenção de vocabulário compartilhado.
 */
@NullMarked
public record TransactionImported(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId, List<UUID> tagIds) implements BusinessEvent {}
