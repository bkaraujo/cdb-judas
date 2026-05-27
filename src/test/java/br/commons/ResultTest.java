package br.commons;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successWithValue_isSuccess() {
        Result<Integer, String> result = Result.success(42);
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }

    @Test
    void successVoid_isSuccess() {
        Result<Void, String> result = Result.success();
        assertTrue(result.isSuccess());
        assertNull(result.getOrThrow());
    }

    @Test
    void failureString_isFailure() {
        Result<Integer, String> result = Result.<Integer>failure("oops");
        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
    }

    @Test
    void failureThrowable_isFailure() {
        Result<Integer, Throwable> result = Result.<Integer>failure(new RuntimeException("boom"));
        assertTrue(result.isFailure());
    }

    @Test
    void getOrThrow_successReturnsValue() {
        assertEquals(10, Result.success(10).getOrThrow());
    }

    @Test
    void getOrThrow_failureThrows() {
        Result<Integer, String> failure = Result.<Integer>failure("err");
        assertThrows(RuntimeException.class, failure::getOrThrow);
    }

    @Test
    void getOrElse_fallbackValue_onSuccess() {
        assertEquals(5, Result.success(5).getOrElse(99));
    }

    @Test
    void getOrElse_fallbackValue_onFailure() {
        Result<Integer, String> failure = Result.<Integer>failure("err");
        assertEquals(99, failure.getOrElse(99));
    }

    @Test
    void getOrElse_supplier_onSuccess() {
        assertEquals(5, Result.success(5).getOrElse(() -> 99));
    }

    @Test
    void getOrElse_supplier_onFailure() {
        Result<Integer, String> failure = Result.<Integer>failure("err");
        assertEquals(99, failure.getOrElse(() -> 99));
    }

    @Test
    void map_success_transformsValue() {
        Result<Integer, String> base = Result.success(10);
        Result<String, String> mapped = base.map(String::valueOf);
        assertTrue(mapped.isSuccess());
        assertEquals("10", mapped.getOrThrow());
    }

    @Test
    void map_failure_propagatesError() {
        Result<Integer, String> failure = Result.<Integer>failure("error");
        Result<String, String> mapped = failure.map(String::valueOf);
        assertTrue(mapped.isFailure());
        assertThrows(RuntimeException.class, mapped::getOrThrow);
    }

    @Test
    void flatMap_success_chainsResult() {
        Result<Integer, String> base = Result.success(10);
        Result<String, String> result = base.flatMap(v -> Result.success(String.valueOf(v)));
        assertTrue(result.isSuccess());
        assertEquals("10", result.getOrThrow());
    }

    @Test
    void flatMap_failure_propagatesError() {
        Result<Integer, String> failure = Result.<Integer>failure("error");
        Result<String, String> result = failure.flatMap(v -> Result.success(String.valueOf(v)));
        assertTrue(result.isFailure());
    }

    @Test
    void recover_failure_returnsRecoveredValue() {
        Result<Integer, String> failure = Result.<Integer>failure("err");
        Result<Integer, String> recovered = failure.recover(e -> -1);
        assertTrue(recovered.isSuccess());
        assertEquals(-1, recovered.getOrThrow());
    }

    @Test
    void recover_success_unchanged() {
        Result<Integer, String> success = Result.success(42);
        Result<Integer, String> result = success.recover(e -> -1);
        assertTrue(result.isSuccess());
        assertEquals(42, result.getOrThrow());
    }

    @Test
    void ifSuccess_calledOnSuccess() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result.success(1).ifSuccess(v -> called.set(true));
        assertTrue(called.get());
    }

    @Test
    void ifSuccess_notCalledOnFailure() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result.<Integer>failure("err").ifSuccess(v -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    void ifFailure_calledOnFailure() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result.<Integer>failure("err").ifFailure(e -> called.set(true));
        assertTrue(called.get());
    }

    @Test
    void ifFailure_notCalledOnSuccess() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result.success(1).ifFailure(e -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    void ifSuccess_returnsThis_allowsChaining() {
        Result<Integer, String> r = Result.success(5);
        assertSame(r, r.ifSuccess(v -> {}));
    }

    @Test
    void ifFailure_returnsThis_allowsChaining() {
        Result<Integer, String> r = Result.<Integer>failure("err");
        assertSame(r, r.ifFailure(e -> {}));
    }
}
