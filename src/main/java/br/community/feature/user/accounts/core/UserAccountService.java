package br.community.feature.user.accounts.core;

import br.community.infra.persistence.features.UserAccountJDBCRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Serviço de acesso ao overlay de conta por utilizador ({@code USER_ACCOUNT}). */
@NullMarked
@Component
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountJDBCRepository repository;

    @Nullable
    public UserAccount find(String userId, UUID accountId) {
        return repository.find(userId, accountId).orElse(null);
    }

    public List<UserAccount> findByUser(String userId) {
        return repository.findByUser(userId);
    }

    public void save(UserAccount ua) {
        repository.save(ua);
    }

    public void delete(String userId, UUID accountId) {
        repository.delete(userId, accountId);
    }
}
