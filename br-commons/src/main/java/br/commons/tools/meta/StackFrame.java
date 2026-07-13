package br.commons.tools.meta;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record StackFrame(
        String className,
        int lineNumber
) {

    @Override
    public String toString() {
        return "at " + className + ":" + lineNumber;
    }

}
