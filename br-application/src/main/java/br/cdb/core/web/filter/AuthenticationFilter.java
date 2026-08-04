package br.cdb.core.web.filter;

import br.cdb.core.security.AccessTokenStore;
import br.cdb.core.web.HTTPRequest;
import br.cdb.core.web.HTTPSession;
import br.cdb.core.web.security.AuthenticatedUser;
import br.commons.Logger;
import br.commons.framework.logger.MDC;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import static br.cdb.core.web.HTTPRequest.TOKEN_HEADER;

/**
 * Valida o token de acesso (header {@value HTTPRequest#TOKEN_HEADER}),
 * popula o {@link HTTPRequest#user()} e — fora do stream — rotaciona o token, guardando o próximo em
 * {@link #NEXT_TOKEN_PROPERTY} para o {@link AuthTokenResponseFilter} emitir na resposta.
 *
 * <p>A identidade sai inteira da {@link HTTPSession}, montada no login: {@code personId} e
 * {@code username} vêm de lá, sem reconsultar {@code UserRepository} a cada requisição só para ir de
 * {@code userId} a {@code personId}. Corolário: usuário apagado no meio de uma sessão continua
 * autenticado até ela caducar — quem quiser derrubá-lo na hora precisa revogar a sessão.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@NullMarked
public class AuthenticationFilter implements ContainerRequestFilter {

    static final String NEXT_TOKEN_PROPERTY = "cdb.nextToken";

    @Inject
    AccessTokenStore tokenStore;

    @Override
    public void filter(ContainerRequestContext request) {
        if (HTTPRequest.isStatic(request)) return;

        val token = request.getHeaderString(TOKEN_HEADER);
        if (token == null) return;

        if (HTTPRequest.isStream(request)) {
            // Worker thread devolvido ao pool assim que o subscribe registra o listener — a conexão
            // fica aberta indefinidamente e o ContainerResponseFilter que limparia o MDC só roda no
            // fechamento do stream. Não empurra X-REQUEST-USER pra não vazar pra próxima requisição
            // que reaproveitar a mesma thread (CurrentUserContext já resolve a autorização em si).
            tokenStore.validate(token).ifPresent(AuthenticationFilter::authenticateStream);
            return;
        }

        val ephemeral = tokenStore.consumeEphemeral(token);
        if (ephemeral.isPresent()) {
            // Chamada HTTP interna fatia→fatia (InternalApi, f000): já é a PESSOA, sem rotação —
            // token de uso único, não existe "próximo" pra devolver ao chamador.
            HTTPRequest.put(HTTPRequest.X_REQUEST_USER,
                    new AuthenticatedUser(ephemeral.get().personId(), "internal"));
            return;
        }

        val session = tokenStore.rotate(token);
        if (session.isPresent()) {
            authenticate(session.get());
            request.setProperty(NEXT_TOKEN_PROPERTY, session.get().token());
        } else {
            Logger.debug("AUTHN %s %s => invalid or expired token", request.getMethod(), HTTPRequest.path(request));
        }
    }

    /** Identidade exposta às features é a PESSOA — todas as tabelas de dados fazem chave com ela. */
    private static void authenticateStream(HTTPSession session) {
        HTTPRequest.put(HTTPRequest.X_REQUEST_USER,
                new AuthenticatedUser(session.personId(), session.username()));
    }

    private static void authenticate(HTTPSession session) {
        authenticateStream(session);
        MDC.push(HTTPRequest.X_REQUEST_USER, session.username());
    }
}
