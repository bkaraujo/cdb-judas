package br.cdb.feature.f000._1_application;

import br.cdb.core.security.AccessTokenStore;
import br.cdb.core.web.HTTPRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.val;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Leitura cross-slice síncrona: chama o endpoint público da fatia dona via HTTP real (loopback),
 * nunca um import direto de classe de outra fatia (ver {@code feature_slices_must_not_depend_on_sibling_slices}
 * em {@code ArchitectureTest}). Autentica com um token efêmero de uso único
 * ({@link AccessTokenStore#issueEphemeral}) — nunca o token de sessão do navegador: ele já foi
 * rotacionado no início desta mesma requisição, e repassá-lo faria a chamada interna consumir a
 * rotação que a resposta externa ainda precisa devolver. Sem bypass por loopback: a chamada interna
 * passa pelo mesmo {@code AuthenticationFilter}/{@code OwnershipFilter} de qualquer requisição real.
 */
@NullMarked
@Singleton
public class InternalApi {

    @Inject
    AccessTokenStore tokenStore;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "cdb.internal.base-url")
    String baseUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    /** {@code path} inclui a barra inicial, relativo a {@code /api/{personId}} (ex.: {@code "/categories/transfer?nature=EXPENSE"}). */
    public <T> T get(String path, Class<T> responseType) {
        val personId = HTTPRequest.personId();
        val token = tokenStore.issueEphemeral(personId);

        val request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/" + personId + path))
                .header(HTTPRequest.TOKEN_HEADER, token)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Internal call GET " + path + " => HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new IllegalStateException("Internal call GET " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Internal call GET " + path + " interrupted", e);
        }
    }
}
