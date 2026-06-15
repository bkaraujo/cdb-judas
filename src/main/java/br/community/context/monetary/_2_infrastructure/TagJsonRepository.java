package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._0_domain.repository.TagRepository;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@NullMarked
public final class TagJsonRepository extends AbstractJsonRepository<Tag, UUID> implements TagRepository {

    public TagJsonRepository(ObjectMapper mapper, Storage storage) {
        super(mapper, storage, Tag.class);
    }

    @Override
    protected String jsonKey() { return "tags"; }

    @Override
    protected UUID getId(Tag entity) { return entity.id(); }
}
