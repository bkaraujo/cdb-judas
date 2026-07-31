package br.cdb.feature.f004;

import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TagJDBCRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TransactionTagJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo da fatia {@code f004} (tags). Os adaptadores JDBC são construídos aqui, no
 * {@link #initialize()} — depois do {@code DataSource} publicado por {@code CoreModule} (o
 * construtor de {@code JDBCRepository} introspecta a tabela, então exige schema já criado).
 */
@NullMarked
public class F004Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(TagRepository.class, TagJDBCRepository::new);
        Context.set(TransactionTagRepository.class, TransactionTagJDBCRepository::new);

        return Result.success();
    }
}
