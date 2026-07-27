package br.cdb.core.web;

import br.cdb.core.web.error.ProblemDetail;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class HTTPResponse {
    private HTTPResponse(){}

    public static Response tooLarge(String code, String detail) {
        return problem(413, "Content Too Large", code, detail);
    }

    public static Response unprocessable(String code, String detail) {
        return problem(422, "Unprocessable Content", code, detail);
    }

    public static Response badRequest(String code, String detail) {
        return problem(Response.Status.BAD_REQUEST, code, detail);
    }

    public static Response problem(Response.Status status, String code, String detail) {
        return problem(status.getStatusCode(), status.getReasonPhrase(), code, detail);
    }

    public static Response problem(int status, String reason, String code, String detail) {
        val pd = ProblemDetail.of(status, reason, null, detail).withCode(code);
        return Response.status(status).entity(pd).build();
    }
}
