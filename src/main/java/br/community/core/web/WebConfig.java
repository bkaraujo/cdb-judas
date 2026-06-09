package br.community.core.web;

import br.community.core.web.security.OwnershipInterceptor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@NullMarked
@Configuration
class WebConfig implements WebMvcConfigurer {

    private final OwnershipInterceptor ownershipInterceptor;

    WebConfig(OwnershipInterceptor ownershipInterceptor) {
        this.ownershipInterceptor = ownershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ownershipInterceptor)
                .addPathPatterns("/api/**")
                // Apenas rotas globais ficam fora do namespace de usuário.
                .excludePathPatterns(
                        // Centro de custo é global (fixo, somente leitura).
                        "/api/cost-center", "/api/cost-center/**",
                        // Versão do sistema é pública.
                        "/api/version",
                        // Recurso self: identidade vem do contexto autenticado, sem uuid na rota.
                        "/api/me"
                );
    }
}
