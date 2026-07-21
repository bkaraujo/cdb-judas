package br.cdb.feature;

import br.cdb.infra.persistence.features.*;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Repositórios de feature são produzidos aqui (não em {@code br.cdb.core.ContextBridge}) porque suas
 * portas vivem em pacotes de feature (ex.: {@code br.cdb.feature.user.tags.UserTagRepository}) — o
 * núcleo comum não pode depender de fatias de feature (ArchUnit {@code core_must_not_access_feature}).
 * Cada produtor recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema
 * antes de qualquer adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class FeatureModule {

    @Produces
    @Singleton
    public UserAccountJDBCRepository userAccountRepository(DataSource dataSource) {
        return Registry.tryGet(UserAccountJDBCRepository.class, UserAccountJDBCRepository::new);
    }
}
