package br.community.core.web.filter;

import br.commons.Logger;
import br.commons.tools.Strings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@NullMarked
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        val wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);
        val requestBody = wrappedRequest.getContentAsByteArray();

        var bodyString = Strings.EMPTY;
        if (requestBody.length > 0) { bodyString = new String(requestBody, StandardCharsets.UTF_8); }

        if (!request.getRequestURI().contains("favicon")) {
            Logger.debug("%s %s => %s", request.getMethod(), request.getRequestURI(), bodyString.isEmpty() ? "none" : bodyString);
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}
