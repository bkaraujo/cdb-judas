package br.commons.tools.meta;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record StackFrame(
        String className,
        String methodName,
        int lineNumber
) {

    @Override
    public String toString() {
        return "at " + className + "." + methodName + ":" + lineNumber;
    }

}
