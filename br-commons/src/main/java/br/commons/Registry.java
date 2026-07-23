package br.commons;

import br.commons.tools.Meta;
import br.commons.tools.Threads;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * <p>
 *     The Registry class is a utility for managing singleton instances of various types
 *     in a thread-safe manner. It provides methods for storing, retrieving, and removing
 *     instances associated with specific types.
 *</p>
 * <p>
 *     This class uses a locking mechanism to ensure thread safety and consistency during
 *     operations on the underlying registry.
 * </p>
 */
@NullMarked
public abstract class Registry {

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<Class<?>, Object> registry = new ConcurrentHashMap<>();

    private Registry(){}

    public static <T> T get(Class<T> type) {
        return Threads.locked(lock,  () -> {
            val instance = registry.get(type);
            if (instance == null) {
                throw new IllegalStateException("No instance of %s registered".formatted(type));
            }

            return type.cast(instance);
        });
    }

    public static <T> T create(Class<T> type) {
        return Threads.locked(lock,  () -> {
            val instance = registry.get(type);
            if (instance == null) {
                throw new IllegalStateException("No instance of %s registered".formatted(type));
            }

            return type.cast(instance);
        });
    }

    public static <T> void set(Class<T> type, Supplier<T> instance) {
        Threads.locked(lock, () -> {
            Logger.verbose("Registering %s", type);
            registry.put(type, instance.get());
        });
    }

    public static <T> void set(Class<T> type) {
        Threads.locked(lock, () -> set(type, () -> Meta.instance(type)));
    }

    public static void remove(Class<?> type) {
        Threads.locked(lock, () -> {
            Logger.verbose("Removing %s", type);
            registry.remove(type);
        });
    }

    public static void clear() {
        Logger.verbose("Clearing registry");
        Threads.locked(lock, registry::clear);
    }


    public static <T> T tryGet(Class<T> type) {
        return tryGet(type, () -> Meta.instance(type));
    }

    public static <T> T tryGet(Class<T> type, Supplier<T> supplier) {
        return Threads.locked(lock, () -> {
            var instance = registry.get(type);
            if (instance == null) {
                instance = supplier.get();
                Logger.verbose("Registering %s", type);
                registry.put(type, instance);
            }

           return type.cast(instance);
        });
    }
}
