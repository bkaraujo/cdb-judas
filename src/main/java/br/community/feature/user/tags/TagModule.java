package br.community.feature.user.tags;

import br.commons.MessageBus;
import br.community.feature.user.stream.SSE;
import br.community.feature.user.tags.core.TagStreamListener;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class TagModule {

    @Bean
    TagStreamListener tagStreamListener(SSE sse) {
        val listener = new TagStreamListener(sse);
        MessageBus.subscribe(listener);
        return listener;
    }
}
