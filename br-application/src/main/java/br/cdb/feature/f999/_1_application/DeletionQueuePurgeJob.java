package br.cdb.feature.f999._1_application;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Amarra o timer de produção a {@link DeletionQueueService#runOnce}, o método testado direto (ver
 * {@code DeletionQueueServiceTest}). Classe própria (não {@code F999Module}) de propósito: um
 * produtor CDI de {@code DeletionQueueRepository} vive em {@code F999Module}, e injetar
 * {@link DeletionQueueService} (que depende dessa porta) de volta nele criaria um ciclo — o próprio
 * bean precisando do produtor que ele mesmo hospeda.
 */
@NullMarked
@ApplicationScoped
@RequiredArgsConstructor
public class DeletionQueuePurgeJob {

    private final DeletionQueueService deletionQueueService;

    @Scheduled(every = "5m")
    void purge() {
        deletionQueueService.runOnce();
    }
}
