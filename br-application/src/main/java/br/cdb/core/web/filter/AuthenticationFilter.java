package br.cdb.core.web.filter;

import br.cdb.core.web.RequestUtils;
import br.cdb.core.web.security.AuthenticatedUser;
import br.cdb.core.web.security.CurrentUserContext;
import br.cdb.core.web.security.UserRepository;
import br.cdb.core.web.security.core.AccessTokenStore;
import br.cdb.feature.auth.LoginResource;
import br.commons.Logger;
import br.commons.framework.logger.MDC;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import static br.cdb.feature.auth.LoginResource.TOKEN_HEADER;

/**
 * Valida o token de acesso (header {@value LoginResource#TOKEN_HEADER}),
 * popula o {@link CurrentUserContext} e — fora do stream — rotaciona o token, guardando o próximo em
 * {@link #NEXT_TOKEN_PROPERTY} para o {@link AuthTokenResponseFilter} emitir na resposta.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@NullMarked
public class AuthenticationFilter implements ContainerRequestFilter {

    static final String NEXT_TOKEN_PROPERTY = "cdb.nextToken";

    @Inject
    AccessTokenStore tokenStore;

    /**
     * {@code Instance} (não injeção direta) de propósito: filtros {@code @Provider} são construídos
     * na montagem do deployment JAX-RS, antes do {@code StartupEvent} — resolver {@code UserRepository}
     * (e, por trás dele, o {@code DataSource} via {@code Registry}) cedo demais quebra o boot. Adiar
     * para o primeiro uso em {@link #authenticate} garante que o {@code StartupEvent} de
     * {@code ContextBridge} já publicou o {@code DataSource} no {@code Registry}.
     */
    @Inject
    Instance<UserRepository> userRepository;

    @Inject
    CurrentUserContext currentUser;

    @Override
    public void filter(ContainerRequestContext request) {
        if (RequestUtils.isStatic(request)) return;

        val token = request.getHeaderString(TOKEN_HEADER);
        if (token == null) return;

        if (RequestUtils.isStream(request)) {
            tokenStore.validate(token).ifPresent(userId -> authenticate(userId, request));
        } else {
            val result = tokenStore.rotate(token);
            if (result.isPresent()) {
                authenticate(result.get().userId(), request);
                request.setProperty(NEXT_TOKEN_PROPERTY, result.get().nextToken());
            } else {
                Logger.debug("AUTHN %s %s => invalid or expired token", request.getMethod(), RequestUtils.path(request));
            }
        }
    }

    private void authenticate(String userId, ContainerRequestContext request) {
        val user = userRepository.get().findById(userId).orElse(null);
        if (user == null) {
            Logger.debug("AUTHN %s %s => token references unknown user '%s'", request.getMethod(), RequestUtils.path(request), userId);
            return;
        }
        currentUser.set(new AuthenticatedUser(userId, user.username()));
        MDC.push("X-REQUEST-USER", user.username());
    }
}
