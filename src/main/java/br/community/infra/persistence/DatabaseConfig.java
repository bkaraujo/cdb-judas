package br.community.infra.persistence;

import br.commons.Logger;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Borda Spring que monta o {@code DataSource} H2 (in-memory), cria o schema
 * ({@link Database#model()}) e publica o {@code DataSource} no {@link Registry} para os
 * adaptadores JDBC do contexto.
 *
 * <p>Roda como bean Spring (e não no {@code main()}) para que o banco esteja disponível tanto
 * no app quanto nos testes {@code @SpringBootTest}. {@code autoCommit=false}: cada {@code execute()}
 * commita explicitamente; o schema é commitado em bloco ao final da inicialização.</p>
 */
@NullMarked
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        val properties = new JDBCProperties();
        properties.driver("org.h2.Driver");
        properties.url("jdbc:h2:mem:cdb;DB_CLOSE_DELAY=-1");
        properties.username("sa");
        properties.password(Strings.EMPTY);
        properties.validationQuery("SELECT 1");
        properties.minPoolSize(5);
        properties.maxPoolSize(20);

        return Registry.tryGet(DataSource.class, () -> {
            val datasource = new DataSource(properties);
            datasource.getConnection().ifSuccess(conn -> {
                val stmt = conn.createStatement().getOrThrow();
                for (val command : Database.model()) {
                    stmt.execute(command).ifFailure(reason -> Logger.error("Erro ao criar schema: %s", reason));
                }
                stmt.close();
                conn.commit();
                conn.close();
            });

            return datasource;
        });
    }
}
