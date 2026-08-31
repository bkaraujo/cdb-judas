package br.cdb.core.security;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SessionEvents extends BusinessEvent {

    @NullMarked
    record Login(String personId) implements SessionEvents {}

    @NullMarked
    record Logout(String personId) implements SessionEvents {}
}
