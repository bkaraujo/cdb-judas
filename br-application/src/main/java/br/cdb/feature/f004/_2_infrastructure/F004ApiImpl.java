package br.cdb.feature.f004._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f004.F004Api;
import br.cdb.feature.f004._2_infrastructure.web.TagResource;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
 * <p>Context-wired ({@code Context.get(F004Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F004ApiImpl extends AbstractApiClient implements F004Api {

    @Override
    public List<TagView> tags() {
        return list("/tags", TagView[].class);
    }

    @Override
    public TagView createTag(TagBody body) {
        return post("/tags", body, TagView.class);
    }

    @Override
    public TagView updateTag(UUID id, TagBody body) {
        return patch("/tags/" + id, body, TagView.class);
    }

    @Override
    public void deleteTag(UUID id, @Nullable String strategy, @Nullable UUID targetId) {
        delete("/tags/" + id + deletionQuery(strategy, targetId));
    }

    @Override
    public Result<Void, BusinessError> ownsTags(Collection<UUID> tagIds) {
        if (tagIds.isEmpty()) return Result.success();

        val owned = tags().stream().map(TagView::id).collect(Collectors.toUnmodifiableSet());
        for (val tagId : tagIds) {
            if (!owned.contains(tagId)) {
                return Result.failure(new BusinessError.NotFound("Tag not found: %s", tagId));
            }
        }
        return Result.success();
    }

    private static String deletionQuery(@Nullable String strategy, @Nullable UUID targetId) {
        if (strategy == null) return "";
        return targetId == null ? "?strategy=" + strategy : "?strategy=" + strategy + "&targetId=" + targetId;
    }
}
