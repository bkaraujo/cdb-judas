package br.community.core.web.filter;

import br.commons.Logger;
import br.commons.framework.logger.MDC;
import br.community.context.security._0_domain.repository.UserRepository;
import br.community.core.web.RequestUtils;
import br.community.feature.system.auth.AccessTokenStore;
import br.community.core.web.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static br.community.feature.system.auth.LoginResource.TOKEN_HEADER;

@NullMarked
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenStore tokenStore;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        val token = request.getHeader(TOKEN_HEADER);
        if (token != null) {
            if (RequestUtils.isStream(request)) {
                tokenStore
                        .validate(token)
                        .ifPresent(userId -> authenticate(userId, request, response, false, null));
            } else {
                val result = tokenStore.rotate(token);
                if (result.isPresent()) {
                    authenticate(result.get().userId(), request, response, true, result.get().nextToken());
                } else {
                    Logger.debug("AUTHN %s %s => invalid or expired token", request.getMethod(), request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String userId, HttpServletRequest request, HttpServletResponse response, boolean rotated, String nextToken) {
        val user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            Logger.debug("AUTHN %s %s => token references unknown user '%s'", request.getMethod(), request.getRequestURI(), userId);
            return;
        }

        val auth = new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId, user.username()), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        if (rotated) response.setHeader(TOKEN_HEADER, nextToken);

        Logger.trace("AUTHN %s %s => user '%s' (%s) authenticated%s", request.getMethod(), request.getRequestURI(), user.username(), userId, rotated ? "" : " (SSE)");
        MDC.push("X-REQUEST-USER", user.username());
        MDC.push("X-REQUEST-USER-ID", userId);
    }
}
