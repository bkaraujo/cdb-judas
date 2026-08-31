package br.cdb.feature.f004._1_application.cache;

import br.cdb.core.cache.SessionScopedCache;
import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public class TagCache {
    private final SessionScopedCache<Tag> store = new SessionScopedCache<>(
            TagLayout.PREFIX,
            personId -> {
                val repo = Context.tryGet(TagRepository.class);
                return repo != null ? repo.findAllByPerson(UUID.fromString(personId)) : List.of();
            },
            Tag::id,
            m -> TagLayout.SIZE,
            TagLayout::write);

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        store.onLogin(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        store.onLogout(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(TagEvents.Created e) {
        store.upsert(e.tag().personId().toString(), e.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(TagEvents.Updated e) {
        store.upsert(e.tag().personId().toString(), e.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(TagEvents.Deleted e) {
        store.evict(e.tag().personId().toString(), e.tag().id());
        return MessageResult.CONSUMED;
    }

    public void forEach(UUID personId, Consumer<TagLayout.View> consumer) {
        val view = new TagLayout.View();
        store.forEach(personId.toString(), seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    public boolean find(UUID personId, UUID id, Consumer<TagLayout.View> consumer) {
        val view = new TagLayout.View();
        return store.find(personId.toString(), id, seg -> {
            consumer.accept(view.bind(seg));
        });
    }
}
