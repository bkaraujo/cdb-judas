package br.commons.tools;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Parser <T> {

    T parse(String raw);

}
