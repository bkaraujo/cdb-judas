package br.cdb.core.web;

import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public abstract class AbstractApiClient {

    private HTTPApi api() { return Context.get(HTTPApi.class); }

    protected final <T> T get(String path, Class<T> responseType) {
        return api().get("/" + path, responseType);
    }

    protected final <T> List<T> list(String path, Class<T[]> responseType) {
        return List.of(api().get("/" + path, responseType));
    }

    protected final <B, T> T post(String path, B body, Class<T> responseType) {
        return api().post("/" + path, body, responseType);
    }

    protected final <B, T> T patch(String path, B body, Class<T> responseType) {
        return api().patch("/" + path, body, responseType);
    }

    protected final void delete(String path) {
        api().delete("/" + path);
    }

}
