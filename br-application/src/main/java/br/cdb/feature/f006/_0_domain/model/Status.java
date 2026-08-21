package br.cdb.feature.f006._0_domain.model;

/**
 * Situação de um {@link Transaction}
 * <ul>
 *     <li>{@link #SCHEDULED} Planejado mas ainda não executado</li>
 *     <li>{@link #CONFIRMED} Executado</li>
 *     <li>{@link #PENDING} Atrasádo/Pendente de Pagamento</li>
 * </ul>
 */
public enum Status {
    SCHEDULED, CONFIRMED, PENDING
}
