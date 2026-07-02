package br.community.feature;

import br.commons.MessageBus;
import br.community.context.monetary.MonetaryContext;
import br.community.feature.user.accounts.closing.ClosingRepository;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.core.AccountStreamListener;
import br.community.feature.user.accounts.core.UserAccountService;
import br.community.feature.user.stream.SSE;
import br.community.feature.user.stream.SseService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Singleton
public class FeatureModule {

    @Produces
    @Singleton
    public SSE sse() {
        return new SseService();
    }

    @Produces
    @Singleton
    AccountStreamListener accountStreamListener(SSE sse, MonetaryContext monetaryContext, UserAccountService userAccountService) {
        val listener = new AccountStreamListener(sse, monetaryContext, userAccountService);
        MessageBus.subscribe(listener);
        return listener;
    }

    /** Força a criação do listener (e o {@code MessageBus.subscribe}) no startup. */
    void registerListeners(@Observes StartupEvent event, AccountStreamListener listener) {
        // A injeção do listener basta para acioná-lo (e subscrever ao MessageBus) no startup.
    }

    @Produces
    @Singleton
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }
}
