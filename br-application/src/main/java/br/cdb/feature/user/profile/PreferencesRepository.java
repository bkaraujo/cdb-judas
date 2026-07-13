package br.cdb.feature.user.profile;

import org.jspecify.annotations.NullMarked;

/**
 * Porta de persistência das preferências do usuário (fatia {@code profile}). Implementada por
 * um adaptador JDBC na infraestrutura, espelhando o padrão {@code UserCategoryRepository}.
 */
@NullMarked
public interface PreferencesRepository {

    /** Preferências do usuário; sem registros, retorna {@link Preferences#defaults()}. */
    Preferences findByUserId(String userId);

    /** Persiste (upsert) as preferências do usuário e as devolve. */
    Preferences save(String userId, Preferences prefs);

}
