package br.commons.tools;

import br.commons.Logger;
import br.commons.RT;
import br.commons.tools.meta.StackFrame;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@NullMarked
public abstract class Meta {
    private Meta(){}

    public static boolean IS_LINUX = false;
    public static boolean IS_WINDOWS = false;

    public static <T> T instance(Class<T> type, String property) {
        try {
            val instance = instance(Class.forName(property));
            return cast(instance, type);
        } catch (Throwable throwable) {
            exit(99, "Failed to create %s", property, throwable);
            return cast(new Object(), type);
        }
    }

    public static <T> T instance(Class<T> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (Throwable throwable) {
            exit(99, "Failed to create %s", type, throwable);
            return cast(new Object(), type);
        }
    }

    public static <T> T instance(Class<T> type, Object ... args) {
        try {
            return cast(type.getConstructor().newInstance(args), type);
        } catch (Throwable throwable) {
            exit(99, "Failed to create %s: %s", type, throwable);
            return cast(new Object(), type);
        }
    }

    public static void close(@Nullable Closeable closeable){
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException exception) {
            Logger.warn("Failed to close %s: %s", closeable, exception.toString());
        }
    }

    public static void exit(int code) {
        exit(code, null);
    }

    public static void exit(@Nullable String message, @Nullable Object ... args) {
        exit(99, message, args);
    }

    public static void exit(int code, @Nullable String message, @Nullable Object ... args) {
        if (message != null) { Logger.error(message, args); }
        System.exit(code);
    }

    public static String fqn(Object type) {
        return fqn(type.getClass());
    }

    public static String fqn(Class<?> type) {
        return type.getTypeName();
    }

    public static <T> T cast(Object object, Class<T> type) {
        if (!assignable(object, type)) { exit(99, "{} is not assignable to %s", type, object); }
        return type.cast(object);
    }

    public static boolean assignable(@Nullable Object object, @Nullable Class<?> type) {
        if (object == null || type == null) return false;
        return assignable(object.getClass(), type);
    }

    public static boolean assignable(@Nullable Class<?> object, @Nullable Class<?> type) {
        if (object == null || type == null) return false;
        if (!object.isPrimitive()) { return type.isAssignableFrom(object); }

        val typeName = type.getSimpleName();
        val objectName = object.getSimpleName();
        if (typeName.equalsIgnoreCase(objectName)) { return true; }
        if (typeName.equals("int")) { return objectName.equalsIgnoreCase("integer"); }

        return false;
    }

    /**
     * Evaluates arguments, converting Suppliers to their values.
     *
     * @param args Mixed array of values and Suppliers
     * @return Array with all Suppliers evaluated
     */
    public static Object[] evaluate(Object[] args) {
        val evaluated = new Object[args.length];

        for (var i = 0; i < args.length; i++) {
            evaluated[i] = switch (args[i]) {
                case Supplier<?> supplier -> supplier.get();
                default -> args[i];
            };
        }

        return evaluated;
    }

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static List<StackFrame> stackFrame() {
        return STACK_WALKER
                .walk(stream -> stream
                        .filter(frame -> !isStackFrameExcluded(frame.getClassName()))
                        .map(frame -> new StackFrame(frame.getClassName(), frame.getLineNumber()))
                        .toList()
                );
    }

    public static StackFrame stackFrame(int skipFrames) {
        val frames = stackFrame();
        return skipFrames < frames.size()
                ? frames.get(skipFrames)
                : new StackFrame("Unknown", 0);
    }

    public static @Nullable Field field(Object container, String name) {
        for (val field : fields(container)) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public static Set<Field> fields(Object container) {
        return fields(container.getClass());
    }

    private static final Map<Class<?>, Set<Field>> knownFields = new ConcurrentHashMap<>();

    @SuppressWarnings("EmptyCatch")
    private static Set<Field> fields(Class<?> container) {
        if (knownFields.containsKey(container)) { return knownFields.get(container); }

        val fields = new HashSet<Field>();

        Class<?> klass = container;
        while (klass != null) {
            try {
                fields.addAll(List.of(klass.getFields()));
                fields.addAll(List.of(klass.getDeclaredFields()));
            } catch (Throwable ignored) {}

            klass = klass.getSuperclass();
        }

        if (!knownFields.containsKey(container)) {
            Logger.trace("Creating Set<Field> for %s", container);
            knownFields.put(container, fields);
        }

        return fields;
    }

    private static final ConcurrentHashMap<String, Boolean> exclusionCache = new ConcurrentHashMap<>();

    private static boolean isStackFrameExcluded(String className) {
        return exclusionCache.computeIfAbsent(className, name -> {
            for (val pkg : RT.packages) {
                if (name.startsWith(pkg)) return true;
            }
            return false;
        });
    }

    private static String mainClassName = Strings.EMPTY;
    public static String userMainClassName() {
        if (mainClassName.isEmpty()) {
            mainClassName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(stream -> stream
                            .map(StackWalker.StackFrame::getDeclaringClass)
                            // Filtra classes que NÃO são de infraestrutura
                            .filter(clazz -> {
                                String name = clazz.getName();
                                return RT.packages.stream().noneMatch(name::startsWith);
                            })
                            // Queremos o "fundo" da pilha filtrada (o ponto de entrada do usuário)
                            // O reduce percorre o stream até o fim e mantém o último elemento
                            .reduce((primeiro, segundo) -> segundo)
                            .map(Class::getName)
                    ).orElse(Strings.EMPTY);

        }

        return mainClassName;
    }

    private static String mainPackageName = Strings.EMPTY;
    public static String mainPackageName() {
        if (mainPackageName.isEmpty()) {
            val mainClassName = userMainClassName();
            mainPackageName = mainClassName.substring(0, mainClassName.lastIndexOf('.'));
        }
        return mainPackageName;
    }
}
