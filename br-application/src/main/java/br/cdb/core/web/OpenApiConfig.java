package br.cdb.core.web;

import br.cdb.feature.auth.LoginResource;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.jspecify.annotations.NullMarked;

/** {@code info.version} vem de {@code quarkus.smallrye-openapi.info-version} (application.properties). */
@NullMarked
@OpenAPIDefinition(
        info = @Info(
                title = "CDB Judas API",
                description = "API de controle financeiro pessoal. Rotas de dados do usuário vivem sob "
                        + "o namespace /api/{uuid}/... (o {uuid} é o id do usuário autenticado, validado "
                        + "pela guarda de propriedade). Apenas rotas globais (ex.: /api/cost-center) e o "
                        + "login (/login) ficam fora do namespace.",
                version = "unused"
        ),
        security = @SecurityRequirement(name = "AccessToken")
)
@SecurityScheme(
        securitySchemeName = "AccessToken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        apiKeyName = LoginResource.TOKEN_HEADER
)
public class OpenApiConfig extends Application {
}
