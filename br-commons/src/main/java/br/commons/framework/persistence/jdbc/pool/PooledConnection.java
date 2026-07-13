package br.commons.framework.persistence.jdbc.pool;

import org.jspecify.annotations.NullMarked;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

@NullMarked
public record PooledConnection(
        Connection connection,
        long createdAt,
        AtomicLong lastAccessedAt
) {

    public PooledConnection(Connection connection) {
        this(connection, System.currentTimeMillis(), new AtomicLong(System.currentTimeMillis()));
    }

    void updateLastAccess() {
        lastAccessedAt.set(System.currentTimeMillis());
    }
}
