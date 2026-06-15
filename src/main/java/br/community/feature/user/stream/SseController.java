package br.community.feature.user.stream;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/{uuid}/stream")
@NullMarked
@RequiredArgsConstructor
public class SseController {

    private final SSE sse;

    // Controlador agnóstico ao segmento {uuid} (sem binding); a guarda de propriedade valida a rota.
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sse.subscribe();
    }
}
