package br.cdb.feature.f004;

import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._2_infrastructure.web.TagResource;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cliente tipado da própria API pública de {@code f004}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, no mesmo papel de {@code f002.F002Api}/{@code f005.F005Api}/
 * {@code f006.F006Api}.
 *
 * <p>HTTP real via {@link HTTPApi} contra as rotas públicas de {@link TagResource}, com o mesmo
 * {@code AuthenticationFilter}/{@code OwnershipFilter} e token efêmero — a listagem já vem escopada
 * pela pessoa do token, que é o que torna {@link #ownsTags} uma guarda de propriedade confiável. O
 * consumidor não alcança serviço, repositório nem o modelo {@code Tag}.
 *
 * <p>Espelha toda a interface pública da fatia (D2 de {@code .claude/plan.md}); {@link #ownsTags}
 * continua a conveniência sobre {@link #tags()} usada cross-slice.
 *
 * <p>Context-wired ({@code Context.tryGet(F004Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F004Api {

    /** Espelha o modelo {@code Tag}. */
    @NullMarked
    public record TagView(UUID id, UUID personId, String name, String color, @Nullable LocalDateTime createdAt) {}

    /** Corpo de {@link #createTag}/{@link #updateTag} — espelha {@code TagRequest}. */
    @NullMarked
    public record TagBody(String name, String color) {}

    private static HTTPApi internalApi() {
        return Context.get(HTTPApi.class);
    }

    /** Tags da pessoa do token. */
    public List<TagView> tags() {
        return List.of(internalApi().get("/tags", TagView[].class));
    }

    public TagView createTag(TagBody body) {
        return internalApi().post("/tags", body, TagView.class);
    }

    public TagView updateTag(UUID id, TagBody body) {
        return internalApi().patch("/tags/" + id, body, TagView.class);
    }

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@link DeletionStrategy#MOVE} exige {@code targetId}. */
    public void deleteTag(UUID id, @Nullable String strategy, @Nullable UUID targetId) {
        internalApi().delete("/tags/" + id + deletionQuery(strategy, targetId));
    }

    /**
     * Guarda de propriedade de tag (anti-IDOR), no molde de {@code f000.UserGuards}: 404
     * {@link BusinessError.NotFound} na primeira tag que não seja da pessoa do token — vinculá-la a
     * uma transação revelaria a existência de tag alheia. Coleção vazia passa sem chamada HTTP.
     */
    public Result<Void, BusinessError> ownsTags(Collection<UUID> tagIds) {
        if (tagIds.isEmpty()) return Result.success();

        val owned = tags().stream().map(TagView::id).collect(Collectors.toUnmodifiableSet());
        for (val tagId : tagIds) {
            if (!owned.contains(tagId)) {
                return Result.failure(new BusinessError.NotFound("Tag not found: " + tagId));
            }
        }
        return Result.success();
    }

    private static String deletionQuery(@Nullable String strategy, @Nullable UUID targetId) {
        if (strategy == null) return "";
        return targetId == null ? "?strategy=" + strategy : "?strategy=" + strategy + "&targetId=" + targetId;
    }
}
