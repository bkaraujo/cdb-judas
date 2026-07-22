package br.cdb.feature.f001._2_infrastructure.web.response;

import br.cdb.feature.f001._0_domain.Preferences;
import br.cdb.feature.f001._2_infrastructure.web.SelfView;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Projeção de leitura/escrita do recurso {@code self} (/api/me). Não expõe a senha do
 * agregado. {@code preferences} reutiliza o record de domínio (formato 1:1 com o contrato).
 */
@NullMarked
public record SelfResponse(
        String id,
        String username,
        @Nullable String name,
        Preferences preferences
) {
    public static SelfResponse from(SelfView self) {
        val person = self.profile().person();
        return new SelfResponse(person.id().toString(), self.username(), person.name(), self.profile().preferences());
    }
}
