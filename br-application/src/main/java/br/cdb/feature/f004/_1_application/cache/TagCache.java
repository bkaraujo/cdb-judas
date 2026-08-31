package br.cdb.feature.f004._1_application.cache;

import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.commons.cache.AbstractCache;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.UUID;

@NullMarked
public class TagCache extends AbstractCache<Tag> {

    public TagCache() {
        super(
                TagLayout.PREFIX,
                personId -> {
                    val repo = Context.tryGet(TagRepository.class);
                    return repo.findAllByPerson(UUID.fromString(personId));
                },
                Tag::id,
                _ -> TagLayout.SIZE,
                TagLayout::write
        );
    }

    @Override
    protected Tag mapToDomain(MemorySegment segment) {
        val view = new TagLayout.View().bind(segment);
        return new Tag(
                view.id(), view.personId(), view.name(), view.color(),
                view.createdAt()
        );
    }

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        onLogin(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        onLogout(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(TagEvents.Created e) {
        upsert(e.tag().personId().toString(), e.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(TagEvents.Updated e) {
        upsert(e.tag().personId().toString(), e.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(TagEvents.Deleted e) {
        evict(e.tag().personId().toString(), e.tag().id());
        return MessageResult.CONSUMED;
    }
}
