package br.cdb.feature.f004;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._1_application.usecase.ReadUseCase;
import br.cdb.feature.f004._1_application.usecase.WriteUseCase;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a mutação de tag da fatia {@code f004} — o par de {@code ReadUseCaseTest}. A exclusão é
 * verificada pelo evento publicado ({@link TagEvents.Deleted}): quem age sobre os vínculos é
 * {@code f006}, best-effort, fora desta fatia.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private WriteUseCase useCase;
    private DeletedEvents deleted;

    /** Captura o evento de exclusão — a estratégia efetiva só é observável por ele. */
    static class DeletedEvents {
        final List<TagEvents.Deleted> events = new ArrayList<>();

        @MessageListener
        public MessageResult onDeleted(TagEvents.Deleted event) {
            events.add(event);
            return MessageResult.CONSUMED;
        }
    }

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        useCase = new WriteUseCase();

        deleted = new DeletedEvents();
        MessageBus.subscribe(deleted);
    }

    private Tag seedTag(UUID personId, String name) {
        return tagRepository().save(new Tag(UUID.randomUUID(), personId, name, "#ff0000", null));
    }

    private TagEvents.@Nullable Deleted lastDeleted() {
        return deleted.events.isEmpty() ? null : deleted.events.getLast();
    }

    @Test
    @DisplayName("createTag persiste a tag da pessoa")
    void createsTag() {
        val tag = useCase.createTag(PERSON_ID, "Mercado", "#00ff00");

        assertEquals(PERSON_ID, tag.personId());
        assertEquals("Mercado", tag.name());
        assertEquals("#00ff00", tag.color());
        assertTrue(tagRepository().findById(tag.id()).isPresent());
    }

    @Test
    @DisplayName("updateTag troca nome e cor preservando o dono")
    void updatesTag() {
        val tag = seedTag(PERSON_ID, "Mercado");

        val r = useCase.updateTag(tag.id(), "Supermercado", "#0000ff");

        assertTrue(r.isSuccess());
        val updated = ((Result.Success<Tag, BusinessError>) r).value();
        assertEquals("Supermercado", updated.name());
        assertEquals("#0000ff", updated.color());
        assertEquals(PERSON_ID, updated.personId());
    }

    @Test
    @DisplayName("updateTag de tag inexistente → NotFound")
    void updateUnknownTag() {
        val r = useCase.updateTag(UUID.randomUUID(), "X", "#000000");
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Tag, BusinessError>) r).error());
    }

    @Test
    @DisplayName("deleteTag sem estratégia apaga a tag e publica DETACH")
    void deleteDefaultsToDetach() {
        val tag = seedTag(PERSON_ID, "Mercado");

        val r = useCase.deleteTag(PERSON_ID, tag.id(), null, null);

        assertTrue(r.isSuccess());
        assertTrue(tagRepository().findById(tag.id()).isEmpty());
        assertEquals(DeletionStrategy.DETACH, assertInstanceOf(TagEvents.Deleted.class, lastDeleted()).strategy());
    }

    @Test
    @DisplayName("deleteTag com MOVE publica a estratégia e o destino")
    void deleteWithMoveCarriesTarget() {
        val tag = seedTag(PERSON_ID, "Mercado");
        val target = seedTag(PERSON_ID, "Compras");

        val r = useCase.deleteTag(PERSON_ID, tag.id(), DeletionStrategy.MOVE, target.id());

        assertTrue(r.isSuccess());
        val event = assertInstanceOf(TagEvents.Deleted.class, lastDeleted());
        assertEquals(DeletionStrategy.MOVE, event.strategy());
        assertEquals(target.id(), event.targetId());
    }

    @Test
    @DisplayName("deleteTag de outra pessoa → NotFound, tag preservada e sem evento")
    void deleteRejectsTagOfAnotherPerson() {
        val tag = seedTag(OTHER_PERSON_ID, "De outro dono");

        val r = useCase.deleteTag(PERSON_ID, tag.id(), null, null);

        assertTrue(r.isFailure());
        assertTrue(tagRepository().findById(tag.id()).isPresent());
        assertNull(lastDeleted());
    }
}
