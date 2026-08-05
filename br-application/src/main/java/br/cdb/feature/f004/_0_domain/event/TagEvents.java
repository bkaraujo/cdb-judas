package br.cdb.feature.f004._0_domain.event;

import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Vocabulário de eventos de tag: {@code Created}/{@code Updated} são só SSE, reagidos por
 *  {@code f999} (único dono do dispatch); {@code Deleted} carrega a estratégia de exclusão e é
 *  reagido também por {@code f006} ({@code F006Module}), que reatribui/desvincula o vínculo
 *  {@code F006_TRANSACTION_TAG} — tabela cuja leitura/escrita são de {@code f006}, não daqui. */
@NullMarked
public interface TagEvents extends BusinessEvent {

    @NullMarked
    record Created(Tag tag) implements TagEvents {}

    @NullMarked
    record Updated(Tag tag) implements TagEvents {}

    @NullMarked
    record Deleted(Tag tag, DeletionStrategy strategy, @Nullable UUID targetId) implements TagEvents {}
}
