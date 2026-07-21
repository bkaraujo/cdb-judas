package br.cdb.core.web.filter;

import br.cdb.core.security.AccessTokenStore;
import br.cdb.core.security.UserRepository;
import br.cdb.core.web.Request;
import br.cdb.core.web.security.AuthenticatedUser;
import br.cdb.feature.f000._2_infrastructure.LoginResource;
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

import static br.cdb.feature.f000._2_infrastructure.LoginResource.TOKEN_HEADER;

/**
 * Valida o token de acesso (header {@value LoginResource#TOKEN_HEADER}),
 * popula o {@link Request#user()} e — fora do stream — rotaciona o token, guardando o próximo em
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

    @Override
    public void filter(ContainerRequestContext request) {
        if (Request.isStatic(request)) return;

        val token = request.getHeaderString(TOKEN_HEADER);
        if (token == null) return;

        if (Request.isStream(request)) {
            // Worker thread devolvido ao pool assim que o subscribe registra o listener — a conexão
            // fica aberta indefinidamente e o ContainerResponseFilter que limparia o MDC só roda no
            // fechamento do stream. Não empurra X-REQUEST-USER pra não vazar pra próxima requisição
            // que reaproveitar a mesma thread (CurrentUserContext já resolve a autorização em si).
            tokenStore.validate(token).ifPresent(userId -> authenticateStream(userId, request));
        } else {
            val result = tokenStore.rotate(token);
            if (result.isPresent()) {
                authenticate(result.get().userId(), request);
                request.setProperty(NEXT_TOKEN_PROPERTY, result.get().nextToken());
            } else {
                Logger.debug("AUTHN %s %s => invalid or expired token", request.getMethod(), Request.path(request));
            }
        }
    }

    private void authenticateStream(String userId, ContainerRequestContext request) {
        val user = userRepository.get().findById(userId).orElse(null);
        if (user == null || user.personId() == null) {
            Logger.debug("AUTHN %s %s => token references unknown user '%s'", request.getMethod(), Request.path(request), userId);
            return;
        }
        // Identidade exposta às features é a PESSOA — todas as tabelas de dados fazem chave com ela.
        Request.put(Request.X_REQUEST_USER, new AuthenticatedUser(user.personId(), user.username()));
    }

    private void authenticate(String userId, ContainerRequestContext request) {
        val user = userRepository.get().findById(userId).orElse(null);
        if (user == null || user.personId() == null) {
            Logger.debug("AUTHN %s %s => token references unknown user '%s'", request.getMethod(), Request.path(request), userId);
            return;
        }

        Request.put(Request.X_REQUEST_USER, new AuthenticatedUser(user.personId(), user.username()));
        MDC.push(Request.X_REQUEST_USER, user.username());
    }
}
