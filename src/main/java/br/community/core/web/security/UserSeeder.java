package br.community.core.web.security;

import br.commons.tools.Strings;
import br.community.context.security._0_domain.User;
import br.community.context.security._0_domain.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@NullMarked
@RequiredArgsConstructor
public final class UserSeeder {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seed() {
        if (repository.findByUsername("admin").isEmpty()) {
            repository.save(new User(
                    "admin",
                    Strings.orEmpty(passwordEncoder.encode("admin"))
            ));
        }
    }
}
