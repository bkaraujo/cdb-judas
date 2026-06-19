package br.community.feature;

import br.commons.MessageBus;
import br.community.context.monetary.MonetaryContext;
import br.community.feature.user.accounts.closing.ClosingRepository;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.core.AccountStreamListener;
import br.community.feature.user.accounts.core.UserAccountService;
import br.community.feature.user.categories.core.CategoryStreamListener;
import br.community.feature.user.stream.SSE;
import br.community.feature.user.stream.SseController;
import br.community.feature.user.stream.SseService;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@NullMarked
@Configuration
public class FeatureModule {

    @Bean
    public SSE sse() {
        return new SseService();
    }

    @Bean
    public SseController sseController(SSE sse) {
        return new SseController(sse);
    }

    @Bean
    CategoryStreamListener categoryStreamListener(SSE sse) {
        val listener = new CategoryStreamListener(sse);
        MessageBus.subscribe(listener);
        return listener;
    }

    @Bean
    AccountStreamListener accountStreamListener(SSE sse, MonetaryContext monetaryContext, UserAccountService userAccountService) {
        val listener = new AccountStreamListener(sse, monetaryContext, userAccountService);
        MessageBus.subscribe(listener);
        return listener;
    }

    @Bean
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }
}
