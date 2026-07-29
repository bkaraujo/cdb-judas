package br.cdb.feature.f002._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Porta que a fatia de contas ({@code f002}) declara para gravar uma linha durável na fila
 * {@code F999_DELETION_QUEUE} logo após publicar seu evento de exclusão — rede de segurança pro job
 * de reconciliação (f999) reprocessar se o listener best-effort falhar ou o processo cair antes da
 * cascata terminar (ver javadoc de {@code Database}). Implementada por um adapter em
 * {@code f999._2_infrastructure.adapter}, único lugar que conhece os dois lados: f002 não pode
 * importar f999 direto (f999 só é permitida como <em>origem</em> cross-slice, nunca alvo).
 */
@NullMarked
public interface DeletionQueue {

    void enqueueAccountDeleted(UUID accountId, UUID personId);

    void enqueueTransactionDeleted(UUID transactionId, UUID personId);
}
