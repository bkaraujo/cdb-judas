package br.community.infra.persistence;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.ObjectMapper;

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
