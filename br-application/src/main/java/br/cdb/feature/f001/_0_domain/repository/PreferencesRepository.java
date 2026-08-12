package br.cdb.feature.f001._0_domain.repository;

import br.cdb.feature.f001._0_domain.model.Preferences;
import org.jspecify.annotations.NullMarked;

/**
 * Porta de persistência das preferências do usuário (fatia {@code profile}). Implementada por
 * um adaptador JDBC na infraestrutura, espelhando o padrão {@code CategoryRepository}.
 */
@NullMarked
public interface PreferencesRepository {

    /** Preferências do usuário; sem registros, retorna {@link Preferences#defaults()}. */
    Preferences findByPersonId(String personId);

    /** Persiste (upsert) as preferências do usuário e as devolve. */
    Preferences save(String personId, Preferences prefs);

}
