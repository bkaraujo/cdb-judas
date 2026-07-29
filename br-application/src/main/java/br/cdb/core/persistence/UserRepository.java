package br.cdb.core.persistence;

import br.cdb.core.security.User;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    /** Resolve o login pela pessoa vinculada ({@code SEC_USER.COD_PERSON}). A camada web expõe o
     *  personId como identidade; a fatia {@code self} (/api/me) reconstrói o {@link User} a partir dele. */
    Optional<User> findByPersonId(String personId);

    User save(User user);

}
