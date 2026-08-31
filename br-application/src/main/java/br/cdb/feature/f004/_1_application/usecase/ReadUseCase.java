package br.cdb.feature.f004._1_application.usecase;

import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._1_application.cache.TagCache;
import br.cdb.feature.f004._1_application.cache.TagLayout;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Toda a leitura de tag da fatia {@code f004} — o par de {@link WriteUseCase}, mesmo arranjo CQRS de
 * {@code f002}/{@code f003}/{@code f006}. Context-wired ({@code Context.tryGet(ReadUseCase.class)},
 * nunca {@code @Inject}); o {@code TagResource} lê <b>só</b> daqui.
 *
 * <p>Tag não tem guarda de propriedade explícita ({@code UserGuards}): a fatia é escopada por
 * pessoa na própria query ({@code F004_TAG.COD_PERSON} — {@code findAllByPerson}/
 * {@code findByPersonAndId}), e o {@code personId} vem do {@code /api/{uuid}/…} já validado pelo
 * {@code OwnershipFilter}.
 *
 * <p>Nota: o nome simples coincide com o {@code ReadUseCase} de {@code f002}/{@code f003} — quem
 * precisa de mais de um referencia os demais pelo nome completo.
 *
 * <p>Lê do cache off-heap; nunca do banco (ver .claude/plan.md, D1).
 */
@NullMarked
public class ReadUseCase {

    private final TagCache cache = Context.tryGet(TagCache.class);

    public List<Tag> tags(UUID personId) {
        val result = new ArrayList<Tag>();
        cache.forEach(personId, v -> {
            result.add(new Tag(v.id(), v.personId(), v.name(), v.color(), v.createdAt()));
        });
        return result;
    }

    /** Guarda implícita: {@code NotFound} quando a tag existe mas é de outra pessoa. */
    public Result<Tag, BusinessError> tag(UUID personId, UUID tagId) {
        val found = new boolean[1];
        val result = new Tag[1];
        cache.find(personId, tagId, v -> {
            result[0] = new Tag(v.id(), v.personId(), v.name(), v.color(), v.createdAt());
            found[0] = true;
        });
        if (found[0]) return Result.success(result[0]);
        return Result.failure(new BusinessError.NotFound("error.tag.not-found", tagId));
    }
}
