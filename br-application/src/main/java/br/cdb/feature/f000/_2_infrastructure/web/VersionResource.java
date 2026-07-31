package br.cdb.feature.f000._2_infrastructure.web;

import br.cdb.core.CoreModule;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@Path("/api/version")
@Tag(name = "System", description = "Recursos de sistema")
public class VersionResource {

    @GET
    @Operation(summary = "Retorna a versão do sistema extraída do pom.xml")
    public Map<String, String> getVersion() {
        return Map.of("version", CoreModule.yaml.asString("cdb.application.version", "0.0.0"));
    }
}
