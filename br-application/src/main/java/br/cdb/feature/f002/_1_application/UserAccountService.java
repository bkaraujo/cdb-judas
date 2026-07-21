package br.cdb.feature.f002._1_application;

import br.cdb.feature.f002._0_domain.UserAccount;
import br.cdb.feature.f002._0_domain.UserAccountRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Serviço de acesso ao overlay de conta por utilizador ({@code PERSON_ACCOUNT}). */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;

    @Nullable
    public UserAccount find(String personId, UUID accountId) {
        return repository.find(personId, accountId).orElse(null);
    }

    public List<UserAccount> findByPerson(String personId) {
        return repository.findByPerson(personId);
    }

    public void save(UserAccount ua) {
        repository.save(ua);
    }

    public void delete(String personId, UUID accountId) {
        repository.delete(personId, accountId);
    }
}
