package br.cdb.feature.f002._1_application;

import br.cdb.feature.f000._0_domain.AccountOwnership;
import br.cdb.feature.f002._0_domain.UserAccount;
import br.cdb.feature.f002._0_domain.UserAccountRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de acesso ao overlay de conta por utilizador ({@code PERSON_ACCOUNT}). Implementa a porta
 * {@link AccountOwnership} da fatia-base ({@code f000}) — é o dono do overlay, então responde à
 * checagem de propriedade anti-IDOR sem a base depender de f002.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserAccountService implements AccountOwnership {

    private final UserAccountRepository repository;

    @Nullable
    public UserAccount find(String personId, UUID accountId) {
        return repository.find(personId, accountId).orElse(null);
    }

    public List<UserAccount> findByPerson(String personId) {
        return repository.findByPerson(personId);
    }

    @Override
    public Set<UUID> ownedAccountIds(String personId) {
        return repository.findByPerson(personId).stream()
                .map(UserAccount::accountId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void save(UserAccount ua) {
        repository.save(ua);
    }

    public void delete(String personId, UUID accountId) {
        repository.delete(personId, accountId);
    }
}
