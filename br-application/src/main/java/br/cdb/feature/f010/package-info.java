/**
 * Regras de nomenclatura — cadastro de um nome/rótulo + uma lista de gatilhos que casam contra a
 * descrição de um lançamento (digitado manualmente ou vindo de importação de PDF). Ao bater qualquer
 * gatilho, a regra pode pré-preencher conta/categoria/centro de custo — mas <b>nunca altera a
 * descrição original</b> ({@code /api/{uuid}/accounts/transaction/rules}).
 *
 * <p>O casamento em si é <b>100% api-side</b> — regras vêm cacheadas no frontend (mesmo padrão
 * de Tags/Categorias/Contas) e o texto é comparado em JS, tanto no cadastro manual de lançamento
 * quanto nas telas de preview de importação. Esta fatia é <b>CRUD puro</b>: não tem consumidor
 * cross-slice nenhum no backend (nem {@code f006} fala com ela).
 *
 * <p><b>Sem {@code *UseCase} de fronteira</b>: par CQRS Context-wired —
 * {@code _1_application.usecase.ReadUseCase} (toda leitura) e {@code WriteUseCase} (toda mutação).
 * O {@code ImportRuleResource} resolve os dois direto no {@code Context}, como em f002/f003/f004/
 * f006. Sem {@code UserGuards}: escopo por pessoa na própria query
 * ({@code F010_IMPORT_RULE.COD_PERSON}), mesma convenção de f004/f005.
 *
 * <p>Gatilhos ambíguos (um é substring de outro, em qualquer direção, comparação normalizada —
 * uppercase + sem acento) entre regras DIFERENTES da mesma pessoa são rejeitados na criação/edição
 * ({@code ImportRuleService.findAmbiguousConflict}, {@code BusinessError.Conflict} → 409) — nunca
 * resolvidos em tempo de aplicação do lado do cliente.
 */
@NullMarked
package br.cdb.feature.f010;

import org.jspecify.annotations.NullMarked;
