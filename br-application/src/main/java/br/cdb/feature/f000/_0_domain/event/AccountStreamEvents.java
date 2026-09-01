package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Pedido de reemissão do snapshot SSE para uma conta afetada <b>indiretamente</b> — a mutação
 * aconteceu em outra fatia/entidade (perna irmã de transferência, conta anterior de uma transação
 * editada, alvo do MOVE de conta, cascata de categoria/tag, importação deduplicada por conta) e não
 * tem um evento CRUD próprio de conta para carregar o fan-out. Onde a conta sofre mutação direta
 * (criar/editar/apagar conta ou cartão), o listener de {@code f999} reage direto ao evento de domínio
 * da fatia dona ({@code f002.AccountEvents}/{@code f003.CreditCardEvents}) — ver
 * {@code f999.AccountStreamListener}, único dono do dispatch SSE. Mora em {@code f000} (fatia-base)
 * para que qualquer fatia de negócio possa publicá-lo sem depender de f002.
 */
@NullMarked
public interface AccountStreamEvents extends BusinessEvent {

    @NullMarked
    record Refresh(UUID accountId, String personId) implements AccountStreamEvents {}
}
