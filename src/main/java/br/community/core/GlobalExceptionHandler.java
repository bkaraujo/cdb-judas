package br.community.core;

import br.commons.Logger;
import br.commons.tools.Strings;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.stream.Collectors;

@NullMarked
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, HttpServletRequest req) {
        HttpStatus status = switch (ex.getError()) {
            case DomainError.NotFound ignored -> HttpStatus.NOT_FOUND;
            case DomainError.Conflict ignored -> HttpStatus.CONFLICT;
            case DomainError.Validation ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case DomainError.BusinessRule ignored -> HttpStatus.BAD_REQUEST;
        };

        val pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        val pd = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());

        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        val detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, detail);

        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        if (isBrokenPipe(ex)) {
            Logger.debug("Client disconnected: %s", req.getRequestURI());
            val pd = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
            pd.setInstance(URI.create(req.getRequestURI()));
            return pd;
        }

        val message = Strings.orEmpty(ex.getMessage());
        if (!message.contains("favicon.ico"))
            Logger.error(message);
        val cause = ex.getCause();
        if (cause != null) { Logger.error(Strings.orEmpty(cause.getMessage())); }

        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    private static boolean isBrokenPipe(Exception ex) {
        if ("ClientAbortException".equals(ex.getClass().getSimpleName())) return true;
        Throwable t = ex;
        while (t != null) {
            val msg = t.getMessage();
            if (msg != null) {
                val lower = msg.toLowerCase();
                if (lower.contains("broken pipe") || lower.contains("pipe quebrado") || lower.contains("connection reset")) return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
