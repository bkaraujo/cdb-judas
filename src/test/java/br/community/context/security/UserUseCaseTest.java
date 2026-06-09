package br.community.context.security;

import br.commons.Result;
import br.community.context.security._0_domain.UserRepository;
import br.community.context.security._0_domain.model.Preferences;
import br.community.context.security._0_domain.model.User;
import br.community.context.security._1_application.usecase.UserUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserUseCaseTest {

    /** Repositório em memória — prior art: InMemoryRepositories do contexto monetary. */
    static final class InMemoryUsers implements UserRepository {
        private final Map<String, User> byId = new LinkedHashMap<>();

        @Override public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }
        @Override public Optional<User> findById(String id) { return Optional.ofNullable(byId.get(id)); }
        @Override public User save(User user) { byId.put(user.id(), user); return user; }
    }

    private InMemoryUsers repo;
    private UserUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUsers();
        useCase = new UserUseCase(repo);
    }

    @Test
    void getMeSobreRegistroLegadoRetornaNomeNuloEPreferenciasPadrao() {
        String id = UUID.randomUUID().toString();
        // Registro legado: sem name e sem preferences (null → normalizado p/ padrão).
        repo.save(new User(id, "admin", null, "hash", null));

        Result<User, DomainError> result = useCase.getMe(id);

        assertTrue(result.isSuccess());
        User user = ((Result.Success<User, DomainError>) result).value();
        assertNotNull(user);
        assertNull(user.name(), "name ausente permanece nulo");
        assertEquals(Preferences.defaults(), user.preferences());
        assertNull(user.preferences().theme());
        assertEquals("pt-BR", user.preferences().language());
        assertEquals("pt-BR", user.preferences().locale());
        assertFalse(user.preferences().sidebarCollapsed());
    }

    @Test
    void getMeUsuarioInexistenteRetornaNotFound() {
        Result<User, DomainError> result = useCase.getMe("inexistente");

        assertTrue(result.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<User, DomainError>) result).error());
    }
}
