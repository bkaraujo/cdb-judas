/**
 * Fatias financeiras escopadas por usuário, agrupadas sob um namespace comum.
 *
 * <p>Subpacotes:
 * <ul>
 *   <li>{@link br.cdb.feature.finance.accounts}   — contas, cartões e lançamentos (+ sub-recursos)</li>
 *   <li>{@link br.cdb.feature.finance.categories} — classificação macro de transações</li>
 *   <li>{@link br.cdb.feature.finance.tags}       — classificação livre/transversal de transações</li>
 *   <li>{@link br.cdb.feature.finance.costcenter} — catálogo somente-leitura, sem namespace de usuário</li>
 *   <li>{@link br.cdb.feature.finance.deletion}   — contrato de exclusão compartilhado entre accounts/cards/categories/tags</li>
 * </ul>
 */
@NullMarked
package br.cdb.feature.finance;

import org.jspecify.annotations.NullMarked;