package br.cdb.feature.f000._0_domain.event;

import br.cdb.core.security.User;
import br.commons.business.BusinessEvent;

public interface UserEvents extends BusinessEvent {

    record Created(
            String id,
            String username
    ) implements UserEvents {}

    record Deleted(
            String id
    ) implements UserEvents {}

}
