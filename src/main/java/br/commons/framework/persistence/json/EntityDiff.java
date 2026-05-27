package br.commons.framework.persistence.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

@NullMarked
public final class EntityDiff {

    private EntityDiff() {}

    @SuppressWarnings("unchecked")
    public static <T> String of(ObjectMapper mapper, @Nullable T before, T after) {
        if (before == null) {
            return " [NEW] " + after;
        }
        try {
            val beforeMap = (Map<String, Object>) mapper.convertValue(before, Map.class);
            val afterMap = (Map<String, Object>) mapper.convertValue(after, Map.class);
            val allKeys = new TreeSet<String>();
            allKeys.addAll(beforeMap.keySet());
            allKeys.addAll(afterMap.keySet());
            val sb = new StringBuilder();
            for (val key : allKeys) {
                val oldVal = beforeMap.get(key);
                val newVal = afterMap.get(key);
                if (!Objects.equals(oldVal, newVal)) {
                    sb.append("\n  ").append(key).append(": ").append(oldVal).append(" → ").append(newVal);
                }
            }
            return sb.isEmpty() ? " (no changes)" : sb.toString();
        } catch (Exception e) {
            return " (diff unavailable: " + e.getMessage() + ")";
        }
    }
}
