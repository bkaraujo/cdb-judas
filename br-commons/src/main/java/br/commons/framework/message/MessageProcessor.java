package br.commons.framework.message;

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
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to process " + Meta.fqn(message) + ": " + throwable, throwable);
        }
    }

}
