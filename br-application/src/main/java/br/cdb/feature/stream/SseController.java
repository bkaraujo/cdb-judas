package br.cdb.feature.stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@Path("/api/{uuid}/stream")
@NullMarked
@RequiredArgsConstructor
public class SseController {

    private final SSE sse;

    // Controlador agnóstico ao segmento {uuid} (sem binding); a guarda de propriedade valida a rota.
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context SseEventSink sink, @Context Sse sseContext) {
        sse.subscribe(sink, sseContext);
    }
}
