package br.commons;

import br.community.context.shared._0_domain.model.DomainError;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    static <T, E> Result<T, E> success(@Nullable T value) {
        return new Success<>(value);
    }

    static <E> Result<Void, E> success() {
        return success(null);
    }

    static <T> Result<T, String> failure(String error) {
        return new Failure<>(error);
    }

    static <T> Result<T, DomainError> failure(DomainError error) {
        return new Failure<>(error);
    }

    static <T> Result<T, Throwable> failure(Throwable throwable) {
        return new Failure<>(throwable);
    }

    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }

    default T getOrThrow() {
        return switch (this) {
            case Success<T, E>(T v) -> v;
            case Failure<T, E>(E e) -> throw new RuntimeException("Result is a failure: " + e);
        };
    }

    default T getOrElse(T fallback) {
        return switch (this) {
            case Success<T, E>(T v) -> v;
            case Failure<T, E> ignored -> fallback;
        };
    }

    default T getOrElse(Supplier<T> fallback) {
        return switch (this) {
            case Success<T, E>(T v) -> v;
            case Failure<T, E> ignored -> fallback.get();
        };
    }

    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T, E>(T v) -> Result.success(mapper.apply(v));
            case Failure<T, E>(E e) -> new Failure<>(e);
        };
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(T v) -> mapper.apply(v);
            case Failure<T, E>(E e) -> new Failure<>(e);
        };
    }

    default Result<T, E> recover(Function<? super E, ? extends T> recovery) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(E e) -> Result.success(recovery.apply(e));
        };
    }

    default Result<T, E> ifSuccess(Consumer<? super T> action) {
        if (this instanceof Success<T, E>(T v)) action.accept(v);
        return this;
    }

    default Result<T, E> ifFailure(Consumer<? super E> action) {
        if (this instanceof Failure<T, E>(E e)) action.accept(e);
        return this;
    }

    @NullMarked
    record Success<T, E>(@Nullable T value) implements Result<T, E> {}

    @NullMarked
    record Failure<T, E>(E error) implements Result<T, E> {}
}
