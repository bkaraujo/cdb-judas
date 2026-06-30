package br.community.core;

import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do {@code DataSource} JDBC, ligada via {@link org.springframework.boot.context.properties.ConfigurationPropertiesScan}.
 * Default é H2 in-memory (seguro: nenhum teste escreve em disco); {@code application.properties} sobe para
 * {@code jdbc:h2:file} em dev e {@code application-test.properties} mantém {@code mem} no perfil de teste.
 */
@NullMarked
@ConfigurationProperties(prefix = "datasource.jdbc")
public record DataSourceProperties(String url, String driver, String username, String password) {

    public DataSourceProperties {
        if (url == null || url.isBlank()) url = "jdbc:h2:mem:cdb;DB_CLOSE_DELAY=-1";
        if (driver == null || driver.isBlank()) driver = "org.h2.Driver";
        if (username == null || username.isBlank()) username = "sa";
        if (password == null) password = Strings.EMPTY;
    }
}
