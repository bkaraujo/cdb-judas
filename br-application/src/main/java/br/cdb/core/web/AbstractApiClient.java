package br.cdb.core.web;

import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import org.jspecify.annotations.NullMarked;
import br.commons.framework.cdi.Context;

import java.util.List;
import java.util.function.Function;

@NullMarked
public abstract class AbstractApiClient {

    private HTTPApi api() { return Context.get(HTTPApi.class); }

    /** Desembrulha o {@link Result} de uma chamada direta a {@code ReadUseCase} (ver
     *  {@code F00NApiImpl}), propagando falha como {@link BusinessException} — mesmo contrato de
     *  erro que a chamada HTTP real teria no cliente. */
    protected static <T, V> V unwrap(Result<T, BusinessError> result, Function<T, V> mapper) {
        return switch (result) {
            case Result.Success<T, BusinessError>(var value) -> mapper.apply(value);
            case Result.Failure<T, BusinessError>(var error) -> throw new BusinessException(error);
        };
    }

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
