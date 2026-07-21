/**
 * Provisionamento inicial (não-HTTP): {@code UserSeeder} semeia o usuário {@code admin} no startup;
 * {@code UserService} cria o login; {@code CategorySeedStep} (via a porta
 * {@code UserProvisioningStep}) semeia o catálogo default de categorias para a pessoa recém-criada.
 */
@NullMarked
package br.cdb.feature.f010;

import org.jspecify.annotations.NullMarked;
