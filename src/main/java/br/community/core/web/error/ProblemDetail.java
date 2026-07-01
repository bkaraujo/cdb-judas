package br.community.core.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/** Corpo `application/problem+json` (RFC 7807), equivalente ao antigo `org.springframework.http.ProblemDetail`. */
@NullMarked
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        @Nullable String detail,
        @Nullable String instance,
        @Nullable String code
) {

    public static ProblemDetail of(Response.Status status, @Nullable String instance, @Nullable String detail) {
        return of(status.getStatusCode(), status.getReasonPhrase(), instance, detail);
    }

    public static ProblemDetail of(int status, String reason, @Nullable String instance, @Nullable String detail) {
        return new ProblemDetail("about:blank", reason, status, detail, instance, null);
    }

    /** 422 não tem constante em {@link Response.Status} (RFC 9110 renomeou para "Unprocessable Content"). */
    public static ProblemDetail unprocessableContent(@Nullable String instance, @Nullable String detail) {
        return of(422, "Unprocessable Content", instance, detail);
    }

    public ProblemDetail withCode(String newCode) {
        return new ProblemDetail(type, title, status, detail, instance, newCode);
    }
}
