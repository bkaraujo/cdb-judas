package br.community.feature.user.accounts;

import br.commons.MessageBus;
import br.community.context.monetary.MonetaryContext;
import br.community.feature.system.stream.SSE;
import br.community.feature.user.accounts.core.AccountStreamListener;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class AccountModule {

    @Bean
    AccountStreamListener accountStreamListener(SSE sse, MonetaryContext monetaryContext) {
        val listener = new AccountStreamListener(sse, monetaryContext);
        MessageBus.subscribe(listener);
        return listener;
    }
}
