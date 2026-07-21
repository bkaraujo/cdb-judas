package br.cdb.feature.f002._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserAccountRepository {
    Optional<UserAccount> find(String personId, UUID accountId);

    List<UserAccount> findByPerson(String personId);

    UserAccount save(UserAccount ua);

    void delete(String personId, UUID accountId);
}
