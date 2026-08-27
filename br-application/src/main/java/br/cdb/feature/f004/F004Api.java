package br.cdb.feature.f004;

import br.cdb.core.View;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f004} */
@NullMarked
public interface F004Api {

    /** Também é o retorno JSON de {@code TagResource} — mesmo tipo dos dois lados, em vez do modelo
     *  de domínio {@code Tag} vazar direto na borda HTTP. */
    @NullMarked
    @Schema(name = "Tag")
    record TagView(UUID id, UUID personId, String name, String color, @Nullable LocalDateTime createdAt) implements View {
        public static TagView from(Tag tag) {
            return new TagView(tag.id(), tag.personId(), tag.name(), tag.color(), tag.createdAt());
        }
    }

    /** Corpo de {@link #createTag}/{@link #updateTag} — espelha {@code TagRequest}. */
    @NullMarked
    record TagBody(String name, String color) {}

    /** Tags da pessoa do token. */
    List<TagView> tags();

    TagView createTag(TagBody body);

    TagView updateTag(UUID id, TagBody body);

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@link DeletionStrategy#MOVE} exige {@code targetId}. */
    void deleteTag(UUID id, @Nullable String strategy, @Nullable UUID targetId);

    /**
     * Guarda de propriedade de tag (anti-IDOR), no molde de {@code f000.UserGuards}: 404
     * {@link BusinessError.NotFound} na primeira tag que não seja da pessoa do token — vinculá-la a
     * uma transação revelaria a existência de tag alheia. Coleção vazia passa sem chamada HTTP.
     */
    Result<Void, BusinessError> ownsTags(Collection<UUID> tagIds);

}
