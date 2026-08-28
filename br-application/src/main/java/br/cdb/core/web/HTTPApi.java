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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private static final int DEFAULT_HTTP_PORT = 80;

    private static final int CONNECT_PROBE_TIMEOUT_MILLIS = 250;

    private static final long CONNECT_PROBE_INTERVAL_MILLIS = 100;

    /**
     * Espera o servidor HTTP desta JVM começar a aceitar conexões, no máximo {@code timeout}.
     * Existe porque o {@code StartupEvent} roda <b>antes</b> de a porta abrir: qualquer
     * {@code FNNNApi} chamado no boot bate em {@link java.net.ConnectException} — foi o que
     * derrubava a aplicação no recálculo de saldos de {@code FeatureBootstrap}.
     */
    public boolean awaitAvailable(Duration timeout) {
        val uri = URI.create(baseUrl);
        val port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_HTTP_PORT;
        val deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try (val socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), port), CONNECT_PROBE_TIMEOUT_MILLIS);
                return true;
            } catch (IOException notYet) {
                Threads.sleep(CONNECT_PROBE_INTERVAL_MILLIS);
            }
        }
        return false;
    }

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
        val personId = actingPersonId();
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

    /**
     * Pessoa por quem esta chamada age. O ator declarado por {@link InternalCall} ganha do usuário da
     * requisição: quem escreveu {@code as(personId, ..)} sabe por quem quer agir, e é o único caminho
     * fora do ciclo HTTP (boot, {@code @Scheduled}, listener do {@code MessageBus}), onde
     * {@link HTTPRequest#user()} é {@code null}.
     */
    private static String actingPersonId() {
        val internal = InternalCall.personId();
        if (internal != null) return internal;

        val user = HTTPRequest.user();
        if (user != null) return user.personId();

        throw new IllegalStateException("Chamada interna sem pessoa: fora de uma requisição HTTP, "
                + "envolva a chamada em InternalCall.as(personId, ..)");
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
