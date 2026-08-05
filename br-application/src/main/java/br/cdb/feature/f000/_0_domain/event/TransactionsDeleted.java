package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Vocabulário compartilhado da fatia-base {@code f000}: publicado por quem apagar transações via
 * a engine {@code WriteUseCases} de f006 (o dono da mutação — f006, ou uma exclusão em cascata de
 * conta/categoria/tag). A limpeza do overlay {@code F006_TRANSACTION_CATEGORY} (f006) e dos vínculos de tag
 * {@code F006_TRANSACTION_TAG} (f004) reage a este evento em vez de o publicador chamar cada fatia
 * satélite diretamente. Mora em {@code f000} (não em quem publica) para respeitar a ordem de fatias:
 * qualquer fNNN pode referenciar a base.
 */
@NullMarked
public record TransactionsDeleted(List<UUID> transactionIds) implements BusinessEvent {}
