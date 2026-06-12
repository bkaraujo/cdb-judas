package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class AccountJsonRepository extends AbstractJsonRepository<MonetaryAccount, UUID> implements AccountRepository {

    public AccountJsonRepository(ObjectMapper mapper, Storage storage) {
        super(mapper, storage, MonetaryAccount.class);
    }

    @Override
    protected String jsonKey() { return "accounts"; }

    @Override
    protected UUID getId(MonetaryAccount entity) { return entity.id(); }
}
