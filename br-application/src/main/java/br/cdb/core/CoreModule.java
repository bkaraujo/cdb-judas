package br.cdb.core;

import br.cdb.core.persistence.repository.CachingUserRepository;
import br.cdb.core.persistence.repository.UserJDBCRepository;
import br.cdb.core.persistence.repository.UserRepository;
import br.commons.Logger;
import br.commons.Yaml;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import br.commons.tools.Strings;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@ApplicationScoped
public class CoreModule {

    public static final Yaml yaml = Yaml.from(Path.of(System.getProperty("cdb.application.yaml", "application.yaml")));

    void initDataSource(@Observes @Priority(0) StartupEvent event) {
        val subtree = yaml.subtree("cdb.datasource");
        if (subtree == null) {
            Logger.fatal("Missing [cdb.datasource] in application.yaml");
            throw new RuntimeException("unreachable");
        }

        val properties = new JDBCProperties();
        properties.driver(subtree.asString("jdbc.driver", "org.h2.Driver"));
        properties.url(subtree.asString("jdbc.url", "jdbc:h2:mem:cdb;DB_CLOSE_DELAY=-1"));
        properties.username(subtree.asString("jdbc.username", "sa"));
        properties.password(subtree.asString("jdbc.password", Strings.EMPTY));
        properties.validationQuery(subtree.asString("validation-query","SELECT 1"));
        properties.minPoolSize(subtree.asInt("pool.min", 5));
        properties.maxPoolSize(subtree.asInt("pool.max", 20));

        Context.set(DataSource.class, () -> new DataSource(properties));
        Context.set(UserRepository.class, () -> new CachingUserRepository(new UserJDBCRepository()));
    }

}
