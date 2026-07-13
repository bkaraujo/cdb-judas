package br.commons.annotation;

import br.commons.Result;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Configurable {

    Result<Void, Throwable> configure(Specification specification);

}
