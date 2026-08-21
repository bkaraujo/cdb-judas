package br.cdb.core.web;

import br.cdb.core.web.security.AuthenticatedUser;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import br.commons.framework.logger.MDC;
import br.commons.tools.Meta;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public abstract class HTTPRequest {

    public static final String X_REQUEST_ID = "X-REQUEST-ID";
    public static final String X_REQUEST_USER = "X-REQUEST-USER";
    /** Chave interna (não header HTTP) onde {@code LocaleFilter} guarda o locale resolvido da
     *  requisição (via {@code Accept-Language}) — ver {@link #locale()}. */
    public static final String LOCALE = "LOCALE";
    /** Header do token opaco rotativo — mora aqui (não em {@code LoginResource}) para que os
     *  filtros de {@code core.web.filter} o referenciem sem depender de {@code feature.f000}, nem
     *  mesmo silenciosamente (constante {@code String} é inlined em tempo de compilação, então um
     *  import estático dela em {@code core} não deixa nenhuma referência de classe no bytecode —
     *  a regra ArchUnit {@code core_must_not_access_feature} não via essa dependência). */
    public static final String TOKEN_HEADER = "X-Access-Token";

    private HTTPRequest() {}

    private static final Map<String, Map<String, @Nullable Object>> objects = new ConcurrentHashMap<>();

    public static void put(String key, @Nullable Object object) {
        val request = objects.computeIfAbsent(MDC.get(X_REQUEST_ID), _ -> new ConcurrentHashMap<>());
        request.put(key, object);
    }

    @Nullable
    public static <T> T get(String key, Class<T> type) {
        val request = objects.computeIfAbsent(
                MDC.get(X_REQUEST_ID),
                _ -> new ConcurrentHashMap<>()
        );

        if (!request.containsKey(key)) return null;

        val value = request.get(key);
        if (value == null) return null;

        return Meta.cast(value, type);
    }

    /** Usuário autenticado */
    public static @Nullable AuthenticatedUser user() {
        return get(X_REQUEST_USER, AuthenticatedUser.class);
    }

    public static String personId() {
        val authenticated = user();
        if (authenticated == null) {
            throw new BusinessException(new BusinessError.Validation("Usuário não Autenticado"));
        }

        return authenticated.personId();
    }

    /** Locale resolvido pelo {@code LocaleFilter} a partir de {@code Accept-Language} — pt-BR default
     *  fora de requisição HTTP (listener de {@code MessageBus}, jobs) ou quando o filtro não rodou. */
    public static Locale locale() {
        val resolved = get(LOCALE, Locale.class);
        return resolved != null ? resolved : Locale.of("pt", "BR");
    }

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
        return path(request.getUriInfo());
    }

    /** Caminho com barra inicial garantida (UriInfo#getPath é relativo à base, sem a barra). */
    public static String path(UriInfo uriInfo) {
        final String p = uriInfo.getPath();
        return p.startsWith("/") ? p : "/" + p;
    }
}
