package br.cdb.feature.f004._1_application;

import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Apaga as linhas {@code PERSON_CATEGORY} da subárvore já validada e (no MOVE) reatribuída —
 * best-effort, nunca falha o request que originou a exclusão. Assinado no startup (padrão
 * {@code TransactionOverlayListener}).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class CategoryDeletedListener {

    private final UserCategoryService userCategoryService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onCategoryDeleted(CategoryDeleted event) {
        userCategoryService.deletePlain(event.categoryIds(), event.personId());
        return MessageResult.CONSUMED;
    }
}
