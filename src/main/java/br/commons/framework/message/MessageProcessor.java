package br.commons.framework.message;

import br.commons.Logger;
import br.commons.tools.Meta;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;

@NullMarked
public record MessageProcessor (
        Object container,
        MethodHandle handle
) {

    public MessageResult process(Message message) {
        try {
            return (MessageResult) handle.invoke(container, message);
        } catch (Throwable throwable) {
            Logger.fatal("Failed to process %s: %s", Meta.fqn(message), throwable.toString());
            return MessageResult.CONSUMED;
        }
    }

}
