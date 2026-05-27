package br.community.context.security._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public interface UserRepository {

    Optional<User> findByUsername(String username);

    User save(User user);

}
