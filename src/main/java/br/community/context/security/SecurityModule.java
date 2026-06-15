package br.community.context.security;

import br.community.context.security._0_domain.repository.UserRepository;
import br.community.context.security._1_application.usecase.UserUseCase;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
public class SecurityModule {

    @Bean
    UserUseCase userUseCase(UserRepository userRepository) {
        return new UserUseCase(userRepository);
    }

    @Bean
    public SecurityContext securityContext(UserUseCase userUseCase) {
        return new SecurityContext(userUseCase);
    }

}
