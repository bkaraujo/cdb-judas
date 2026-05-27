package br.commons.framework.logger;

import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;
import java.util.Map;

@NullMarked
public final class MDC {
    private static final ThreadLocal<Map<String, String>> context = ThreadLocal.withInitial(LinkedHashMap::new);

    private MDC() {}

    public static void push(String key, String value) {
        context.get().put(key, value);
    }

    public static void pop(String key) {
        val map = context.get();
        map.remove(key);
        if (map.isEmpty()) { context.remove(); }
    }

    public static String prefix() {
        val map = context.get();
        if (map.isEmpty()) return Strings.EMPTY;

        val sb = new StringBuilder("[");

        var first = true;
        for (val entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        sb.append("]");
        return sb.toString();
    }
}
