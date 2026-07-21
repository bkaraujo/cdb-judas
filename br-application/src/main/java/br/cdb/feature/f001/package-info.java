/**
 * Self-service do perfil ({@code GET/PATCH /api/me}): nome + preferências (tema, idioma, locale,
 * sidebar), com write-through no servidor. {@code Profile} junta {@code Person} (contexto people)
 * com {@code Preferences} (dono desta fatia) e o {@code username} de login, resolvido à parte via
 * {@code UserRepository} — sem {@code {uuid}} na rota, a identidade vem do token autenticado.
 */
@NullMarked
package br.cdb.feature.f001;

import org.jspecify.annotations.NullMarked;
