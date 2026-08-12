package br.cdb.core.web.error;

import br.cdb.core.web.HTTPRequest;
import br.commons.Logger;
import br.commons.tools.Strings;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Provider
@NullMarked
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception ex) {
        // Exception.class é o mais genérico possível: sem isto, ele intercepta também
        // WebApplicationExceptions internas do JAX-RS (ex.: NotAllowedException/405 em rota com
        // método sem handler), mascarando-as como 500. Respeita a resposta já embutida nelas.
        if (ex instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        val instance = HTTPRequest.path(uriInfo);

        if (isBrokenPipe(ex)) {
            Logger.debug("Client disconnected: %s", instance);
            val pd = ProblemDetail.of(Response.Status.SERVICE_UNAVAILABLE, instance, null);
            return Response.status(pd.status()).entity(pd).build();
        }

        val message = Strings.orEmpty(ex.getMessage());
        if (!message.contains("favicon.ico"))
            Logger.error(() -> message);
        val cause = ex.getCause();
        if (cause != null) { Logger.error(() -> Strings.orEmpty(cause.getMessage())); }

        val pd = ProblemDetail.of(Response.Status.INTERNAL_SERVER_ERROR, instance, "Erro interno");
        return Response.status(pd.status()).entity(pd).build();
    }

    private static boolean isBrokenPipe(Exception ex) {
        if ("ClientAbortException".equals(ex.getClass().getSimpleName())) return true;
        @Nullable Throwable t = ex;
        while (t != null) {
            val msg = t.getMessage();
            if (msg != null) {
                val lower = Strings.lower(msg);
                if (lower.contains("broken pipe") || lower.contains("pipe quebrado") || lower.contains("connection reset")) return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
