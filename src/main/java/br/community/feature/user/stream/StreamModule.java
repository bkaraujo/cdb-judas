package br.community.feature.user.stream;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
public class StreamModule {

    @Bean
    public SSE sse() {
        return new SseService();
    }

    @Bean
    public SseController sseController(SSE sse) {
        return new SseController(sse);
    }
}
