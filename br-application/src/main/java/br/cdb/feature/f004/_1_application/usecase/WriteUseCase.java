package br.cdb.feature.f004._1_application.usecase;

import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._1_application.service.TagService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Toda a mutação de tag da fatia {@code f004} — o par de {@link ReadUseCase}, mesmo arranjo CQRS de
 * {@code f002}/{@code f003}/{@code f006}. Context-wired ({@code Context.tryGet(WriteUseCase.class)},
 * nunca {@code @Inject}); o {@code TagResource} escreve <b>só</b> por aqui.
 *
 * <p>A exclusão publica {@link TagEvents.Deleted} com a estratégia já resolvida ({@code DETACH} é o
 * padrão quando o chamador não escolhe) — quem reage é {@code f006} (re-key/limpeza do vínculo
 * transação↔tag) e {@code f999} (dispatch SSE), sempre best-effort. A tag é apagada antes do evento:
 * sem FK entre tabelas de dados, o vínculo órfão é limpo pelo listener.
 *
 * <p>Nota: o nome simples coincide com o {@code WriteUseCase} de {@code f002}/{@code f003} — quem
 * precisa de mais de um referencia os demais pelo nome completo.
 */
@NullMarked
public class WriteUseCase {

    private final TagService service = Context.tryGet(TagService.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    public Result<Tag, BusinessError> createTag(UUID personId, String name, String color) {
        return service.create(personId, name, color);
    }

    public Result<Tag, BusinessError> updateTag(UUID id, String name, String color) {
        return service.update(id, name, color);
    }

    public Result<DeletionOutcome, BusinessError> deleteTag(UUID personId, UUID tagId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return switch (reads.tag(personId, tagId)) {
            case Result.Success(var tag) -> {
                val effectiveStrategy = strategy != null ? strategy : DeletionStrategy.DETACH;

                service.deleteById(tag.id());
                MessageBus.submit(new TagEvents.Deleted(tag, effectiveStrategy, targetId));
                yield Result.success(new DeletionOutcome.Completed());
            }
            case Result.Failure(var error) -> Result.failure(error);
        };
    }
}
