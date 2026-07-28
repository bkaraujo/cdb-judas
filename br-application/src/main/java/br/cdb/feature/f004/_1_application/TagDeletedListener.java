package br.cdb.feature.f004._1_application;

import br.cdb.feature.f000._0_domain.event.TagDeleted;
import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Purga {@code PERSON_TRANSACTION_TAG} (no-op se o MOVE já reatribuiu os vínculos) e apaga a linha
 * {@code PERSON_TAG} — best-effort, nunca falha o request que originou a exclusão. Assinado no
 * startup (padrão {@code TransactionOverlayListener}).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagDeletedListener {

    private final UserTransactionTagService tagLinkService;
    private final UserTagService userTagService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onTagDeleted(TagDeleted event) {
        tagLinkService.deleteByTag(event.personId(), event.tagId());
        userTagService.deleteById(event.tagId());
        return MessageResult.CONSUMED;
    }
}
