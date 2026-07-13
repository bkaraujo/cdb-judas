package br.cdb.core.web.security;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    User save(User user);

}
