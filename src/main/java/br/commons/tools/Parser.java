package br.commons.tools;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Parser <T> {

    boolean parseable(String raw);

    T parse(String raw);

}
