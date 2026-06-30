package br.community.core.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class RequestUtils {

    private RequestUtils() {}

    public static boolean isStream(ContainerRequestContext request) {
        return "text/event-stream".equals(request.getHeaderString(HttpHeaders.ACCEPT));
    }

    public static boolean isApi(ContainerRequestContext request) {
        return path(request).startsWith("/api/");
    }

    public static boolean isStatic(ContainerRequestContext request) {
        return !isApi(request);
    }

    /** Caminho com barra inicial garantida (UriInfo#getPath é relativo à base, sem a barra). */
    public static String path(ContainerRequestContext request) {
        final String p = request.getUriInfo().getPath();
        return p.startsWith("/") ? p : "/" + p;
    }
}
