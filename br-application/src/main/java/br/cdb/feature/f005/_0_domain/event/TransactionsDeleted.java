package br.cdb.feature.f005._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Publicado por quem quer que apague transações via {@code MonetaryUseCases.ucTransaction()} (o
 * dono da mutação, não necessariamente f005) — a limpeza do overlay {@code PERSON_TRANSACTION}
 * (f005) e dos vínculos de tag {@code PERSON_TRANSACTION_TAG} (f008) reage a este evento em vez de
 * o publicador chamar cada fatia satélite diretamente.
 */
@NullMarked
public record TransactionsDeleted(List<UUID> transactionIds) implements BusinessEvent {}
