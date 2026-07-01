package br.community.core.web.error;

import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.RequestUtils;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@Provider
@NullMarked
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DomainException ex) {
        val instance = RequestUtils.path(uriInfo);
        val pd = switch (ex.getError()) {
            case DomainError.NotFound ignored -> ProblemDetail.of(Response.Status.NOT_FOUND, instance, ex.getMessage());
            case DomainError.Conflict ignored -> ProblemDetail.of(Response.Status.CONFLICT, instance, ex.getMessage());
            case DomainError.Validation ignored -> ProblemDetail.unprocessableContent(instance, ex.getMessage());
            case DomainError.BusinessRule ignored -> ProblemDetail.of(Response.Status.BAD_REQUEST, instance, ex.getMessage());
        };
        return Response.status(pd.status()).entity(pd).build();
    }
}
