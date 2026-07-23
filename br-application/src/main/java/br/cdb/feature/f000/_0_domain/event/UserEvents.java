package br.cdb.feature.f000._0_domain.event;

import br.cdb.core.security.User;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UserEvents extends BusinessEvent {

    @NullMarked
    record Created(
            String id,
            String username
    ) implements UserEvents {}

    @NullMarked
    record Deleted(
            String id
    ) implements UserEvents {}

}
