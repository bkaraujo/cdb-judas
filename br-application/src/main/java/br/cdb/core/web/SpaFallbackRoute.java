package br.cdb.core.web;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Fallback estilo SPA: qualquer {@code GET} que não seja API/sistema nem pareça um asset estático
 * (sem extensão no último segmento) é rerroteado para {@code /index.html}, para o roteamento
 * client-side (History API) funcionar em deep-links. Roda em {@code order} 20000 — depois do
 * handler estático embutido do Quarkus (10000) e do roteamento JAX-RS — então só age quando nada
 * mais respondeu à rota.
 */
@NullMarked
@Singleton
public class SpaFallbackRoute {

    private static final int ORDER_AFTER_BUILTIN_ROUTES = 20_000;

    void install(@Observes StartupEvent event, Router router) {
        router.route().order(ORDER_AFTER_BUILTIN_ROUTES).handler(ctx -> {
            val path = ctx.normalizedPath();
            if (!"GET".equals(ctx.request().method().name()) || isReserved(path) || looksLikeStaticAsset(path)) {
                ctx.next();
                return;
            }
            ctx.reroute("/index.html");
        });
    }

    private static boolean isReserved(String path) {
        return path.startsWith("/api/") || path.equals("/login") || path.startsWith("/swagger")
                || path.startsWith("/q/") || path.startsWith("/v3/api-docs");
    }

    private static boolean looksLikeStaticAsset(String path) {
        val lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return lastSegment.contains(".");
    }
}
