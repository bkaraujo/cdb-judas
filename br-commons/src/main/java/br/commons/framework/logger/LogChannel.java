package br.commons.framework.logger;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface LogChannel {

    void write(String message);

}
