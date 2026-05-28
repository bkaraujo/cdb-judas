package br.community.core.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class RequestUtils {

    public static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    public static boolean isStatic(HttpServletRequest request) {
        return !isApi(request);
    }

}
