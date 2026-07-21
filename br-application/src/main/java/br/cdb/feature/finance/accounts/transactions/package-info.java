/**
 * CRUD de lançamentos e transferências migrou para {@code br.cdb.feature.f005} (reestruturação
 * fNNN, .claude/refactor.md). O que resta aqui é o que ainda vira {@code f006}: o motor de
 * importação de extrato ({@code importer/}, parsers BTG/Santander, casamento de cartão, sugestão
 * de categoria) e {@code core.ChargeKind} (classificação de linha de fatura, usado só pelos
 * parsers).
 */
@NullMarked
package br.cdb.feature.finance.accounts.transactions;

import org.jspecify.annotations.NullMarked;
