package br.community.core.web;

import jakarta.servlet.http.HttpServletRequest;

public abstract class RequestUtils {

    public static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    public static boolean isStatic(HttpServletRequest request) {
        return !isApi(request);
    }

}
