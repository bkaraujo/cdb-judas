package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class CategoryJsonRepository extends AbstractJsonRepository<MonetaryCategory, UUID> implements CategoryRepository {

    public CategoryJsonRepository(ObjectMapper mapper, Storage storage) {
        super(mapper, storage, MonetaryCategory.class);
    }

    @Override
    protected String jsonKey() { return "categories"; }

    @Override
    protected UUID getId(MonetaryCategory entity) { return entity.id(); }
}
