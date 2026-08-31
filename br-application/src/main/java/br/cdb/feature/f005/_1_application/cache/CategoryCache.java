package br.cdb.feature.f005._1_application.cache;

import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f005._0_domain.event.CategoryEvents;
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.commons.cache.AbstractCache;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.UUID;

@NullMarked
public class CategoryCache extends AbstractCache<Category> {

    public CategoryCache() {
        super(
            CategoryLayout.PREFIX,
            // findAll, não findAllByPerson: as duas globais de transferência pertencem ao
            // SYSTEM_PERSON_ID e precisam entrar no cache de toda pessoa (ver UserCategoryService).
            personId -> {
                val service = Context.tryGet(UserCategoryService.class);
                return service.findAll(UUID.fromString(personId));
            },
            Category::id,
            m -> CategoryLayout.SIZE,
            CategoryLayout::write
        );
    }

    @Override
    protected Category mapToDomain(MemorySegment segment) {
        val view = new CategoryLayout.View().bind(segment);
        return new Category(
                view.id(), view.personId(), view.nature(), view.name(),
                view.parentId(), view.isSystem(), view.active(),
                view.createdAt(), view.updatedAt()
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
    public MessageResult onCreated(CategoryEvents.Created e) {
        upsert(e.category().personId().toString(), e.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(CategoryEvents.Updated e) {
        upsert(e.category().personId().toString(), e.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(CategoryEvents.Deleted e) {
        evict(e.personId().toString(), e.categoryId());
        return MessageResult.CONSUMED;
    }
}
