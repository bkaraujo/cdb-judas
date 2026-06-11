package br.commons.framework.serializer;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@NullMarked
public record YamlEntry (
        String key,
        Object value
) {

    public Map<String, Object> toMap() {
        val map = new ConcurrentSkipListMap<String, Object>();
        map.put(key, value);
        return map;
    }

}
