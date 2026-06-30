package br.community.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.jspecify.annotations.NullMarked;

/**
 * Configuração do {@code DataSource} JDBC (pool próprio), ligada via SmallRye Config.
 * Default é H2 in-memory (seguro: nenhum teste escreve em disco); {@code application.properties}
 * sobe para {@code jdbc:h2:file} em dev e o perfil {@code %test} mantém {@code mem}.
 */
@NullMarked
@ConfigMapping(prefix = "datasource.jdbc")
public interface DataSourceProperties {

    @WithDefault("jdbc:h2:mem:cdb;DB_CLOSE_DELAY=-1")
    String url();

    @WithDefault("org.h2.Driver")
    String driver();

    @WithDefault("sa")
    String username();

    @WithDefault("")
    String password();
}
