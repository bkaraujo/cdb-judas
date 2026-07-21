/**
 * Importação de extrato/fatura (preview→confirm, parsers BTG e Santander). Consolida o que antes
 * era {@code finance.accounts.statement} (parsing de PDF) e {@code finance.accounts.transactions
 * .importer} (orquestração, casamento de cartão, sugestão de categoria, expansão de parcelas).
 */
@NullMarked
package br.cdb.feature.f006;

import org.jspecify.annotations.NullMarked;
