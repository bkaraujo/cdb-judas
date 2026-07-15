package br.cdb.feature;

import br.cdb.feature.user.accounts.closing.ClosingRepository;
import br.cdb.feature.user.accounts.closing.ClosingService;
import br.cdb.feature.user.accounts.transactions.UserTransactionRepository;
import br.cdb.feature.user.categories.UserCategoryRepository;
import br.cdb.feature.user.profile.PreferencesRepository;
import br.cdb.feature.user.stream.SSE;
import br.cdb.feature.user.stream.SseService;
import br.cdb.feature.user.tags.UserTagRepository;
import br.cdb.feature.user.tags.UserTransactionTagRepository;
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

    @Produces
    @Singleton
    public UserTagRepository userTagRepository(DataSource dataSource) {
        return Registry.tryGet(UserTagRepository.class, UserTagJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserTransactionTagRepository userTransactionTagRepository(DataSource dataSource) {
        return Registry.tryGet(UserTransactionTagRepository.class, UserTransactionTagJDBCRepository::new);
    }

    @Produces
    @Singleton
    public PreferencesRepository preferencesRepository(DataSource dataSource) {
        return Registry.tryGet(PreferencesRepository.class, PreferencesJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserCategoryRepository userCategoryRepository(DataSource dataSource) {
        return Registry.tryGet(UserCategoryRepository.class, UserCategoryJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserTransactionRepository userTransactionRepository(DataSource dataSource) {
        return Registry.tryGet(UserTransactionRepository.class, UserTransactionJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserAccountJDBCRepository userAccountRepository(DataSource dataSource) {
        return Registry.tryGet(UserAccountJDBCRepository.class, UserAccountJDBCRepository::new);
    }
}
