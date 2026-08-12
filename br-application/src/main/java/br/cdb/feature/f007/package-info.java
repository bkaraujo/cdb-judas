/**
 * Importação de extrato/fatura ({@code /api/{uuid}/accounts/transactions/import/*}).
 *
 * <p>Parsers BTG/Santander/Sam's Club, preview→confirm, expansão de parcelas, casamento de cartão, dedup por
 * assinatura de grupo determinística ({@code GroupSignature}) e reconciliação de lançamentos manuais.
 * {@code ImportUseCase} é o único {@code *UseCase} de fronteira (fluxo único, não CRUD — sem par CQRS).
 *
 * <p>Leitura cross-slice é sempre via {@code FNNNApi} (HTTP real por {@code f000.InternalApi}) —
 * {@code F002Api} (contas, cartões embutidos, fechamento), {@code F003Api} (cartões), {@code F004Api}
 * (posse de tag) e {@code F006Api} (histórico de transações). Nenhum {@code Context.tryGet} de use
 * case irmão. Escrita é via a porta {@code TransactionWriter} (declarada em {@code f007._0_domain}),
 * implementada por {@code f999.TransactionWriterAdapter} — o único ponto do código que conhece f006 e
 * f007 ao mesmo tempo: uma fatura grande vira centenas de linhas, e a importação precisa do
 * {@code groupId} determinístico que {@code WriteUseCases.createInstallments} não produz, então a
 * escrita continua em processo em vez de HTTP por linha.
 *
 * <p>Publica {@code TransactionImported} (evento de {@code f000}) após persistir cada linha — o
 * listener de overlay de {@code f006} (`F006Module`) grava os vínculos {@code F006_TRANSACTION_CATEGORY}/
 * {@code F006_TRANSACTION_TAG}. Sem tabela própria: não existe {@code F007_*} no schema.
 */
@NullMarked
package br.cdb.feature.f007;

import org.jspecify.annotations.NullMarked;
