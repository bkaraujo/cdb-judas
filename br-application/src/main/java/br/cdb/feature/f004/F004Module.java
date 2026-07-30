package br.cdb.feature.f004;

import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TagJDBCRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TransactionTagJDBCRepository;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f004} (tags). Produtores recebem {@link DataSource} sem usá-lo no corpo
 * só para forçar o CDI a criar o schema antes do adaptador JDBC — a dependência real fica escondida
 * dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F004Module {

    @Produces
    @Singleton
    public TagRepository userTagRepository() {
        return Registry.tryGet(TagRepository.class, TagJDBCRepository::new);
    }

    @Produces
    @Singleton
    public TransactionTagRepository userTransactionTagRepository() {
        return Registry.tryGet(TransactionTagRepository.class, TransactionTagJDBCRepository::new);
    }

    void onStart(@Observes @Priority(4) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
