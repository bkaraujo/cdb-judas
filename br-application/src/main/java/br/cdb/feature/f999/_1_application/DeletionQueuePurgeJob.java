package br.cdb.feature.f999._1_application;

import br.commons.framework.cdi.Context;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jspecify.annotations.NullMarked;

/**
 * Amarra o timer de produção a {@link DeletionQueueService#runOnce}, o método testado direto (ver
 * {@code DeletionQueueServiceTest}). Classe própria (não {@code F999Module}) porque o módulo não é
 * mais um bean CDI: aqui mora o único {@code @Scheduled} da fatia, e o serviço vem do {@link Context}
 * a cada disparo — resolvido tarde, nunca no boot (o adaptador JDBC precisa do schema já criado).
 */
@NullMarked
@ApplicationScoped
public class DeletionQueuePurgeJob {

    @Scheduled(every = "5m", delayed = "5m")
    void purge() {
        Context.get(DeletionQueueService.class).runOnce();
    }
}
