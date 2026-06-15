package br.community.feature.user.stream;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@NullMarked
public interface SSE {

    enum Event {
        INITIALIZE,
        UPSERT,
        DELETE
    }


    SseEmitter subscribe();

    void dispatch(String username, SSE.Event type, Object payload);

}
