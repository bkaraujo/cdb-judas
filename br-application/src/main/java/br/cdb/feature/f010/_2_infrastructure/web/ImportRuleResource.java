package br.cdb.feature.f010._2_infrastructure.web;

import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.usecase.ReadUseCase;
import br.cdb.feature.f010._1_application.usecase.WriteUseCase;
import br.cdb.feature.f010._2_infrastructure.web.request.ImportRuleRequest;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/transaction/rules")
@Produces(MediaType.APPLICATION_JSON)
public class ImportRuleResource {

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);

    @GET
    public List<ImportRule> listAll(@PathParam("uuid") UUID uuid) {
        return reads.rules(uuid);
    }

    @POST
    public RestResponse<ImportRule> create(@PathParam("uuid") UUID uuid, @Valid ImportRuleRequest req) {
        return switch (writes.createRule(uuid, req.name(), req.accountId(), req.categoryId(), req.costCenterId())) {
            case Result.Success(var rule) -> RestResponse.status(RestResponse.Status.CREATED, rule);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public ImportRule update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid ImportRuleRequest req) {
        return switch (writes.updateRule(uuid, id, req.name(), req.accountId(), req.categoryId(), req.costCenterId())) {
            case Result.Success(var rule) -> rule;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id) {
        return switch (writes.deleteRule(uuid, id)) {
            case Result.Success(var ignored) -> Response.noContent().build();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
