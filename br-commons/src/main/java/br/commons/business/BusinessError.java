package br.commons.business;

import br.commons.i18n.Messages;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Erro de domínio. {@code key} é uma chave de tradução (ver {@link Messages}), não texto pronto —
 * {@link #render(Locale)} é o único ponto que vira texto, resolvido no locale de quem pede (a
 * requisição HTTP, tipicamente), nunca no instante em que o erro nasce dentro do use case.
 */
@NullMarked
public sealed interface BusinessError {

    String key();
    Object[] args();

    default String render(Locale locale) {
        return Messages.of(key(), locale, args());
    }

    @NullMarked
    record NotFound(String key, Object... args) implements BusinessError {}

    @NullMarked
    record BusinessRule(String key, Object... args) implements BusinessError {}

    @NullMarked
    record Validation(String key, Object... args) implements BusinessError {}

    @NullMarked
    record Conflict(String key, Object... args) implements BusinessError {}

}
