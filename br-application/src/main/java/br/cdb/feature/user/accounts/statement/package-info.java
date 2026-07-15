/**
 * Análise (parsing) de extratos e faturas em PDF: o modelo de documento e o contrato de parser,
 * compartilhados como suporte à importação ({@code transactions.importer}).
 *
 * <p>Não expõe rota HTTP — é consumido pelo {@code StatementImportService}, que itera os
 * {@link br.cdb.feature.user.accounts.statement.StatementParser} registrados, pergunta a cada um
 * via {@code parseable} se reconhece o texto extraído (emissor + tipo de documento) e deixa o único
 * capaz produzir o {@link br.cdb.feature.user.accounts.statement.MonetaryDocument}.
 *
 * <p>Tipos principais:
 * <ul>
 *   <li>{@link br.cdb.feature.user.accounts.statement.StatementParser} — SPI de parser por banco
 *       ({@code Parser<MonetaryDocument>})</li>
 *   <li>{@link br.cdb.feature.user.accounts.statement.MonetaryDocument} — resultado selado,
 *       discriminado entre {@code Invoice} (fatura de cartão) e {@code Statement} (extrato bancário),
 *       cada variante carregando o {@code Issuer} detectado</li>
 *   <li>{@link br.cdb.feature.user.accounts.statement.MonetaryDocumentEntry} — uma linha mantida
 *       do documento (lançamento de cartão ou movimento de conta)</li>
 *   <li>{@link br.cdb.feature.user.accounts.statement.Issuer} — emissor/banco identificado pelos
 *       marcadores (CNPJ/domínio) presentes no texto</li>
 * </ul>
 *
 * <p>Subpacote {@link br.cdb.feature.user.accounts.statement.provider} — implementações de parser
 * específicas por banco.
 */
@NullMarked
package br.cdb.feature.user.accounts.statement;

import org.jspecify.annotations.NullMarked;
