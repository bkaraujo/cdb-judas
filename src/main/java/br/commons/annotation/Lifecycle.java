package br.commons.annotation;

import br.commons.Result;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Lifecycle {

    default Result<Void, Throwable> initialize() { return Result.success(); }
    default Result<Void, Throwable> terminate() { return Result.success(); }

}
