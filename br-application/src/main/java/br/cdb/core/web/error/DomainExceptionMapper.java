package br.cdb.core.web.error;

import br.cdb.core.web.HTTPRequest;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@Provider
@NullMarked
public class DomainExceptionMapper implements ExceptionMapper<BusinessException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BusinessException ex) {
        val instance = HTTPRequest.path(uriInfo);
        val error = ex.getError();
        val detail = error.render(HTTPRequest.locale());
        val pd = switch (error) {
            case BusinessError.NotFound ignored -> ProblemDetail.of(Response.Status.NOT_FOUND, instance, detail);
            case BusinessError.Conflict ignored -> ProblemDetail.of(Response.Status.CONFLICT, instance, detail);
            case BusinessError.Validation ignored -> ProblemDetail.unprocessableContent(instance, detail);
            case BusinessError.BusinessRule ignored -> ProblemDetail.of(Response.Status.BAD_REQUEST, instance, detail);
        };
        return Response.status(pd.status()).entity(pd.withCode(error.key())).build();
    }
}
