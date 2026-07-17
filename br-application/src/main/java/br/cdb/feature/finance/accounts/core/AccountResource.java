package br.cdb.feature.finance.accounts.core;

import br.cdb.context.monetary._1_application.command.AccountCommand;
import br.cdb.feature.finance.deletion.DeletionStrategy;
import br.cdb.feature.finance.deletion.Deletions;
import br.cdb.feature.user.UserUseCase;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AccountResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final UserUseCase userUseCase;

    @GET
    public List<AccountResponse> listAll() {
        return switch (userUseCase.accounts()) {
            case Result.Success(var views) -> views.stream().map(AccountResource::toDto).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @GET
    @Path("/{id}")
    public AccountResponse getById(@PathParam("id") UUID id) {
        return switch (userUseCase.account(id)) {
            case Result.Success(var view) -> toDto(view);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @POST
    public RestResponse<AccountResponse> create(@Valid AccountRequest req) {
        return switch (userUseCase.createAccount(toCreateCommand(req), req.color())) {
            case Result.Success(var view) -> RestResponse.status(RestResponse.Status.CREATED, toDto(view));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public AccountResponse update(@PathParam("id") UUID id, @Valid AccountRequest req) {
        return switch (userUseCase.updateAccount(toUpdateCommand(id, req), req.color())) {
            case Result.Success(var view) -> toDto(view);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
            @PathParam("uuid") UUID uuid,
            @PathParam("id") UUID id,
            @QueryParam("strategy") @Nullable String strategy,
            @QueryParam("targetId") @Nullable UUID targetId
    ) {
        return Deletions.execute(strategy, targetId, ALLOWED_STRATEGIES,
                parsed -> userUseCase.deleteAccount(uuid, id, parsed, targetId),
                "a esta conta.");
    }

    private static AccountResponse toDto(UserUseCase.AccountView view) {
        return AccountResponse.from(view.account(), view.overlay(), view.cards(), view.transactions());
    }

    private static AccountCommand.Create toCreateCommand(AccountRequest req) {
        return new AccountCommand.Create(req.name(), req.type(), req.active(),
                req.creditLimit(), req.overdraftLimit(), req.closingDay(), req.dueDay());
    }

    private static AccountCommand.Update toUpdateCommand(UUID id, AccountRequest req) {
        return new AccountCommand.Update(id, req.name(), req.type(), req.active(),
                req.creditLimit(), req.overdraftLimit(), req.closingDay(), req.dueDay());
    }
}
