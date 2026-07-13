/**
 * Controle do período de fechamento de alterações de conta.
 *
 * <p>Rotas ({@code /api/{uuid}/accounts/closing}):
 * <pre>
 * GET    /closing  — retorna o período de fechamento ativo (ou null se não definido)
 * POST   /closing  — define o período de fechamento (body: {@code {"period":"yyyy-MM"}})
 * DELETE /closing  — remove o período de fechamento
 * </pre>
 *
 * <p>O período de fechamento determina a competência de referência usada para
 * consolidar informações financeiras em aberto no contexto monetário.
 */
@NullMarked
package br.cdb.feature.user.accounts.closing;

import org.jspecify.annotations.NullMarked;
