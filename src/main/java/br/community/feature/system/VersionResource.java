package br.community.feature.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@Path("/api/version")
@Tag(name = "System", description = "Recursos de sistema")
public class VersionResource {

    @Inject
    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Operation(summary = "Retorna a versão do sistema extraída do pom.xml")
    public Map<String, String> getVersion() {
        return Map.of("version", version);
    }
}
