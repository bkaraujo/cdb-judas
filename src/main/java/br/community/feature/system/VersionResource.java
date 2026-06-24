package br.community.feature.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@NullMarked
@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
@Tag(name = "System", description = "Recursos de sistema")
public class VersionResource {

    private final Optional<BuildProperties> buildProperties;

    @org.springframework.beans.factory.annotation.Value("${info.app.version:dev}")
    private String fallbackVersion;

    @GetMapping
    @Operation(summary = "Retorna a versão do sistema extraída do pom.xml")
    public Map<String, String> getVersion() {
        val version = buildProperties
                .map(BuildProperties::getVersion)
                .orElse(fallbackVersion);
        return Map.of("version", version);
    }
}
