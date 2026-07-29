package br.cdb.feature.f999._0_domain;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linha de {@code F999_DELETION_QUEUE}: um evento cross-slice que a fatia dona já publicou (via
 * {@code MessageBus}) e que precisa de retry se o listener falhar ou o processo cair antes do
 * best-effort completar — ver {@code Database} (javadoc de classe) e {@code DeletionQueueService}.
 * {@code type} identifica qual evento reconstruir no retry ({@code DeletionQueueService#republish});
 * {@code targetId} é o id singular do alvo (conta, transação — nunca uma lista: um lote vira uma
 * linha por item).
 */
@NullMarked
public record DeletionQueueEntry(
        UUID id,
        String type,
        UUID targetId,
        UUID personId,
        int attempts,
        boolean locked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
