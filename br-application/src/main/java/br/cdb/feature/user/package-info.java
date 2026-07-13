/**
 * Features de usuário — recursos escopados pelo workspace {@code {uuid}}.
 *
 * <p>A maioria dos endpoints é prefixada por {@code /api/{uuid}/…}, onde {@code {uuid}} identifica o
 * workspace/tenant do usuário autenticado; exceções pontuais operam sobre o usuário corrente
 * diretamente (ex.: {@code profile} em {@code /api/me}).
 *
 * <ul>
 *   <li>{@link br.cdb.feature.user.accounts}    — gestão de contas bancárias e cartões</li>
 *   <li>{@link br.cdb.feature.user.categories}  — classificação de transações por categoria</li>
 *   <li>{@link br.cdb.feature.user.dashboard}   — resumo financeiro mensal consolidado</li>
 *   <li>{@link br.cdb.feature.user.tags}        — rótulos livres para organização pessoal</li>
 *   <li>{@link br.cdb.feature.user.stream}      — canal SSE de eventos de domínio em tempo real</li>
 *   <li>{@link br.cdb.feature.user.profile}     — perfil do usuário autenticado ({@code /api/me})</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.user;

import org.jspecify.annotations.NullMarked;
