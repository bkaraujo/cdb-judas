package br.cdb.feature.f005._1_application.cache;

import br.cdb.core.cache.SessionScopedCache;
import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f005._0_domain.event.CategoryEvents;
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.repository.CategoryRepository;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public class CategoryCache {
    private final SessionScopedCache<Category> store = new SessionScopedCache<>(
            CategoryLayout.PREFIX,
            personId -> Context.get(CategoryRepository.class).findAllByPerson(UUID.fromString(personId)),
            Category::id,
            m -> CategoryLayout.SIZE,
            CategoryLayout::write);

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
    public MessageResult onCreated(CategoryEvents.Created e) {
        store.upsert(e.category().personId().toString(), e.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(CategoryEvents.Updated e) {
        store.upsert(e.category().personId().toString(), e.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(CategoryEvents.Deleted e) {
        store.evict(e.personId().toString(), e.categoryId());
        return MessageResult.CONSUMED;
    }

    public void forEach(UUID personId, Consumer<CategoryLayout.View> consumer) {
        val view = new CategoryLayout.View();
        store.forEach(personId.toString(), seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    public boolean find(UUID personId, UUID id, Consumer<CategoryLayout.View> consumer) {
        val view = new CategoryLayout.View();
        return store.find(personId.toString(), id, seg -> {
            consumer.accept(view.bind(seg));
        });
    }
}
