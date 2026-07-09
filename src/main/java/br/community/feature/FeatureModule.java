package br.community.feature;

import br.community.feature.user.accounts.closing.ClosingRepository;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.stream.SSE;
import br.community.feature.user.stream.SseService;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
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
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }
}
