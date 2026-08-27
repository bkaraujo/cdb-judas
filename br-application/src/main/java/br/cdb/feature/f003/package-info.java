/**
 * Cartões de crédito ({@code /api/{uuid}/accounts/{accountId}/cards}). Extraída de {@code f002}
 * (era passthrough sem modelo/repositório próprio, fundida lá; cresceu até justificar fatia
 * própria — ver .claude/refactor.md).
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: a fatia é um par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (toda leitura; guarda de propriedade da entrada HTTP,
 * e os finders {@code list} lidos cross-slice por {@code f000.UserGuards}/{@code f006}/{@code f999})
 * e {@code WriteUseCase} (toda mutação; política de usuário, SSE e cascata de exclusão). O
 * {@code AccountCardResource} resolve os dois direto no {@code Context}, como em f002/f006. Os nomes
 * simples coincidem com o par de f002 — quem precisa dos dois usa o nome completo em um deles.
 *
 * <p>{@code f002.F002Api.AccountView} mantém uma projeção somente-leitura (record {@code CardView})
 * do mesmo shape — f003 é dono das mutações.
 */
@NullMarked
package br.cdb.feature.f003;

import org.jspecify.annotations.NullMarked;
