package br.cdb.feature.f004._1_application.usecase;

import br.cdb.context.monetary.AbstractUseCaseTest;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura de tag da fatia {@code f004} — o par de {@code WriteUseCaseTest}. Aqui o fake
 * <em>modela</em> {@code COD_PERSON} (personId é campo de {@link Tag}), então a guarda implícita por
 * pessoa é exercitável fora do HTTP; o caminho completo é de {@code F004TagResourceTest}.
 */
class ReadUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private ReadUseCase reads;

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        reads = new ReadUseCase();
    }

    private Tag seedTag(UUID personId, String name) {
        return tagRepository().save(new Tag(UUID.randomUUID(), personId, name, "#ff0000", null));
    }

    @Test
    @DisplayName("tags devolve só as tags da pessoa")
    void listsTagsOfPerson() {
        seedTag(PERSON_ID, "Mercado");
        seedTag(PERSON_ID, "Lazer");
        seedTag(OTHER_PERSON_ID, "De outro dono");

        val tags = reads.tags(PERSON_ID);

        assertEquals(List.of("Mercado", "Lazer"), tags.stream().map(Tag::name).toList());
    }

    @Test
    @DisplayName("tag devolve a tag da pessoa")
    void findsTagOfPerson() {
        val tag = seedTag(PERSON_ID, "Mercado");

        val r = reads.tag(PERSON_ID, tag.id());

        assertTrue(r.isSuccess());
        assertEquals(tag.id(), ((Result.Success<Tag, BusinessError>) r).value().id());
    }

    @Test
    @DisplayName("tag de outra pessoa → NotFound (guarda implícita por COD_PERSON)")
    void hidesTagOfAnotherPerson() {
        val tag = seedTag(OTHER_PERSON_ID, "De outro dono");

        val r = reads.tag(PERSON_ID, tag.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Tag, BusinessError>) r).error());
    }

    @Test
    @DisplayName("tag inexistente → NotFound")
    void findUnknownTag() {
        val r = reads.tag(PERSON_ID, UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Tag, BusinessError>) r).error());
    }
}
