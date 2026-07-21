package br.cdb.feature.f000._0_domain;

import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SSE {

    enum Event {
        INITIALIZE,
        UPSERT,
        DELETE
    }

    void subscribe(SseEventSink sink, Sse sse);

    void dispatch(String username, SSE.Event type, Object payload);

}
