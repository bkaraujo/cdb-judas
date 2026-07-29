package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Publicado por {@code StatementImportProcessor}/{@code InvoiceImportProcessor} (f007) após
 * persistir uma transação importada. Reagido por {@code TransactionOverlayListener} (f006), que
 * grava o vínculo {@code F005_TRANSACTION_CATEGORY} — mantém o 1:1 com {@code F006_TRANSACTION} sem
 * f007 depender de f006 diretamente. Mora em {@code f000} (não em quem publica) para respeitar a
 * ordem de fatias.
 */
@NullMarked
public record TransactionImported(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId) implements BusinessEvent {}
