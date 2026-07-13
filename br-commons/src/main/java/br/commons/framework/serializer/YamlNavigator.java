package br.commons.framework.serializer;

import br.commons.Logger;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@NullMarked
final class YamlNavigator {
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private YamlNavigator() {}

    static String[] splitKey(String key) {
        return DOT_PATTERN.split(key);
    }

    @Nullable
    static Object findInRaw(Map<String, Object> raw, String key) {
        val tokens = DOT_PATTERN.split(key, -1);

        Object container = raw;
        for (val token : tokens) {
            if (container instanceof Map<?, ?> map) {
                val next = map.get(token);
                if (next != null) {
                    container = next;
                    continue;
                }
                return null;
            }

            if (container instanceof List<?> list) {
                try {
                    val index = Integer.parseInt(token);
                    if (index >= list.size()) {
                        Logger.warn("Element index (%d) beyond bounds (%d)", index, list.size() - 1);
                        return null;
                    }
                    container = list.get(index);
                } catch (NumberFormatException e) {
                    Logger.fatal("Expected a number, got %s", token);
                    return null;
                }
            }
        }

        return container;
    }
}
