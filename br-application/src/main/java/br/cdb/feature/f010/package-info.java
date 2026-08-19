/**
 * Regras de nomenclatura — cadastro que casa um texto (`nome`) contra a descrição de um lançamento
 * (digitado manualmente ou vindo de importação de PDF) e, ao bater, substitui a descrição inteira
 * pelo próprio `nome` e pode pré-preencher conta/categoria/centro de custo
 * ({@code /api/{uuid}/accounts/transaction/rules}).
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
 * <p>Regras ambíguas (padrão substring de outra já cadastrada, em qualquer direção, comparação
 * normalizada — uppercase + sem acento) são rejeitadas na criação/edição
 * ({@code ImportRuleService.findAmbiguousConflict}, {@code BusinessError.Conflict} → 409) — nunca
 * resolvidas em tempo de aplicação do lado do cliente.
 */
@NullMarked
package br.cdb.feature.f010;

import org.jspecify.annotations.NullMarked;
