package br.cdb.feature.f005._1_application;

import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Apaga as linhas {@code PERSON_CATEGORY} da subárvore já validada e (no MOVE) reatribuída —
 * best-effort, nunca falha o request que originou a exclusão. Assinado no startup (mesmo padrão do
 * listener de overlay de {@code f006}, assinado em {@code F006Module.initialize()}).
 */
@NullMarked
@Singleton
public class CategoryDeletedListener {

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    /** Context-wired (o serviço deixou de ser bean CDI): resolvido por chamada, já com a porta de
     *  {@code F005Module} publicada. */
    @MessageListener
    public MessageResult onCategoryDeleted(CategoryDeleted event) {
        val userCategoryService = Context.tryGet(UserCategoryService.class);
        userCategoryService.deletePlain(event.categoryIds(), event.personId());
        return MessageResult.CONSUMED;
    }
}
