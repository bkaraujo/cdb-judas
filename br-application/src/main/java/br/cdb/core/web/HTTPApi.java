package br.cdb.core.web;

import br.cdb.core.CoreModule;
import br.cdb.core.security.AccessTokenStore;
import br.commons.tools.Threads;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.HttpMethod;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Objects;

/**
 * Leitura cross-slice síncrona: chama o endpoint público da fatia dona via HTTP real (loopback),
 * nunca um import direto de classe de outra fatia (ver {@code feature_slices_must_not_depend_on_sibling_slices}
 * em {@code ArchitectureTest}). Autentica com um token efêmero de uso único
 * ({@link AccessTokenStore#ephemeral}) — nunca o token de sessão do navegador: ele já foi
 * rotacionado no início desta mesma requisição, e repassá-lo faria a chamada interna consumir a
 * rotação que a resposta externa ainda precisa devolver. Sem bypass por loopback: a chamada interna
 * passa pelo mesmo {@code AuthenticationFilter}/{@code OwnershipFilter} de qualquer requisição real.
 */
@NullMarked
@Singleton
public class HTTPApi {

    @Inject
    AccessTokenStore tokenStore;

    @Inject
    ObjectMapper mapper;

    private final String baseUrl = Objects.requireNonNull(CoreModule.yaml.asString("cdb.internal.base-url"));

    private final HttpClient client = HttpClient.newHttpClient();

    /** {@code path} inclui a barra inicial, relativo a {@code /api/{personId}} (ex.: {@code "/categories/transfer?nature=EXPENSE"}). */
    public <T> T get(String path, Class<T> responseType) {
        return send("GET", path, null, responseType, 200);
    }

    public <B, T> T post(String path, B body, Class<T> responseType) {
        return send(HttpMethod.POST, path, body, responseType, 200, 201);
    }

    public <B, T> T patch(String path, B body, Class<T> responseType) {
        return send(HttpMethod.PATCH, path, body, responseType, 200, 204);
    }

    public void delete(String path) {
        execute(HttpMethod.DELETE, path, null, 200, 204);
    }

    private <T> T send(String method, String path, @Nullable Object body, Class<T> responseType, int... acceptedStatuses) {
        val response = execute(method, path, body, acceptedStatuses);
        try {
            return mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new IllegalStateException("Internal call " + method + " " + path + " failed", e);
        }
    }

    private HttpResponse<String> execute(String method, String path, @Nullable Object body, int... acceptedStatuses) {
        val personId = HTTPRequest.personId();
        val token = tokenStore.ephemeral(personId);

        try {
            val request = buildRequest(method, path, body, personId, token);
            val response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return validated(method, path, response, acceptedStatuses);
        } catch (IOException e) {
            throw new IllegalStateException("Internal call " + method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Threads.interrupt();
            throw new IllegalStateException("Internal call " + method + " " + path + " interrupted", e);
        }
    }

    private HttpRequest buildRequest(String method, String path, @Nullable Object body, String personId, String token) throws IOException {
        val builder = HttpRequest.newBuilder(URI.create(baseUrl + "/api/" + personId + path))
                .header(HTTPRequest.TOKEN_HEADER, token)
                .header("Accept", "application/json");

        if (body == null) {
            return builder.method(method, BodyPublishers.noBody()).build();
        }
        return builder.header("Content-Type", "application/json")
                .method(method, BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
    }

    private static HttpResponse<String> validated(String method, String path, HttpResponse<String> response, int... acceptedStatuses) {
        if (Arrays.stream(acceptedStatuses).noneMatch(status -> status == response.statusCode())) {
            throw new IllegalStateException("Internal call " + method + " " + path + " => HTTP " + response.statusCode());
        }
        return response;
    }
}
