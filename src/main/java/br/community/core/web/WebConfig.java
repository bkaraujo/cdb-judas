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
                // Rotas legadas (ainda fora do namespace) e globais — isentas durante a transição.
                // Cada exclusão é removida quando o recurso correspondente migra para /api/{uuid}/...
                .excludePathPatterns(
                        "/api/categories", "/api/categories/**",
                        "/api/dashboard", "/api/dashboard/**",
                        "/api/operations/**",
                        "/api/cost-centers", "/api/cost-centers/**",
                        "/api/tags", "/api/tags/**",
                        "/api/v1/**"
                );
    }
}
