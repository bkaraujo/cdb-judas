/**
 * Importação de extratos PDF para lançamentos automáticos.
 *
 * <p>Fluxo em duas etapas:
 * <ol>
 *   <li><b>Preview</b> — {@code POST /transactions/import/preview} (multipart/form-data)<br>
 *       Recebe o arquivo PDF, detecta o emissor ({@code Issuer}), faz o parse e retorna
 *       uma prévia das transações identificadas sem persistir nada.</li>
 *   <li><b>Confirm</b> — {@code POST /transactions/import/confirm} ou
 *       {@code POST /transactions/import/statement/confirm} (application/json)<br>
 *       Persiste as linhas selecionadas pelo usuário, criando ou reconciliando lançamentos.</li>
 * </ol>
 *
 * <p>Tipos de documento suportados:
 * <ul>
 *   <li>{@code CREDIT_CARD_INVOICE} — fatura de cartão de crédito</li>
 *   <li>{@code BANK_STATEMENT}      — extrato bancário</li>
 * </ul>
 *
 * <p>Subpacotes internos:
 * <ul>
 *   <li>{@link br.community.feature.user.accounts.statement.importer.preview}
 *       — abstrações de parsing, detecção de emissor, expansão de parcelas e
 *         sugestão automática de categoria</li>
 *   <li>{@link br.community.feature.user.accounts.statement.importer.confirm}
 *       — comandos e resultado da etapa de confirmação</li>
 *   <li>{@link br.community.feature.user.accounts.statement.importer.provider}
 *       — parsers específicos por banco/emissor (BTG, Santander …)</li>
 * </ul>
 */
@NullMarked
package br.community.feature.user.accounts.statement.importer;

import org.jspecify.annotations.NullMarked;
