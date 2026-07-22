package br.cdb.feature.f000;

import br.cdb.feature.f000._0_domain.ClosingRepository;
import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._1_application.ClosingService;
import br.cdb.feature.f000._2_infrastructure.persistence.ClosingJDBCRepository;
import br.cdb.feature.f000._2_infrastructure.service.SseService;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia-base {@code f000}. {@link #closingRepository} recebe {@link DataSource} sem
 * usá-lo no corpo só para forçar o CDI a criar o schema antes do adaptador JDBC — a dependência real
 * fica escondida dentro do {@link Registry} (mesmo padrão de {@code br.cdb.core.ContextBridge}).
 */
@NullMarked
@Singleton
public class F000Module {

    @Produces
    @Singleton
    public SSE sse() {
        return new SseService();
    }

    @Produces
    @Singleton
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }

    @Produces
    @Singleton
    public ClosingRepository closingRepository(DataSource dataSource) {
        return Registry.tryGet(ClosingRepository.class, ClosingJDBCRepository::new);
    }
}
