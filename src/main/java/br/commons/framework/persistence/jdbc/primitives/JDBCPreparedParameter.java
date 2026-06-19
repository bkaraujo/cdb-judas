package br.commons.framework.persistence.jdbc.primitives;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record JDBCPreparedParameter(
        int index,
        @Nullable Object value
) {
}
