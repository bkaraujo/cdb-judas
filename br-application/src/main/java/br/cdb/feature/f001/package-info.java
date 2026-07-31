/**
 * Self-service do perfil ({@code GET/PATCH /api/me}): nome + preferências (tema, idioma, locale,
 * sidebar), com write-through no servidor. {@code Profile} junta {@code Person} (contexto people)
 * com {@code Preferences} (dono desta fatia) e o {@code username} de login, resolvido à parte via
 * {@code UserRepository} — sem {@code {uuid}} na rota, a identidade vem do token autenticado.
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: a fatia é um par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (o perfil) e {@code WriteUseCase} (nome e preferências).
 * O {@code SelfResource} resolve os dois direto no {@code Context}, como em f002–f006;
 * {@code ProfileService} mudou-se para {@code _1_application.service} e deixou de ser bean CDI na
 * mesma mudança. Os nomes simples coincidem com os pares das outras fatias — quem precisa de mais de
 * um usa o nome completo.
 *
 * <p>Sem guarda de propriedade: não há {@code {uuid}} na rota para divergir do token, então não há
 * IDOR a barrar — o {@code personId} é o do próprio autenticado.
 */
@NullMarked
package br.cdb.feature.f001;

import org.jspecify.annotations.NullMarked;
