/**
 * CRUD de conta/cartão/saldo, closing e o CRUD de lançamentos migraram para {@code br.cdb.feature}
 * {@code .f002}/{@code .f000}/{@code .f005} (reestruturação fNNN, .claude/refactor.md). O que resta
 * aqui é o motor de importação de extrato ainda não migrado (candidato a {@code f006}):
 * {@code statement/} (parsing de extratos/faturas PDF) e {@code transactions/importer}
 * (+ {@code transactions.core.ChargeKind}).
 */
@NullMarked
package br.cdb.feature.finance.accounts;

import org.jspecify.annotations.NullMarked;
