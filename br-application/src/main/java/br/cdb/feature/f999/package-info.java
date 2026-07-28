/**
 * Provisionamento inicial (não-HTTP): {@code UserSeeder} semeia o usuário {@code admin} no startup;
 * {@code UserService} cria o login; {@code CategorySeedStep} (via a porta
 * {@code UserProvisioningStep}) semeia o catálogo default de categorias para a pessoa recém-criada.
 *
 * <p>Também é o único dono do dispatch SSE (última fatia — pode depender de todas):
 * {@code AccountStreamListener}/{@code TagStreamListener}/{@code CategoryStreamListener}
 * (em {@code _1_application}) reagem ao vocabulário de eventos publicado por f002/f004/f005/f006/f007
 * após cada mutação já persistida e chamam {@code SSE.dispatch} — nenhuma outra fatia o faz.
 */
@NullMarked
package br.cdb.feature.f999;

import org.jspecify.annotations.NullMarked;
