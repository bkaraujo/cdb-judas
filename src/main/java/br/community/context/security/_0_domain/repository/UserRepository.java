package br.community.context.security._0_domain.repository;

import br.community.context.security._0_domain.model.User;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    User save(User user);

}
