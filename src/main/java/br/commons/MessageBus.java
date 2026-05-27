package br.commons;

import br.commons.framework.message.Message;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageProcessor;
import br.commons.framework.message.MessageResult;
import br.commons.tools.Meta; import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static br.commons.Logger.lazy;

@NullMarked
public abstract class MessageBus {
    private MessageBus() { /* Private constructor */ }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final List<MessageProcessor> EMPTY = List.of();

    private static final ConcurrentMap<Class<?>, List<MessageProcessor>> processors = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, List<MessageProcessor>> dispatchCache = new ConcurrentHashMap<>();

    public static int subscribe(Class<?> container) {
        return subscribe(Meta.instance(container));
    }

    public static int subscribe(Object container) {
        int subscribed = 0;

        Logger.verbose("Processing %s", lazy(() -> Meta.fqn(container)));
        for (val method : container.getClass().getMethods()) {
            val annotation = method.getAnnotation(MessageListener.class);
            if (annotation == null) { continue; }

            val parameters = method.getParameters();
            if (method.getReturnType() != MessageResult.class) {
                Logger.warn(
                        "Method '%s.%s' must return %s",
                        Meta.fqn(container),
                        method.getName(),
                        Meta.fqn(MessageResult.class)
                );
                continue;
            }

            if (parameters.length != 1) {
                Logger.warn(
                        "Method '%s.%s' must accept a single parameter",
                        Meta.fqn(container),
                        method.getName()
                );
                continue;
            }

            if (!Meta.assignable(parameters[0].getType(), Message.class)) {
                Logger.warn(
                        "Method '%s.%s' parameter must be assignable to %s",
                        Meta.fqn(container),
                        method.getName(),
                        Meta.fqn(Message.class)
                );

                continue;
            }

            try { method.setAccessible(true); }
            catch (final SecurityException e) {
                Logger.warn(
                        "Failed to make '%s.%s' accessible'",
                        Meta.fqn(container),
                        method.getName()
                );

                continue;
            }

            try {
                val handle = LOOKUP.unreflect(method);
                subscribed++;
                processors
                        .computeIfAbsent(parameters[0].getType(), ignored -> new ArrayList<>())
                        .add(new MessageProcessor(container, handle));

                Logger.verbose("Method accepted: %s(%s)", Logger.lazy(method::getName), Logger.lazy(() -> Meta.fqn(parameters[0].getType())));
            } catch (IllegalAccessException e) {
                Logger.warn(
                        "Failed to create handle for '%s.%s': %s",
                        Meta.fqn(container),
                        method.getName(),
                        Strings.orEmpty(e.getMessage())
                );
            }
        }

        // Invalidate dispatch cache when new processors are registered
        dispatchCache.clear();

        return subscribed;
    }

    public static <T extends Message> void submit(T message) {
        val messageClass = message.getClass();
        val processorList = dispatchCache.computeIfAbsent(messageClass, MessageBus::buildDispatchList);

        for (val processor : processorList) {
            if (processor.process(message) == MessageResult.AVAILABLE) {
                break;
            }
        }
    }

    private static List<MessageProcessor> buildDispatchList(Class<?> messageClass) {
        Logger.trace("Building dispatch list for %s", lazy(() -> Meta.fqn(messageClass)));

        val result = new ArrayList<MessageProcessor>();

        // Walk hierarchy from Object to concrete class (reversed order)
        val hierarchy = new ArrayList<Class<?>>();
        Class<?> klass = messageClass;
        while (klass != Object.class) {
            hierarchy.add(klass);
            klass = klass.getSuperclass();
        }

        // Process from base to concrete (reversed)
        for (var i = hierarchy.size() - 1; i >= 0; i--) {
            result.addAll(processors.getOrDefault(hierarchy.get(i), EMPTY));
        }

        return result.isEmpty() ? EMPTY : List.copyOf(result);
    }

}
