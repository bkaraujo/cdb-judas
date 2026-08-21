package br.cdb.core.web.filter;

import br.cdb.core.web.HTTPRequest;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Locale;

/**
 * Resolve o locale da requisição a partir de {@code Accept-Language} (negociação de conteúdo que o
 * JAX-RS já faz — {@link ContainerRequestContext#getAcceptableLanguages()} devolve a lista já ordenada
 * por qualidade do header) e guarda em {@link HTTPRequest#locale()} para {@code DomainExceptionMapper}/
 * {@code GenericExceptionMapper} resolverem a mensagem de erro no idioma certo.
 *
 * <p>Só pt-BR e en-US são suportados hoje — casa por prefixo de idioma ({@code pt*}/{@code en*});
 * qualquer outra coisa (header ausente, idioma não suportado) cai no default pt-BR. Prioridade
 * numérica explícita (menor que {@link Priorities#AUTHENTICATION}, mesmo truque de
 * {@code MDCLoggingFilter}) porque não depende de autenticação — mensagem de erro precisa do locale
 * mesmo em rota pública/token inválido.
 */
@Provider
@Priority(600)
@NullMarked
public class LocaleFilter implements ContainerRequestFilter {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Locale EN_US = Locale.of("en", "US");

    @Override
    public void filter(ContainerRequestContext request) {
        HTTPRequest.put(HTTPRequest.LOCALE, resolve(request.getAcceptableLanguages()));
    }

    private static Locale resolve(List<Locale> acceptable) {
        for (val candidate : acceptable) {
            val language = candidate.getLanguage();
            if ("pt".equalsIgnoreCase(language)) return PT_BR;
            if ("en".equalsIgnoreCase(language)) return EN_US;
        }
        return PT_BR;
    }
}
