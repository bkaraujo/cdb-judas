package br.community.core.web;

import br.community.core.web.security.LoginResource;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
class OpenApiConfig {

    private static final String TOKEN_SCHEME = "AccessToken";

    @Bean
    public OpenAPI cdbJudasOpenAPI(ObjectProvider<BuildProperties> buildProperties) {
        val build = buildProperties.getIfAvailable();
        val version = build != null ? build.getVersion() : "dev";

        return new OpenAPI()
                .info(new Info()
                        .title("CDB Judas API")
                        .description("API de controle financeiro pessoal. Rotas de dados do usuário vivem sob "
                                + "o namespace /api/{uuid}/... (o {uuid} é o id do usuário autenticado, validado "
                                + "pela guarda de propriedade). Apenas rotas globais (ex.: /api/cost-center) e o "
                                + "login (/login) ficam fora do namespace.")
                        .version(version))
                .components(new Components()
                        .addSecuritySchemes(TOKEN_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(LoginResource.TOKEN_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(TOKEN_SCHEME));
    }
}
