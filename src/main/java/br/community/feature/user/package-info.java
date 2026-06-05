/**
 * Features de usuário — recursos escopados pelo workspace {@code {uuid}}.
 *
 * <p>Todo endpoint neste subpacote é prefixado por {@code /api/{uuid}/…},
 * onde {@code {uuid}} identifica o workspace/tenant do usuário autenticado.
 *
 * <ul>
 *   <li>{@link br.community.feature.user.accounts}    — gestão de contas bancárias e cartões</li>
 *   <li>{@link br.community.feature.user.categories}  — classificação de transações por categoria</li>
 *   <li>{@link br.community.feature.user.dashboard}   — resumo financeiro mensal consolidado</li>
 *   <li>{@link br.community.feature.user.tags}        — rótulos livres para organização pessoal</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user;

import org.jspecify.annotations.NullMarked;
