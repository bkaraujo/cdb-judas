package br.community.core.web.filter;

import br.commons.framework.logger.MDC;
import br.community.core.web.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@NullMarked
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MDCLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (RequestUtils.isStatic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var xRequestId = request.getHeader("X-REQUEST-ID");
        if (xRequestId == null) { xRequestId = UUID.randomUUID().toString(); }

        MDC.push("X-REQUEST-ID", xRequestId);
        try { filterChain.doFilter(request, response); }
        finally { MDC.pop("X-REQUEST-ID"); }
    }
}
