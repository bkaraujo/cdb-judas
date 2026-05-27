package br.community.context.shared;

import br.commons.MessageBus;
import br.community.context.shared._1_application.DomainEventListener;
import br.community.context.shared._1_application.SSE;
import br.community.context.shared._2_infrastructure.sse.SseService;
import br.community.context.shared._2_infrastructure.web.SseController;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
public class SharedModule {

    @Bean
    public SSE sse() {
        return new SseService();
    }

    @Bean
    public DomainEventListener domainEventListener(SSE sse) {
        val listener = new DomainEventListener(sse);
        MessageBus.subscribe(listener);
        return listener;
    }

    @Bean
    public SseController sseController(SSE sse) {
        return new SseController(sse);
    }
}
