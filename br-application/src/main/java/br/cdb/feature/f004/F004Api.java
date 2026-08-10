package br.cdb.feature.f004;

import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f004._2_infrastructure.web.TagResource;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cliente tipado da própria API pública de {@code f004}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, no mesmo papel de {@code f002.F002Api}/{@code f005.F005Api}/
 * {@code f006.F006Api}.
 *
 * <p>HTTP real via {@link InternalApi} contra a rota pública ({@link TagResource#listAll}), com o
 * mesmo {@code AuthenticationFilter}/{@code OwnershipFilter} e token efêmero — a listagem já vem
 * escopada pela pessoa do token, que é o que torna {@link #ownsTags} uma guarda de propriedade
 * confiável. O consumidor não alcança serviço, repositório nem o modelo {@code Tag}.
 *
 * <p>Context-wired ({@code Context.tryGet(F004Api.class)}), sem estado próprio: {@link InternalApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F004Api {

    /** Corpo mínimo de {@code GET /tags} — só o {@code id} interessa à guarda. */
    @NullMarked
    private record TagDto(UUID id) {}

    private static InternalApi internalApi() {
        return Context.get(InternalApi.class);
    }

    /**
     * Guarda de propriedade de tag (anti-IDOR), no molde de {@code f000.UserGuards}: 404
     * {@link BusinessError.NotFound} na primeira tag que não seja da pessoa do token — vinculá-la a
     * uma transação revelaria a existência de tag alheia. Coleção vazia passa sem chamada HTTP.
     */
    public Result<Void, BusinessError> ownsTags(Collection<UUID> tagIds) {
        if (tagIds.isEmpty()) return Result.success();

        val owned = tagIds();
        for (val tagId : tagIds) {
            if (!owned.contains(tagId)) {
                return Result.failure(new BusinessError.NotFound("Tag not found: " + tagId));
            }
        }
        return Result.success();
    }

    /** Ids das tags da pessoa do token. */
    private static Set<UUID> tagIds() {
        return Arrays.stream(internalApi().get("/tags", TagDto[].class))
                .map(TagDto::id)
                .collect(Collectors.toUnmodifiableSet());
    }
}
