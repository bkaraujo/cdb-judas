package br.community.feature.user.categories;

import br.commons.MessageBus;
import br.community.feature.user.categories.core.CategoryStreamListener;
import br.community.feature.user.stream.SSE;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class CategoryModule {

    @Bean
    CategoryStreamListener categoryStreamListener(SSE sse) {
        val listener = new CategoryStreamListener(sse);
        MessageBus.subscribe(listener);
        return listener;
    }
}
